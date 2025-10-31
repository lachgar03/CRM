package com.crm.AuthService.auth.services;

import com.crm.AuthService.auth.dtos.AuthResponse;
import com.crm.AuthService.auth.dtos.LoginRequest;
import com.crm.AuthService.auth.dtos.TenantRegistrationRequest;
import com.crm.AuthService.exception.*;
import com.crm.AuthService.role.entities.Role;
import com.crm.AuthService.role.repositories.RoleRepository;
import com.crm.AuthService.security.JwtService;
import com.crm.AuthService.tenant.entities.Tenant;
import com.crm.AuthService.tenant.repository.TenantRepository;
import com.crm.AuthService.user.entities.User;
import com.crm.AuthService.user.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RoleRepository roleRepository;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final PasswordEncoder passwordEncoder;

    @Value("${jwt.expiration}")
    private Long jwtExpiration;

    private static final String TENANT_STATUS_ACTIVE = "ACTIVE";
    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final String TOKEN_TYPE_BEARER = "Bearer";
    private static final List<String> RESERVED_SUBDOMAINS = List.of(
            "admin", "api", "www", "app", "test", "staging", "prod", "production",
            "dev", "demo", "mail", "ftp", "cdn", "static", "assets"
    );

    /**
     * Authenticates a user and returns JWT tokens
     * @param loginRequest Login credentials
     * @return AuthResponse containing access and refresh tokens
     * @throws BadCredentialsException if credentials are invalid
     * @throws DisabledException if user or tenant is disabled
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest loginRequest) {
        try {
            // Authenticate user
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );

            User user = (User) authentication.getPrincipal();

            // Validate user and tenant status
            validateUserAndTenantStatus(user);

            // Generate tokens and build response
            return buildAuthResponse(user);

        } catch (org.springframework.security.authentication.BadCredentialsException e) {
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        } catch (org.springframework.security.authentication.DisabledException e) {
            throw new DisabledException("Compte utilisateur désactivé");
        } catch (org.springframework.security.authentication.LockedException e) {
            throw new DisabledException("Compte utilisateur verrouillé");
        }
    }

    /**
     * Refreshes an access token using a valid refresh token
     * @param refreshToken Valid refresh token
     * @return AuthResponse with new access token
     * @throws InvalidTokenException if refresh token is invalid or expired
     * @throws UserNotFoundException if user not found
     * @throws DisabledException if user or tenant is disabled
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        // Extract username from token
        String username = jwtService.extractUsername(refreshToken);

        // Find user
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UserNotFoundException(username));

        // Validate token
        if (!jwtService.isTokenValid(refreshToken, user)) {
            throw new InvalidTokenException("Refresh token invalide ou expiré");
        }

        // Validate user and tenant status
        validateUserAndTenantStatus(user);

        // Generate new access token only
        String newAccessToken = jwtService.generateToken(user, user.getTenant().getId());

        return buildAuthResponse(user, newAccessToken, refreshToken);
    }

    /**
     * Registers a new tenant with admin user
     * @param request Tenant registration details
     * @return AuthResponse with tokens for the new admin user
     * @throws TenantAlreadyExistsException if subdomain exists
     * @throws EmailAlreadyExistsException if email exists
     * @throws RoleNotFoundException if admin role not found
     */
    @Override
    @Transactional
    public AuthResponse registerTenant(TenantRegistrationRequest request) {
        // Validate subdomain format and availability
        validateSubdomain(request.getSubdomain());

        // Check subdomain uniqueness
        if (tenantRepository.findBySubdomain(request.getSubdomain()).isPresent()) {
            throw new TenantAlreadyExistsException(request.getSubdomain());
        }

        // Check email uniqueness
        if (userRepository.findByEmail(request.getAdminEmail()).isPresent()) {
            throw new EmailAlreadyExistsException(request.getAdminEmail());
        }

        // Get admin role
        Role adminRole = roleRepository.findByName(ROLE_ADMIN)
                .orElseThrow(() -> new RoleNotFoundException(ROLE_ADMIN));

        // Create tenant
        Tenant newTenant = buildTenant(request);

        // Create admin user
        User adminUser = buildAdminUser(request, adminRole, newTenant);

        // Associate user with tenant
        newTenant.setUsers(List.of(adminUser));

        // Save tenant (cascade will save user)
        Tenant savedTenant = tenantRepository.save(newTenant);
        User savedAdmin = savedTenant.getUsers().get(0);

        // Generate tokens and return response
        return buildAuthResponse(savedAdmin);
    }

    // ============================================================
    // PRIVATE HELPER METHODS
    // ============================================================

    /**
     * Validates user and tenant status
     * @param user User to validate
     * @throws DisabledException if user or tenant is disabled
     */
    private void validateUserAndTenantStatus(User user) {
        if (!user.isEnabled()) {
            throw new DisabledException("Compte utilisateur désactivé");
        }

        if (user.getTenant() == null) {
            throw new DisabledException("Tenant non trouvé");
        }

        if (!TENANT_STATUS_ACTIVE.equals(user.getTenant().getStatus())) {
            throw new DisabledException(
                    String.format("Le tenant '%s' est désactivé ou suspendu",
                            user.getTenant().getName())
            );
        }
    }

    /**
     * Validates subdomain format and reserved names
     * @param subdomain Subdomain to validate
     * @throws IllegalArgumentException if subdomain is invalid
     */
    private void validateSubdomain(String subdomain) {
        if (subdomain == null || subdomain.isBlank()) {
            throw new IllegalArgumentException("Le sous-domaine ne peut pas être vide");
        }

        // Check length (3-63 characters)
        if (subdomain.length() < 3 || subdomain.length() > 63) {
            throw new IllegalArgumentException(
                    "Le sous-domaine doit contenir entre 3 et 63 caractères"
            );
        }

        // Check format: lowercase alphanumeric and hyphens, must start/end with alphanumeric
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
    private Tenant buildTenant(TenantRegistrationRequest request) {
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
     * @param tenant Associated tenant
     * @return User entity
     */
    private User buildAdminUser(TenantRegistrationRequest request, Role adminRole, Tenant tenant) {
        return User.builder()
                .firstName(request.getAdminFirstName())
                .lastName(request.getAdminLastName())
                .email(request.getAdminEmail().toLowerCase())
                .password(passwordEncoder.encode(request.getAdminPassword()))
                .roles(Set.of(adminRole))
                .tenant(tenant)
                .enabled(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Builds AuthResponse with new tokens
     * @param user User entity
     * @return AuthResponse
     */
    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user, user.getTenant().getId());
        String refreshToken = jwtService.generateRefreshToken(user, user.getTenant().getId());
        return buildAuthResponse(user, accessToken, refreshToken);
    }

    /**
     * Builds AuthResponse with provided tokens
     * @param user User entity
     * @param accessToken Access token
     * @param refreshToken Refresh token
     * @return AuthResponse
     */
    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        Set<String> roleNames = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(TOKEN_TYPE_BEARER)
                .expiresIn(jwtExpiration / 1000)
                .tenantId(user.getTenant().getId())
                .username(user.getEmail())
                .roles(roleNames)
                .tenantName(user.getTenant().getName())
                .build();
    }
}