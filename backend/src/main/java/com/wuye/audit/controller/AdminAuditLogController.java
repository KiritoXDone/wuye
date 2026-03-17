package com.wuye.audit.controller;

import com.wuye.audit.dto.AuditLogListQuery;
import com.wuye.audit.service.AuditLogService;
import com.wuye.audit.vo.AuditLogVO;
import com.wuye.common.api.ApiResponse;
import com.wuye.common.api.PageResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AdminAuditLogController {

    private final AuditLogService auditLogService;

    public AdminAuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @GetMapping
    public ApiResponse<PageResponse<AuditLogVO>> list(@CurrentUser LoginUser loginUser, AuditLogListQuery query) {
        return ApiResponse.success(auditLogService.page(loginUser, query));
    }
}
