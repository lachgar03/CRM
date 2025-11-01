package com.crm.AuthService.user.services;

import com.crm.AuthService.cache.CacheEvictionService;
import com.crm.AuthService.exception.EmailAlreadyExistsException;
import com.crm.AuthService.exception.RoleNotFoundException;
import com.crm.AuthService.role.entities.Role;
import com.crm.AuthService.role.repositories.RoleRepository;
import com.crm.AuthService.security.TenantContextHolder;
import com.crm.AuthService.tenant.entities.Tenant;
import com.crm.AuthService.tenant.repository.TenantRepository;
import com.crm.AuthService.user.dtos.*;
import com.crm.AuthService.user.entities.User;
import com.crm.AuthService.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final CacheEvictionService cacheEvictionService;


    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable, String search) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        Page<User> users;
        if (search != null && !search.isBlank()) {
            users = userRepository.findByTenantIdAndSearch(tenantId, search, pageable);
        } else {
            users = userRepository.findByTenantId(tenantId, pageable);
        }

        return users.map(this::toUserResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        return toUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        // Check email uniqueness
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // Get tenant
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant not found"));

        // Get roles
        Set<Role> roles = new HashSet<>();
        if (request.getRoleIds() != null && !request.getRoleIds().isEmpty()) {
            roles = new HashSet<>(roleRepository.findAllById(request.getRoleIds()));

            if (roles.size() != request.getRoleIds().size()) {
                throw new RoleNotFoundException("One or more roles not found");
            }
        }

        // Create user
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .tenant(tenant)
                .roles(roles)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created: id={}, email={}, tenantId={}", savedUser.getId(), savedUser.getEmail(), tenantId);

        return toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
        String oldEmail = user.getEmail();

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new EmailAlreadyExistsException(request.getEmail());
            }
            user.setEmail(request.getEmail().toLowerCase());
        }
        if (request.getEnabled() != null) {
            user.setEnabled(request.getEnabled());
        }
        if (request.getRoleIds() != null) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(request.getRoleIds()));
            user.setRoles(roles);
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        cacheEvictionService.evictUserCaches(updatedUser.getId(), oldEmail);
        if (!oldEmail.equals(updatedUser.getEmail())) {
            cacheEvictionService.evictUserCaches(updatedUser.getId(), updatedUser.getEmail());
        }

        log.info("User updated: id={}, email={}, tenantId={}", updatedUser.getId(), updatedUser.getEmail(), tenantId);

        return toUserResponse(updatedUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        user.setEnabled(false);
        userRepository.save(user);
        cacheEvictionService.evictUserCaches(user.getId(), user.getEmail());

        log.info("User soft deleted: id={}, email={}, tenantId={}", user.getId(), user.getEmail(), tenantId);
    }

    @Override
    @Transactional
    public UserResponse activateUser(Long id) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        user.setEnabled(true);
        User savedUser = userRepository.save(user);
        cacheEvictionService.evictUserCaches(savedUser.getId(), savedUser.getEmail());

        log.info("User activated: id={}, email={}", savedUser.getId(), savedUser.getEmail());

        return toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse deactivateUser(Long id) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        user.setEnabled(false);
        User savedUser = userRepository.save(user);

        log.info("User deactivated: id={}, email={}", savedUser.getId(), savedUser.getEmail());

        return toUserResponse(savedUser);
    }

    @Override
    @Transactional
    public UserResponse assignRoles(Long id, Set<Long> roleIds) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        User user = userRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        Set<Role> roles = new HashSet<>(roleRepository.findAllById(roleIds));

        if (roles.size() != roleIds.size()) {
            throw new RoleNotFoundException("One or more roles not found");
        }

        user.setRoles(roles);
        User savedUser = userRepository.save(user);

        log.info("Roles assigned: userId={}, roleIds={}", savedUser.getId(), roleIds);

        return toUserResponse(savedUser);
    }

    private UserResponse toUserResponse(User user) {
        Set<RoleDto> roleDtos = user.getRoles().stream()
                .map(role -> RoleDto.builder()
                        .id(role.getId())
                        .name(role.getName())
                        .description(role.getDescription())
                        .build())
                .collect(Collectors.toSet());

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .enabled(user.isEnabled())
                .tenantId(user.getTenant().getId())
                .tenantName(user.getTenant().getName())
                .roles(roleDtos)
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}