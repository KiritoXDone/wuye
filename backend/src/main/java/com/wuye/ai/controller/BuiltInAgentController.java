package com.wuye.ai.controller;

import com.wuye.ai.service.BuiltInAgentService;
import com.wuye.ai.vo.AgentAdminBillStatsVO;
import com.wuye.ai.vo.AgentResidentBillSummaryVO;
import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/agent")
public class BuiltInAgentController {

    private final BuiltInAgentService builtInAgentService;

    public BuiltInAgentController(BuiltInAgentService builtInAgentService) {
        this.builtInAgentService = builtInAgentService;
    }

    @GetMapping("/me/bill-summary")
    public ApiResponse<AgentResidentBillSummaryVO> residentBillSummary(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(builtInAgentService.residentBillSummary(loginUser));
    }

    @GetMapping("/admin/bill-stats")
    public ApiResponse<AgentAdminBillStatsVO> adminBillStats(@CurrentUser LoginUser loginUser,
                                                             @RequestParam(required = false) Integer periodYear,
                                                             @RequestParam(required = false) Integer periodMonth) {
        return ApiResponse.success(builtInAgentService.adminBillStats(loginUser, periodYear, periodMonth));
    }
}
