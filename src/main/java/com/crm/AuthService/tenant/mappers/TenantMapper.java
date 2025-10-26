package com.crm.AuthService.tenant.mappers;

import com.crm.AuthService.tenant.dtos.TenantDto;
import com.crm.AuthService.tenant.entities.Tenant;
import org.mapstruct.Mapper;


@Mapper(componentModel = "spring")
public interface TenantMapper {
    TenantDto toDto(Tenant tenant);
    Tenant toEntity(TenantDto tenantDto);
}
