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


@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final PermissionService permissionService;


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

        return joinPoint.proceed();
    }


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