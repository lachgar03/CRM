package com.crm.AuthService.role.services;

import com.crm.AuthService.exception.RoleNotFoundException;
import com.crm.AuthService.role.dtos.*;
import com.crm.AuthService.role.entities.Permission;
import com.crm.AuthService.role.entities.Role;
import com.crm.AuthService.role.repositories.PermissionRepository;
import com.crm.AuthService.role.repositories.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleServiceImpl implements RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        // Get all roles (including system roles)
        List<Role> roles = roleRepository.findAll();
        return roles.stream()
                .map(this::toRoleResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Role not found with id: " + id));
        return toRoleResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        // Check if role name already exists
        if (roleRepository.findByName(request.getName()).isPresent()) {
            throw new IllegalArgumentException("Role with name '" + request.getName() + "' already exists");
        }

        // Get permissions if provided
        Set<Permission> permissions = new HashSet<>();
        if (request.getPermissionIds() != null && !request.getPermissionIds().isEmpty()) {
            permissions = permissionRepository.findAllById(request.getPermissionIds())
                    .stream()
                    .collect(Collectors.toSet());
        }

        // Create role
        Role role = new Role();
        role.setName(request.getName());
        role.setDescription(request.getDescription());
        role.setIsSystemRole(false); // Custom roles are never system roles
        role.setPermissions(permissions);

        Role savedRole = roleRepository.save(role);
        log.info("Role created: id={}, name={}", savedRole.getId(), savedRole.getName());

        return toRoleResponse(savedRole);
    }

    @Override
    @Transactional
    public RoleResponse updateRole(Long id, UpdateRoleRequest request) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Role not found with id: " + id));

        // Prevent editing system roles
        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            throw new IllegalArgumentException("Cannot update system role");
        }

        // Update fields
        if (request.getName() != null) {
            // Check if new name already exists
            roleRepository.findByName(request.getName()).ifPresent(existingRole -> {
                if (!existingRole.getId().equals(id)) {
                    throw new IllegalArgumentException("Role with name '" + request.getName() + "' already exists");
                }
            });
            role.setName(request.getName());
        }
        if (request.getDescription() != null) {
            role.setDescription(request.getDescription());
        }

        Role updatedRole = roleRepository.save(role);
        log.info("Role updated: id={}, name={}", updatedRole.getId(), updatedRole.getName());

        return toRoleResponse(updatedRole);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Role not found with id: " + id));

        // Prevent deleting system roles
        if (Boolean.TRUE.equals(role.getIsSystemRole())) {
            throw new IllegalArgumentException("Cannot delete system role");
        }

        // TODO: Check if any users have this role before deleting
        // For now, we'll allow deletion (could implement soft delete)

        roleRepository.delete(role);
        log.info("Role deleted: id={}, name={}", role.getId(), role.getName());
    }

    @Override
    @Transactional
    public RoleResponse assignPermissions(Long id, Set<Long> permissionIds) {
        Role role = roleRepository.findById(id)
                .orElseThrow(() -> new RoleNotFoundException("Role not found with id: " + id));

        // Get permissions
        Set<Permission> permissions = permissionRepository.findAllById(permissionIds)
                .stream()
                .collect(Collectors.toSet());

        if (permissions.size() != permissionIds.size()) {
            throw new IllegalArgumentException("One or more permissions not found");
        }

        role.setPermissions(permissions);
        Role updatedRole = roleRepository.save(role);

        log.info("Permissions assigned to role: roleId={}, permissionCount={}",
                updatedRole.getId(), permissions.size());

        return toRoleResponse(updatedRole);
    }

    private RoleResponse toRoleResponse(Role role) {
        Set<PermissionDto> permissionDtos = role.getPermissions().stream()
                .map(permission -> PermissionDto.builder()
                        .id(permission.getId())
                        .resource(permission.getResource())
                        .action(permission.getAction())
                        .build())
                .collect(Collectors.toSet());

        return RoleResponse.builder()
                .id(role.getId())
                .name(role.getName())
                .description(role.getDescription())
                .isSystemRole(role.getIsSystemRole())
                .permissions(permissionDtos)
                .build();
    }
}