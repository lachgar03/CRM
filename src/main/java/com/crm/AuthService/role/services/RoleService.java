package com.crm.AuthService.role.services;

import com.crm.AuthService.role.dtos.CreateRoleRequest;
import com.crm.AuthService.role.dtos.RoleResponse;
import com.crm.AuthService.role.dtos.UpdateRoleRequest;

import java.util.List;
import java.util.Set;

public interface RoleService {
    List<RoleResponse> getAllRoles();
    RoleResponse getRoleById(Long id);
    RoleResponse createRole(CreateRoleRequest request);
    RoleResponse updateRole(Long id, UpdateRoleRequest request);
    void deleteRole(Long id);
    RoleResponse assignPermissions(Long id, Set<Long> permissionIds);
}
