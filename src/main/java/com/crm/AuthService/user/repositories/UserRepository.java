package com.crm.AuthService.user.repositories;

import com.crm.AuthService.user.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {


    Optional<User> findByEmail(String email);


    Optional<User> findByIdAndTenantId(Long id, Long tenantId);


    Page<User> findByTenantId(Long tenantId, Pageable pageable);



    Page<User> findByTenantIdAndSearch(
            @Param("search") String search,
            Pageable pageable
    );


    long countByTenantId(Long tenantId);

    boolean existsByEmail(String email);


}