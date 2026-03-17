package com.wuye.agent.controller;

import com.wuye.agent.dto.AgentGroupAssignDTO;
import com.wuye.agent.service.AgentAuthorizationService;
import com.wuye.agent.vo.AgentGroupVO;
import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/agent-groups")
public class AgentAuthorizationAdminController {

    private final AgentAuthorizationService agentAuthorizationService;

    public AgentAuthorizationAdminController(AgentAuthorizationService agentAuthorizationService) {
        this.agentAuthorizationService = agentAuthorizationService;
    }

    @PostMapping
    public ApiResponse<AgentGroupVO> assign(@CurrentUser LoginUser loginUser,
                                            @Valid @RequestBody AgentGroupAssignDTO dto) {
        return ApiResponse.success(agentAuthorizationService.assign(loginUser, dto));
    }

    @GetMapping
    public ApiResponse<List<AgentGroupVO>> list(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(agentAuthorizationService.listAll(loginUser));
    }
}
