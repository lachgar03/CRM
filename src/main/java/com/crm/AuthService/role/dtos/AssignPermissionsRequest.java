package com.crm.AuthService.role.dtos;

import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignPermissionsRequest {
    @NotEmpty(message = "At least one permission is required")
    private Set<Long> permissionIds;
}