package com.crm.AuthService.migration;

import com.crm.AuthService.security.TenantContextHolder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.EmptyInterceptor;
import org.springframework.stereotype.Component;

@Component
public class TenantSchemaInterceptor extends EmptyInterceptor {

    @PersistenceContext
    private EntityManager entityManager;

    private void setSearchPath() {
        Long tenantId = TenantContextHolder.getTenantId();
        if (tenantId != null) {
            String schemaName = "tenant_" + tenantId;
            // Execute the SQL command to switch schema
            entityManager.createNativeQuery("SET search_path TO " + schemaName + ", public")
                    .executeUpdate();
        }
    }

    @Override
    public boolean onLoad(Object entity, Object id, Object[] state, String[] propertyNames, org.hibernate.type.Type[] types) {
        setSearchPath();
        return false;
    }

    @Override
    public boolean onSave(Object entity, Object id, Object[] state, String[] propertyNames, org.hibernate.type.Type[] types) {
        setSearchPath();
        return false;
    }
}
