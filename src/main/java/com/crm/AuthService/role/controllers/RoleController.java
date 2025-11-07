package com.crm.AuthService.role.controllers;

import com.crm.AuthService.annotations.RequirePermission;
import com.crm.AuthService.role.dtos.*;
import com.crm.AuthService.role.services.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;


    @PostMapping
    @RequirePermission(resource = "ROLE", action = "MANAGE")
    public ResponseEntity<RoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roleService.createRole(request));
    }


    @GetMapping
    @RequirePermission(resource = "ROLE", action = "READ")
    public ResponseEntity<List<RoleResponse>> getAllRoles() {
        return ResponseEntity.ok(roleService.getAllRoles());
    }


    @GetMapping("/{id}")
    @RequirePermission(resource = "ROLE", action = "READ")
    public ResponseEntity<RoleResponse> getRoleById(@PathVariable Long id) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    @PutMapping("/{id}")
    @RequirePermission(resource = "ROLE", action = "MANAGE")
    public ResponseEntity<RoleResponse> updateRole(@PathVariable Long id, @Valid @RequestBody UpdateRoleRequest request) {
        return ResponseEntity.ok(roleService.updateRole(id, request));
    }


    @PostMapping("/{id}/assign-permissions")
    @RequirePermission(resource = "ROLE", action = "MANAGE")
    public ResponseEntity<RoleResponse> assignPermissions(@PathVariable Long id, @Valid @RequestBody AssignPermissionsRequest request) {
        return ResponseEntity.ok(roleService.assignPermissions(id, request.getPermissionIds()));
    }

    @DeleteMapping("/{id}")
    @RequirePermission(resource = "ROLE", action = "MANAGE")
    public ResponseEntity<Void> deleteRole(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}