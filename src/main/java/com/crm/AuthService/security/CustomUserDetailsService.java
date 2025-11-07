package com.crm.AuthService.security;

import com.crm.AuthService.exception.TenantNotFoundException;
import com.crm.AuthService.role.entities.Role;
import com.crm.AuthService.role.repositories.RoleRepository;
import com.crm.AuthService.tenant.entities.Tenant;
import com.crm.AuthService.tenant.repository.TenantRepository;
import com.crm.AuthService.user.entities.User;
import com.crm.AuthService.user.repositories.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final TenantRepository tenantRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        Set<String> roleNames = new HashSet<>();
        if(user.getRoleIds() != null && !user.getRoleIds().isEmpty()) {
            roleNames = roleRepository.findAllById(user.getRoleIds())
                    .stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet());
        }

        user.setRoleNames(roleNames);
        user.setTenantId(tenant.getId());
        user.setTenantName(tenant.getName());
        user.setTenantStatus(tenant.getStatus());

        return user;
    }
}