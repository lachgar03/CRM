package com.crm.AuthService.tenant.repository;

import com.crm.AuthService.tenant.entities.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TenantRepository extends JpaRepository<Tenant, Long> {

}
