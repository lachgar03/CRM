package com.crm.AuthService.tenant.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "tenants", schema = "public")
@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
public class Tenant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String subdomain;

    @Column(nullable = false, length = 100)
    private String status;

    @Column(name = "schema_name", length = 63)
    private String schemaName;

    @Column(name = "subscription_plan", length = 50)
    private String subscription_plan;

    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = "ACTIVE";
        }
    }
}