package com.wuye.ai.controller;

import com.wuye.ai.dto.AgentCommandConfirmRequest;
import com.wuye.ai.dto.AgentCommandPreviewRequest;
import com.wuye.ai.dto.AgentConversationListQuery;
import com.wuye.ai.dto.AgentConversationRequest;
import com.wuye.ai.service.BuiltInAgentService;
import com.wuye.ai.vo.AgentAdminBillStatsVO;
import com.wuye.ai.vo.AgentCommandExecutionVO;
import com.wuye.ai.vo.AgentCommandPreviewVO;
import com.wuye.ai.vo.AgentConversationListItemVO;
import com.wuye.ai.vo.AgentConversationVO;
import com.wuye.ai.vo.AgentResidentBillSummaryVO;
import com.wuye.common.api.ApiResponse;
import com.wuye.common.api.PageResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    @PostMapping("/commands/preview")
    public ApiResponse<AgentCommandPreviewVO> preview(@CurrentUser LoginUser loginUser,
                                                      @Valid @RequestBody AgentCommandPreviewRequest request) {
        return ApiResponse.success(builtInAgentService.preview(loginUser, request));
    }

    @PostMapping("/commands/confirm")
    public ApiResponse<AgentCommandExecutionVO> confirm(@CurrentUser LoginUser loginUser,
                                                        @Valid @RequestBody AgentCommandConfirmRequest request) {
        return ApiResponse.success(builtInAgentService.confirm(loginUser, request));
    }

    @GetMapping("/commands/{commandId}")
    public ApiResponse<AgentCommandExecutionVO> getCommand(@CurrentUser LoginUser loginUser,
                                                           @PathVariable String commandId) {
        return ApiResponse.success(builtInAgentService.getCommand(loginUser, commandId));
    }

    @PostMapping("/conversation")
    public ApiResponse<AgentConversationVO> converse(@CurrentUser LoginUser loginUser,
                                                     @Valid @RequestBody AgentConversationRequest request) {
        return ApiResponse.success(builtInAgentService.converse(loginUser, request));
    }

    @PostMapping(path = "/conversation/stream", produces = "text/event-stream")
    public SseEmitter streamConversation(@CurrentUser LoginUser loginUser,
                                         @Valid @RequestBody AgentConversationRequest request) {
        return builtInAgentService.streamConversation(loginUser, request);
    }

    @GetMapping("/conversation/sessions")
    public ApiResponse<PageResponse<AgentConversationListItemVO>> listConversations(@CurrentUser LoginUser loginUser,
                                                                                     AgentConversationListQuery query) {
        return ApiResponse.success(builtInAgentService.listConversations(loginUser, query));
    }

    @GetMapping("/conversation/{sessionId}")
    public ApiResponse<AgentConversationVO> getConversation(@CurrentUser LoginUser loginUser,
                                                            @PathVariable String sessionId) {
        return ApiResponse.success(builtInAgentService.getConversation(loginUser, sessionId));
    }
}
