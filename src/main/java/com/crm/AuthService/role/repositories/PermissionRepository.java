package com.crm.AuthService.role.repositories;

import com.crm.AuthService.role.entities.Permission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByResourceAndAction(String resource, String action);
}
