package com.crm.AuthService.tenant.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TenantDto {
    private String name;
    private String subdomain;
    private String subscription_plan;

}
