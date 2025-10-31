package com.crm.AuthService.role.services;

import com.crm.AuthService.role.entities.Permission;
import com.crm.AuthService.role.entities.Role;
import com.crm.AuthService.user.entities.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private static final String SUPER_ADMIN_ROLE = "ROLE_SUPER_ADMIN";

    /**
     * Check if current authenticated user has a specific permission
     *
     * @param resource Resource name (e.g., "USER", "CUSTOMER")
     * @param action Action name (e.g., "CREATE", "READ", "UPDATE", "DELETE")
     * @return true if user has permission, false otherwise
     */
    public boolean hasPermission(String resource, String action) {
        User user = getCurrentUser();
        if (user == null) {
            log.warn("No authenticated user found when checking permission: {}:{}", resource, action);
            return false;
        }

        return hasPermission(user, resource, action);
    }

    /**
     * Check if a specific user has a permission
     *
     * @param user User to check
     * @param resource Resource name
     * @param action Action name
     * @return true if user has permission, false otherwise
     */
    public boolean hasPermission(User user, String resource, String action) {
        // Super admins have all permissions
        if (isSuperAdmin(user)) {
            log.debug("User {} is SUPER_ADMIN - permission granted", user.getEmail());
            return true;
        }

        // Get all user permissions and check
        Set<Permission> permissions = getUserPermissions(user);

        boolean hasPermission = permissions.stream()
                .anyMatch(p ->
                        p.getResource().equalsIgnoreCase(resource) &&
                                p.getAction().equalsIgnoreCase(action)
                );

        if (hasPermission) {
            log.debug("Permission granted: user={}, permission={}:{}",
                    user.getEmail(), resource, action);
        } else {
            log.warn("Permission denied: user={}, permission={}:{}",
                    user.getEmail(), resource, action);
        }

        return hasPermission;
    }

    /**
     * Check if user has any of the specified roles
     *
     * @param roleNames Role names to check
     * @return true if user has at least one of the roles
     */
    public boolean hasAnyRole(String... roleNames) {
        User user = getCurrentUser();
        if (user == null) {
            return false;
        }

        Set<String> userRoles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        for (String roleName : roleNames) {
            if (userRoles.contains(roleName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Check if user has a specific role
     *
     * @param roleName Role name to check
     * @return true if user has the role
     */
    public boolean hasRole(String roleName) {
        return hasAnyRole(roleName);
    }

    /**
     * Get all permissions for a user (aggregated from all their roles)
     * Cached for performance
     *
     * @param user User to get permissions for
     * @return Set of all permissions
     */
    @Cacheable(value = "userPermissions", key = "#user.id")
    public Set<Permission> getUserPermissions(User user) {
        return user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .collect(Collectors.toSet());
    }

    /**
     * Check if user is a super admin
     *
     * @param user User to check
     * @return true if user has SUPER_ADMIN role
     */
    public boolean isSuperAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> SUPER_ADMIN_ROLE.equals(role.getName()));
    }

    /**
     * Check if current user is a super admin
     *
     * @return true if current user has SUPER_ADMIN role
     */
    public boolean isSuperAdmin() {
        User user = getCurrentUser();
        return user != null && isSuperAdmin(user);
    }

    /**
     * Check if user is a tenant admin
     *
     * @param user User to check
     * @return true if user has TENANT_ADMIN role
     */
    public boolean isTenantAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> "ROLE_TENANT_ADMIN".equals(role.getName()) ||
                        "ROLE_ADMIN".equals(role.getName()));
    }

    /**
     * Check if current user is a tenant admin
     *
     * @return true if current user has TENANT_ADMIN role
     */
    public boolean isTenantAdmin() {
        User user = getCurrentUser();
        return user != null && isTenantAdmin(user);
    }

    /**
     * Get the current authenticated user
     *
     * @return Current user or null if not authenticated
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        Object principal = authentication.getPrincipal();

        if (principal instanceof User) {
            return (User) principal;
        }

        return null;
    }

    /**
     * Require permission or throw exception
     *
     * @param resource Resource name
     * @param action Action name
     * @throws org.springframework.security.access.AccessDeniedException if permission denied
     */
    public void requirePermission(String resource, String action) {
        if (!hasPermission(resource, action)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    String.format("Access denied: Missing permission %s:%s", resource, action)
            );
        }
    }
}