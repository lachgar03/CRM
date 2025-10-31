package com.crm.AuthService.user.repositories;

import com.crm.AuthService.user.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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
    @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND " +
            "(LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<User> findByTenantIdAndSearch(
            @Param("tenantId") Long tenantId,
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

    /**
     * Check if email exists in tenant (excluding specific user ID)
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u " +
            "WHERE u.email = :email AND u.tenant.id = :tenantId AND u.id <> :excludeUserId")
    boolean existsByEmailAndTenantIdExcludingUser(
            @Param("email") String email,
            @Param("tenantId") Long tenantId,
            @Param("excludeUserId") Long excludeUserId
    );
}