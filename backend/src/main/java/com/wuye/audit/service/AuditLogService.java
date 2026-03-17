package com.wuye.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.audit.dto.AuditLogListQuery;
import com.wuye.audit.entity.AuditLog;
import com.wuye.audit.mapper.AuditLogMapper;
import com.wuye.audit.vo.AuditLogVO;
import com.wuye.common.api.PageResponse;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogMapper auditLogMapper;
    private final AccessGuard accessGuard;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogMapper auditLogMapper,
                           AccessGuard accessGuard,
                           ObjectMapper objectMapper) {
        this.auditLogMapper = auditLogMapper;
        this.accessGuard = accessGuard;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(LoginUser loginUser, String bizType, String bizId, String action, Object detail) {
        AuditLog auditLog = new AuditLog();
        auditLog.setBizType(bizType);
        auditLog.setBizId(bizId);
        auditLog.setAction(action);
        auditLog.setOperatorId(loginUser == null ? null : loginUser.accountId());
        HttpServletRequest request = currentRequest();
        auditLog.setIp(resolveIp(request));
        auditLog.setUserAgent(trimToLength(request == null ? null : request.getHeader("User-Agent"), 255));
        auditLog.setDetailJson(writeJson(detail));
        auditLogMapper.insert(auditLog);
    }

    public PageResponse<AuditLogVO> page(LoginUser loginUser, AuditLogListQuery query) {
        accessGuard.requireAnyRole(loginUser, "ADMIN", "FINANCE");
        int pageNo = query.getPageNo() == null || query.getPageNo() < 1 ? 1 : query.getPageNo();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : query.getPageSize();
        int offset = (pageNo - 1) * pageSize;
        LocalDateTime createdAtStart = query.getCreatedAtStart();
        LocalDateTime createdAtEnd = query.getCreatedAtEnd();
        List<AuditLogVO> list = auditLogMapper.listPage(
                query.getBizType(),
                query.getBizId(),
                query.getOperatorId(),
                createdAtStart,
                createdAtEnd,
                offset,
                pageSize
        );
        long total = auditLogMapper.count(
                query.getBizType(),
                query.getBizId(),
                query.getOperatorId(),
                createdAtStart,
                createdAtEnd
        );
        return new PageResponse<>(list, pageNo, pageSize, total);
    }

    private String writeJson(Object detail) {
        if (detail == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(detail);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize audit detail json", ex);
        }
    }

    private HttpServletRequest currentRequest() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return null;
        }
        return attributes.getRequest();
    }

    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return trimToLength(forwardedFor.split(",")[0].trim(), 64);
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return trimToLength(realIp.trim(), 64);
        }
        return trimToLength(request.getRemoteAddr(), 64);
    }

    private String trimToLength(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
