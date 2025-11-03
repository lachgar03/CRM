package com.crm.AuthService.user.repositories;

import com.crm.AuthService.user.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email (global search)
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by ID and tenant ID (tenant-scoped)
     */
    Optional<User> findByIdAndTenantId(Long id, Long tenantId);

    /**
     * Find all users by tenant ID (paginated)
     */
    Page<User> findByTenantId(Long tenantId, Pageable pageable);

    /**
     * Search users by tenant ID with text search (email, first name, last name)
     */

    Page<User> findByTenantIdAndSearch(
            @Param("search") String search,
            Pageable pageable
    );

    /**
     * Count users by tenant ID
     */
    long countByTenantId(Long tenantId);

    /**
     * Check if email exists (global)
     */
    boolean existsByEmail(String email);


}