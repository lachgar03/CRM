package com.crm.AuthService.security;

import com.crm.AuthService.tenant.entities.Tenant;
import com.crm.AuthService.tenant.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filter to resolve Tenant ID from X-Tenant-Subdomain header
 * for public endpoints like /login and /register.
 *
 * This filter runs BEFORE JwtAuthenticationFilter.
 * The JwtAuthenticationFilter is responsible for clearing the context.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TenantResolutionFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepository;
    public static final String TENANT_HEADER = "X-Tenant-Subdomain";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // Only attempt to resolve if context is not already set
        if (TenantContextHolder.getTenantId() == null) {
            String subdomain = request.getHeader(TENANT_HEADER);

            if (subdomain != null && !subdomain.isBlank()) {
                try {
                    Optional<Tenant> tenantOpt = tenantRepository.findBySubdomain(subdomain.toLowerCase());
                    if (tenantOpt.isPresent()) {
                        // Set tenant ID in context
                        TenantContextHolder.setTenantId(tenantOpt.get().getId());
                        log.debug("Tenant context set from {} header: {} (Tenant ID: {})",
                                TENANT_HEADER, subdomain, tenantOpt.get().getId());
                    } else {
                        log.warn("Invalid {} header received: {}", TENANT_HEADER, subdomain);
                    }
                } catch (Exception e) {
                    log.error("Error resolving tenant from subdomain: {}", subdomain, e);
                }
            }
        }

        // Continue the filter chain
        filterChain.doFilter(request, response);
    }
}