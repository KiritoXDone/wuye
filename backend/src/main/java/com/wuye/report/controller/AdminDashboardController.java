package com.wuye.report.controller;

import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import com.wuye.report.service.AdminDashboardService;
import com.wuye.report.vo.AdminDashboardSummaryVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<AdminDashboardSummaryVO> summary(@CurrentUser LoginUser loginUser,
                                                        @RequestParam(value = "periodYear", required = false) Integer periodYear,
                                                        @RequestParam(value = "periodMonth", required = false) Integer periodMonth) {
        return ApiResponse.success(adminDashboardService.summary(loginUser, periodYear, periodMonth));
    }
}
