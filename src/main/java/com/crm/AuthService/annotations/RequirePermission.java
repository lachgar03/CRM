package com.crm.AuthService.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to enforce permission-based access control
 *
 * Usage:
 * @RequirePermission(resource = "USER", action = "CREATE")
 * public void createUser(...) { ... }
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequirePermission {

    /**
     * Resource name (e.g., USER, CUSTOMER, ROLE, TICKET)
     */
    String resource();

    /**
     * Action name (e.g., CREATE, READ, UPDATE, DELETE)
     */
    String action();

    /**
     * Optional description for documentation
     */
    String description() default "";
}