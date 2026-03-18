package com.wuye.ai.controller;

import com.wuye.ai.dto.AiRuntimeConfigUpdateDTO;
import com.wuye.ai.service.AiRuntimeConfigService;
import com.wuye.ai.vo.AiRuntimeConfigVO;
import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/ai/runtime-config")
public class AdminAiRuntimeConfigController {

    private final AiRuntimeConfigService aiRuntimeConfigService;

    public AdminAiRuntimeConfigController(AiRuntimeConfigService aiRuntimeConfigService) {
        this.aiRuntimeConfigService = aiRuntimeConfigService;
    }

    @GetMapping
    public ApiResponse<AiRuntimeConfigVO> get(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(aiRuntimeConfigService.get(loginUser));
    }

    @PutMapping
    public ApiResponse<AiRuntimeConfigVO> save(@CurrentUser LoginUser loginUser,
                                               @Valid @RequestBody AiRuntimeConfigUpdateDTO dto) {
        return ApiResponse.success(aiRuntimeConfigService.save(loginUser, dto));
    }
}
