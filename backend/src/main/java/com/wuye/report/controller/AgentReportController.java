package com.wuye.report.controller;

import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import com.wuye.report.service.AgentReportService;
import com.wuye.report.vo.AgentMonthlyReportVO;
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
@RequestMapping("/api/v1/agent/reports")
public class AgentReportController {

    private final AgentReportService agentReportService;

    public AgentReportController(AgentReportService agentReportService) {
        this.agentReportService = agentReportService;
    }

    @GetMapping("/monthly")
    public ApiResponse<AgentMonthlyReportVO> monthly(@CurrentUser LoginUser loginUser,
                                                     @RequestParam("groupId") Long groupId,
                                                     @RequestParam("periodYear") @NotNull Integer periodYear,
                                                     @RequestParam("periodMonth") @NotNull @Min(1) @Max(12) Integer periodMonth) {
        return ApiResponse.success(agentReportService.monthly(loginUser, groupId, periodYear, periodMonth));
    }
}
