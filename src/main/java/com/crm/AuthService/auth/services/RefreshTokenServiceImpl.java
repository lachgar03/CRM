package com.crm.AuthService.auth.services;

import com.crm.AuthService.auth.dtos.AuthResponse;
import com.crm.AuthService.exception.InvalidTokenException;
import com.crm.AuthService.exception.TenantNotFoundException;
import com.crm.AuthService.exception.UserNotFoundException;
import com.crm.AuthService.role.entities.Role;
import com.crm.AuthService.role.repositories.RoleRepository;
import com.crm.AuthService.security.JwtService;
import com.crm.AuthService.security.TenantContextHolder;
import com.crm.AuthService.tenant.entities.Tenant;
import com.crm.AuthService.tenant.repository.TenantRepository;
import com.crm.AuthService.user.entities.User;
import com.crm.AuthService.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final JwtService jwtService;
    private final AuthHelper authHelper;
    private final UserRepository userRepository;
    // INJECT REPOSITORIES
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;

    @Override
    public AuthResponse refreshToken(String refreshToken) throws InvalidTokenException, UserNotFoundException {

        // Tenant context IS set here by JwtAuthenticationFilter
        String username = jwtService.extractUsername(refreshToken);
        Long tenantId = TenantContextHolder.getRequiredTenantId();

        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new InvalidTokenException("Refresh token invalide ou expiré");
        }

        // === Manually populate transient fields for AuthHelper ===
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + tenantId));

        Set<String> roleNames = new HashSet<>();
        if(user.getRoleIds() != null && !user.getRoleIds().isEmpty()) {
            roleNames = roleRepository.findAllById(user.getRoleIds()).stream()
                    .map(Role::getName)
                    .collect(Collectors.toSet());
        }

        user.setTenantId(tenant.getId());
        user.setTenantName(tenant.getName());
        user.setTenantStatus(tenant.getStatus());
        user.setRoleNames(roleNames);
        // === End population ===

        // This call now works, as the 'user' object has the required transient data
        authHelper.validateUserAndTenantStatus(user);

        // Generate new access token
        String newAccessToken = jwtService.generateToken(user, tenantId);

        // This call now works without making extra DB calls
        return authHelper.buildAuthResponse(user, newAccessToken, refreshToken);
    }
}