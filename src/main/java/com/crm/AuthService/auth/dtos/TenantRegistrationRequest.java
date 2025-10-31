package com.crm.AuthService.auth.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TenantRegistrationRequest {
    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Subdomain is required")
    @Pattern(regexp = "^[a-z0-9-]+$", message = "Subdomain must be lowercase alphanumeric")
    private String subdomain;

    private String subscriptionPlan = "FREE"; // Default
    @NotBlank(message = "Admin email is required")
    @Email
    private String adminEmail;

    @NotBlank(message = "Admin password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String adminPassword;

    @NotBlank(message = "Admin first name is required")
    private String adminFirstName;

    @NotBlank(message = "Admin last name is required")
    private String adminLastName;
}
