package com.crm.AuthService.auth.services;

import com.crm.AuthService.auth.dtos.AuthResponse;
import com.crm.AuthService.auth.dtos.TenantRegistrationRequest;
import com.crm.AuthService.exception.TenantNotFoundException; // Recommended
import com.crm.AuthService.role.entities.Role;
import com.crm.AuthService.role.repositories.RoleRepository;
import com.crm.AuthService.security.JwtService;
import com.crm.AuthService.security.TenantContextHolder; // Import context holder
import com.crm.AuthService.tenant.entities.Tenant;
import com.crm.AuthService.tenant.repository.TenantRepository; // Import repo
import com.crm.AuthService.user.entities.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.DisabledException; // Import
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AuthHelper {
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;
    private static final String TENANT_STATUS_ACTIVE = "ACTIVE";
    private static final String TOKEN_TYPE_BEARER = "Bearer";

    private static final List<String> RESERVED_SUBDOMAINS = List.of(
            "admin", "api", "www", "app", "test", "staging", "prod", "production",
            "dev", "demo", "mail", "ftp", "cdn", "static", "assets"
    );


    /**
     * Validates user and tenant status
     * @param user User to validate
     * @throws DisabledException if user or tenant is disabled
     */
    /**
     * Validates user and tenant status
     * @param user User to validate
     * @throws DisabledException if user or tenant is disabled
     */
    public void validateUserAndTenantStatus(User user) {
        if (!user.isEnabled()) {
            throw new DisabledException("Compte utilisateur désactivé");
        }

        // MODIFIED: Read status directly from the User principal
        if (user.getTenantStatus() == null) {
            // This should not happen if CustomUserDetailsService is working
            throw new IllegalStateException("Tenant status not loaded onto User principal");
        }

        if (!TENANT_STATUS_ACTIVE.equals(user.getTenantStatus())) {
            throw new DisabledException(
                    String.format("Le tenant '%s' est désactivé ou suspendu",
                            user.getTenantName()) // Use name from principal
            );
        }
    }

    /**
     * Validates subdomain format and reserved names
     * @param subdomain Subdomain to validate
     * @throws IllegalArgumentException if subdomain is invalid
     */
    public void validateSubdomain(String subdomain) {
        if (subdomain == null || subdomain.isBlank()) {
            throw new IllegalArgumentException("Le sous-domaine ne peut pas être vide");
        }

        // Check length (3-63 characters)
        if (subdomain.length() < 3 || subdomain.length() > 63) {
            throw new IllegalArgumentException(
                    "Le sous-domaine doit contenir entre 3 et 63 caractères"
            );
        }

        // Check format
        if (!subdomain.matches("^[a-z0-9]([a-z0-9-]{0,61}[a-z0-9])?$")) {
            throw new IllegalArgumentException(
                    "Le sous-domaine ne peut contenir que des lettres minuscules, " +
                            "chiffres et tirets (ne peut pas commencer ou finir par un tiret)"
            );
        }

        // Check reserved names
        if (RESERVED_SUBDOMAINS.contains(subdomain.toLowerCase())) {
            throw new IllegalArgumentException(
                    String.format("Le sous-domaine '%s' est réservé", subdomain)
            );
        }
    }

    /**
     * Builds a Tenant entity from registration request
     * @param request Registration request
     * @return Tenant entity
     */
    public Tenant buildTenant(TenantRegistrationRequest request) {
        return Tenant.builder()
                .name(request.getCompanyName())
                .subdomain(request.getSubdomain().toLowerCase())
                .subscription_plan(request.getSubscriptionPlan())
                .status(TENANT_STATUS_ACTIVE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Builds an admin User entity from registration request
     * @param request Registration request
     * @param adminRole Admin role
     * @param tenant Associated tenant (only used for context, not saved on user)
     * @return User entity
     */
    public User buildAdminUser(TenantRegistrationRequest request, Role adminRole, Tenant tenant) {
        return User.builder()
                .firstName(request.getAdminFirstName())
                .lastName(request.getAdminLastName())
                .email(request.getAdminEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getAdminPassword()))
                .roleIds(Set.of(adminRole.getId())) // FIXED: Use roleIds
                // .tenant(tenant) // REMOVED: User entity does not have this field
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Builds AuthResponse with new tokens
     * @param user User entity
     * @return AuthResponse
     */
    public AuthResponse buildAuthResponse(User user) {
        // Get tenantId from user principal (which was set from context)
        Long tenantId = user.getTenantId();
        if (tenantId == null) {
            // Fallback for registration flow
            tenantId = TenantContextHolder.getRequiredTenantId();
        }

        String accessToken = jwtService.generateToken(user, tenantId);
        String refreshToken = jwtService.generateRefreshToken(user, tenantId);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    /**
     * Builds AuthResponse with provided tokens
     * @param user User entity
     * @param accessToken Access token
     * @param refreshToken Refresh token
     * @return AuthResponse
     */
    public AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {

        // MODIFIED: All data is now on the User principal. No DB calls needed.

        // Get Role names from transient field
        Set<String> roleNames = user.getRoleNames();

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(TOKEN_TYPE_BEARER)
                .expiresIn(jwtExpiration / 1000)
                .tenantId(user.getTenantId()) // Get from principal
                .username(user.getEmail())
                .roles(roleNames) // Get from principal
                .tenantName(user.getTenantName()) // Get from principal
                .build();
    }

}