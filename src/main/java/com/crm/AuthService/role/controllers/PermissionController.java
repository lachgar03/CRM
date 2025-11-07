package com.crm.AuthService.role.controllers;

import com.crm.AuthService.role.dtos.PermissionDto;
import com.crm.AuthService.role.entities.Permission;
import com.crm.AuthService.role.repositories.PermissionRepository;
import com.crm.AuthService.annotations.RequirePermission;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionRepository permissionRepository;

    @GetMapping
    @RequirePermission(resource = "PERMISSION", action = "READ")
    public ResponseEntity<List<PermissionDto>> getAllPermissions() {
        List<Permission> permissions = permissionRepository.findAll();

        List<PermissionDto> permissionDtos = permissions.stream()
                .map(permission -> PermissionDto.builder()
                        .id(permission.getId())
                        .resource(permission.getResource())
                        .action(permission.getAction())
                        .build())
                .collect(Collectors.toList());

        return ResponseEntity.ok(permissionDtos);
    }
}