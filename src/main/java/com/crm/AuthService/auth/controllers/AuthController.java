package com.crm.AuthService.auth.controllers;

import com.crm.AuthService.auth.dtos.AuthResponse;
import com.crm.AuthService.auth.dtos.LoginRequest;
import com.crm.AuthService.auth.dtos.TenantRegistrationRequest;
import com.crm.AuthService.auth.services.LoginService;
import com.crm.AuthService.auth.services.RefreshTokenService;
import com.crm.AuthService.auth.services.TenantRegistrationService;
import com.crm.AuthService.exception.TenantNotFoundException;
import com.crm.AuthService.tenant.entities.Tenant;
import com.crm.AuthService.tenant.repository.TenantRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final LoginService loginService;
    private final TenantRegistrationService tenantRegistrationService;
    private final RefreshTokenService refreshTokenService;
    private final TenantRepository tenantRepository;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        AuthResponse response = loginService.login(loginRequest);
        return ResponseEntity.ok(response);
    }


    @PostMapping("/register")
    public ResponseEntity<String> register(
                                            @Valid @RequestBody TenantRegistrationRequest registrationRequest) {

        tenantRegistrationService.registerTenant(registrationRequest);

        String responseMessage = String.format(
                "Tenant registration for '%s' accepted. Provisioning is in progress.",
                registrationRequest.getSubdomain()
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(responseMessage);
    }


    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("Le refresh token est obligatoire");
        }
        return ResponseEntity.ok(refreshTokenService.refreshToken(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        return ResponseEntity.ok("Déconnexion réussie");
    }


    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Login , Tenant Registration and Refresh token Services are running");
    }
    @GetMapping("/provision-status/{subdomain}")
    public ResponseEntity<Map<String, String>> getProvisioningStatus(@PathVariable String subdomain) {
        Tenant tenant = tenantRepository.findBySubdomain(subdomain.toLowerCase())

                .orElseThrow(() -> new TenantNotFoundException("Tenant not found: " + subdomain));

        Map<String, String> response = Map.of(
                "subdomain", tenant.getSubdomain(),
                "status", tenant.getStatus()
        );

        return ResponseEntity.ok(response);
    }

}