package com.ems.modules.audit.aspect;

import com.ems.modules.audit.annotation.Auditable;
import com.ems.modules.audit.model.AuditLog;
import com.ems.modules.audit.repository.AuditLogRepository;
import com.ems.modules.auth.model.User;
import com.ems.modules.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Slf4j
@Aspect
@Component
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AuditAspect(AuditLogRepository auditLogRepository, UserRepository userRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        String action = auditable.action();
        String entityName = auditable.entity();
        
        log.debug("AOP Audit interceptor triggered for action: {}, entity: {}", action, entityName);

        // Capture arguments before proceeding (to record as old/new values)
        String argumentsJson = "";
        try {
            if (joinPoint.getArgs().length > 0) {
                argumentsJson = objectMapper.writeValueAsString(joinPoint.getArgs()[0]);
            }
        } catch (Exception e) {
            argumentsJson = "[Unserializable arguments: " + e.getMessage() + "]";
        }

        // Proceed with original method call
        Object result = joinPoint.proceed();

        // Record the audit log in a new transaction to ensure it is committed independently
        try {
            saveAuditLog(action, entityName, argumentsJson, result);
        } catch (Exception e) {
            log.error("Failed to commit audit log. Error: {}", e.getMessage());
        }

        return result;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    protected void saveAuditLog(String action, String entityName, String argumentsJson, Object result) {
        // Resolve currently logged-in user
        User currentUser = null;
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getPrincipal())) {
            
            String username = authentication.getName();
            currentUser = userRepository.findByUsername(username).orElse(null);
        }

        // Resolve client IP Address
        String ipAddress = "0.0.0.0";
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String xfHeader = request.getHeader("X-Forwarded-For");
            if (xfHeader != null) {
                ipAddress = xfHeader.split(",")[0];
            } else {
                ipAddress = request.getRemoteAddr();
            }
        }

        // Serialize returned result (represents the updated state of the entity)
        String resultJson = "";
        try {
            if (result != null) {
                resultJson = objectMapper.writeValueAsString(result);
            }
        } catch (Exception e) {
            resultJson = "[Unserializable result]";
        }

        AuditLog auditLog = AuditLog.builder()
                .user(currentUser)
                .action(action)
                .entityName(entityName)
                .oldValue(argumentsJson) // Arguments represent the inputs/old state
                .newValue(resultJson)    // Result represents the outputs/new state
                .ipAddress(ipAddress)
                .build();

        auditLogRepository.save(auditLog);
        log.debug("AuditLog saved successfully for user: {}, action: {}", 
                currentUser != null ? currentUser.getUsername() : "ANONYMOUS", action);
    }
}
