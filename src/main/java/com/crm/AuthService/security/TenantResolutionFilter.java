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


@Slf4j
@Component
@RequiredArgsConstructor
public class TenantResolutionFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepository;
    public static final String TENANT_HEADER = "X-Tenant-Subdomain";
    private final TenantSchemaResolver tenantSchemaResolver;
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if (TenantContextHolder.getTenantId() == null) {
            String subdomain = request.getHeader(TENANT_HEADER);

            if (subdomain != null && !subdomain.isBlank()) {
                try {
                    Optional<Tenant> tenantOpt = tenantRepository.findBySubdomain(subdomain.toLowerCase());
                    if (tenantOpt.isPresent()) {
                        Long tenantId = tenantOpt.get().getId();
                        TenantContextHolder.setTenantId(tenantId);
                        tenantSchemaResolver.setCurrentTenantSchema(tenantId);
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

        filterChain.doFilter(request, response);
    }
}