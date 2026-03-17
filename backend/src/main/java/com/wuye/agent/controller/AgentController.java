package com.wuye.agent.controller;

import com.wuye.agent.service.AgentAuthorizationService;
import com.wuye.agent.vo.AgentGroupVO;
import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/agent")
public class AgentController {

    private final AgentAuthorizationService agentAuthorizationService;

    public AgentController(AgentAuthorizationService agentAuthorizationService) {
        this.agentAuthorizationService = agentAuthorizationService;
    }

    @GetMapping("/groups")
    public ApiResponse<List<AgentGroupVO>> myGroups(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(agentAuthorizationService.listMyGroups(loginUser));
    }
}
