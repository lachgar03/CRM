package com.crm.AuthService.user.services;

import com.crm.AuthService.cache.CacheEvictionService;
import com.crm.AuthService.exception.EmailAlreadyExistsException;
import com.crm.AuthService.exception.RoleNotFoundException;
import com.crm.AuthService.exception.TenantNotFoundException; // Use a specific exception
import com.crm.AuthService.exception.UserNotFoundException;
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

    /**
     * Helper to get the current tenant from the public schema
     */
    private Tenant getRequiredTenant(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found with id: " + tenantId));
    }

    /**
     * Helper to validate and get Role IDs
     */
    private Set<Long> validateRoleIds(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return new HashSet<>();
        }
        long foundRolesCount = roleRepository.countByIdIn(roleIds);
        if (foundRolesCount != roleIds.size()) {
            throw new RoleNotFoundException("One or more roles not found");
        }
        return roleIds;
    }


    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAllUsers(Pageable pageable, String search) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Tenant tenant = getRequiredTenant(tenantId); // Fetch tenant info for response

        Page<User> users;
        if (search != null && !search.isBlank()) {
            // Assuming findByTenantIdAndSearch is a custom query that doesn't need tenantId
            // If it's pure JPA, it will use the context, which is correct
            users = userRepository.findByTenantIdAndSearch(search, pageable); // Assuming tenantId is not needed if you use context
        } else {
            users = userRepository.findAll(pageable); // Context handles tenant filtering
        }

        return users.map(user -> toUserResponse(user, tenant));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Tenant tenant = getRequiredTenant(tenantId); // Fetch tenant info

        User user = userRepository.findById(id) // Context handles tenant filtering
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        return toUserResponse(user, tenant);
    }

    @Override
    @Transactional
    public UserResponse createUser(CreateUserRequest request) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Tenant tenant = getRequiredTenant(tenantId); // Fetch tenant for response

        // Check email uniqueness *within the current tenant*
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException(request.getEmail());
        }

        // Validate roles (from public schema)
        Set<Long> roleIds = validateRoleIds(request.getRoleIds());

        // Create user
        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .roleIds(roleIds) // <-- Set role IDs
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();

        User savedUser = userRepository.save(user);
        log.info("User created: id={}, email={}, tenantId={}", savedUser.getId(), savedUser.getEmail(), tenantId);

        return toUserResponse(savedUser, tenant);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Tenant tenant = getRequiredTenant(tenantId); // Fetch tenant for response

        User user = userRepository.findById(id)
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
            Set<Long> roleIds = validateRoleIds(request.getRoleIds());
            user.setRoleIds(roleIds); // <-- Set role IDs
        }

        user.setUpdatedAt(LocalDateTime.now());
        User updatedUser = userRepository.save(user);

        cacheEvictionService.evictUserCaches(updatedUser.getId(), oldEmail);
        if (!oldEmail.equals(updatedUser.getEmail())) {
            cacheEvictionService.evictUserCaches(updatedUser.getId(), updatedUser.getEmail());
        }

        log.info("User updated: id={}, email={}, tenantId={}", updatedUser.getId(), updatedUser.getEmail(), tenantId);

        return toUserResponse(updatedUser, tenant);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        // This is a soft delete, just disable the user
        user.setEnabled(false);
        user.setAccountNonLocked(false); // Also lock
        userRepository.save(user);
        cacheEvictionService.evictUserCaches(user.getId(), user.getEmail());

        log.info("User soft deleted: id={}, email={}, tenantId={}", user.getId(), user.getEmail(), tenantId);
    }

    @Override
    @Transactional
    public UserResponse activateUser(Long id) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Tenant tenant = getRequiredTenant(tenantId); // Fetch tenant for response

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        user.setEnabled(true);
        user.setAccountNonLocked(true); // Also unlock
        User savedUser = userRepository.save(user);
        cacheEvictionService.evictUserCaches(savedUser.getId(), savedUser.getEmail());

        log.info("User activated: id={}, email={}", savedUser.getId(), savedUser.getEmail());

        return toUserResponse(savedUser, tenant);
    }

    @Override
    @Transactional
    public UserResponse deactivateUser(Long id) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Tenant tenant = getRequiredTenant(tenantId); // Fetch tenant for response

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        user.setEnabled(false);
        user.setAccountNonLocked(false); // Also lock
        User savedUser = userRepository.save(user);

        cacheEvictionService.evictUserCaches(savedUser.getId(), savedUser.getEmail());
        log.info("User deactivated: id={}, email={}", savedUser.getId(), savedUser.getEmail());

        return toUserResponse(savedUser, tenant);
    }

    @Override
    @Transactional
    public UserResponse assignRoles(Long id, Set<Long> roleIds) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        Tenant tenant = getRequiredTenant(tenantId); // Fetch tenant for response

        User user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));

        Set<Long> validatedRoleIds = validateRoleIds(roleIds);
        user.setRoleIds(validatedRoleIds); // <-- Set role IDs
        User savedUser = userRepository.save(user);

        log.info("Roles assigned: userId={}, roleIds={}", savedUser.getId(), roleIds);

        return toUserResponse(savedUser, tenant);
    }

    /**
     * Private helper to build the response DTO.
     * This version requires the Tenant object to be passed in,
     * as the User entity itself does not store it.
     */
    private UserResponse toUserResponse(User user, Tenant tenant) {

        // Manually fetch Roles from public schema using the IDs
        Set<RoleDto> roleDtos = new HashSet<>();
        if (user.getRoleIds() != null && !user.getRoleIds().isEmpty()) {
            Set<Role> roles = new HashSet<>(roleRepository.findAllById(user.getRoleIds()));
            roleDtos = roles.stream()
                    .map(role -> RoleDto.builder()
                            .id(role.getId())
                            .name(role.getName())
                            .description(role.getDescription())
                            .build())
                    .collect(Collectors.toSet());
        }

        return UserResponse.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .enabled(user.isEnabled())
                .tenantId(tenant.getId()) // <-- Get from Tenant object
                .tenantName(tenant.getName()) // <-- Get from Tenant object
                .roles(roleDtos) // <-- Set the manually fetched roles
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}