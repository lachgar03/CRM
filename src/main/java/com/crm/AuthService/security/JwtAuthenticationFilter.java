package com.crm.AuthService.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final TenantSchemaResolver tenantSchemaResolver;
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            final String authHeader = request.getHeader("Authorization");
            final String jwt;
            final String username;
            final Long tenantId;

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                filterChain.doFilter(request, response);
                return;
            }

            jwt = authHeader.substring(7);

            try {
                username = jwtService.extractUsername(jwt);
                tenantId = jwtService.extractTenantId(jwt);

                if (tenantId != null) {
                    TenantContextHolder.setTenantId(tenantId);
                    tenantSchemaResolver.setCurrentTenantSchema(tenantId);
                    log.debug("Tenant context established from JWT: tenantId={}", tenantId);
                }

                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                    if (jwtService.isTokenValid(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        userDetails,
                                        null,
                                        userDetails.getAuthorities()
                                );

                        authToken.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request)
                        );

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        log.debug("User authenticated successfully: username={}, tenantId={}",
                                username, tenantId);
                    } else {
                        log.warn("Invalid JWT token for user: {}", username);
                    }
                }
            } catch (Exception e) {
                log.error("Error processing JWT token: {}", e.getMessage());

            }

            filterChain.doFilter(request, response);

        } finally {
            tenantSchemaResolver.clearCurrentTenantSchema();
            TenantContextHolder.clear();
            log.trace("Tenant context and shama resolver cleared after request");
        }
    }
}