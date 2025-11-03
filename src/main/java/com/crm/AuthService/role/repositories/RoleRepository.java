package com.crm.AuthService.role.repositories;

import com.crm.AuthService.role.entities.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);
    Optional<Role> findByNameAndIsSystemRole(String name, boolean isSystemRole);
    long countByIdIn(Set<Long> roleIds);
}
