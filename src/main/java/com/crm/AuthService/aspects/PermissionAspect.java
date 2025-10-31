package com.crm.AuthService.aspects;

import com.crm.AuthService.role.services.PermissionService;
import com.crm.AuthService.annotations.RequirePermission;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * AOP Aspect to intercept methods annotated with @RequirePermission
 * and enforce permission checks before method execution
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionService permissionService;

    /**
     * Intercepts all methods annotated with @RequirePermission
     *
     * @param joinPoint The method being intercepted
     * @return The result of the method if permission check passes
     * @throws Throwable if method execution fails or permission is denied
     */
    @Around("@annotation(com.crm.AuthService.annotations.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        RequirePermission annotation = method.getAnnotation(RequirePermission.class);

        if (annotation == null) {
            // Fallback: check class-level annotation
            annotation = joinPoint.getTarget().getClass().getAnnotation(RequirePermission.class);
        }

        if (annotation != null) {
            String resource = annotation.resource();
            String action = annotation.action();

            log.debug("Permission check: method={}, resource={}, action={}",
                    method.getName(), resource, action);

            // Check if user has the required permission
            if (!permissionService.hasPermission(resource, action)) {
                log.warn("Access denied: method={}, resource={}, action={}",
                        method.getName(), resource, action);
                throw new AccessDeniedException(
                        String.format("Access denied: Missing permission %s:%s", resource, action)
                );
            }

            log.debug("Permission granted: method={}, resource={}, action={}",
                    method.getName(), resource, action);
        }

        // Permission granted - proceed with method execution
        return joinPoint.proceed();
    }

    /**
     * Intercepts methods in classes annotated with @RequirePermission
     * (class-level annotation applies to all methods)
     *
     * @param joinPoint The method being intercepted
     * @return The result of the method if permission check passes
     * @throws Throwable if method execution fails or permission is denied
     */
    @Around("@within(com.crm.AuthService.annotations.RequirePermission)")
    public Object checkClassLevelPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        RequirePermission annotation = joinPoint.getTarget().getClass()
                .getAnnotation(RequirePermission.class);

        if (annotation != null) {
            String resource = annotation.resource();
            String action = annotation.action();

            log.debug("Class-level permission check: class={}, resource={}, action={}",
                    joinPoint.getTarget().getClass().getSimpleName(), resource, action);

            if (!permissionService.hasPermission(resource, action)) {
                log.warn("Access denied (class-level): class={}, resource={}, action={}",
                        joinPoint.getTarget().getClass().getSimpleName(), resource, action);
                throw new AccessDeniedException(
                        String.format("Access denied: Missing permission %s:%s", resource, action)
                );
            }
        }

        return joinPoint.proceed();
    }
}