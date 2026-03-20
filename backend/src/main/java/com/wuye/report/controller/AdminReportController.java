package com.wuye.report.controller;

import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import com.wuye.report.service.AdminMonthlyReportService;
import com.wuye.report.vo.AdminMonthlyReportVO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/reports")
public class AdminReportController {

    private final AdminMonthlyReportService adminMonthlyReportService;

    public AdminReportController(AdminMonthlyReportService adminMonthlyReportService) {
        this.adminMonthlyReportService = adminMonthlyReportService;
    }

    @GetMapping("/property-yearly")
    public ApiResponse<AdminMonthlyReportVO> propertyYearly(@CurrentUser LoginUser loginUser,
                                                            @RequestParam("periodYear") @NotNull Integer periodYear) {
        return ApiResponse.success(adminMonthlyReportService.propertyYearly(loginUser, periodYear));
    }

    @GetMapping("/water-monthly")
    public ApiResponse<AdminMonthlyReportVO> waterMonthly(@CurrentUser LoginUser loginUser,
                                                          @RequestParam("periodYear") @NotNull Integer periodYear,
                                                          @RequestParam("periodMonth") @NotNull @Min(1) @Max(12) Integer periodMonth) {
        return ApiResponse.success(adminMonthlyReportService.waterMonthly(loginUser, periodYear, periodMonth));
    }

    @GetMapping("/monthly")
    public ApiResponse<AdminMonthlyReportVO> monthly(@CurrentUser LoginUser loginUser,
                                                     @RequestParam("periodYear") @NotNull Integer periodYear,
                                                     @RequestParam("periodMonth") @NotNull @Min(1) @Max(12) Integer periodMonth) {
        return ApiResponse.success(adminMonthlyReportService.monthly(loginUser, periodYear, periodMonth));
    }
}
