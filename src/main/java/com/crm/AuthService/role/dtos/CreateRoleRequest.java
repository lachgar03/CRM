package com.crm.AuthService.role.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateRoleRequest {
    @NotBlank(message = "Role name is required")
    private String name;

    private String description;

    private Set<Long> permissionIds;
}