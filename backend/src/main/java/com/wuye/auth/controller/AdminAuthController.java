package com.wuye.auth.controller;

import com.wuye.auth.dto.AdminPasswordLoginDTO;
import com.wuye.auth.service.AuthService;
import com.wuye.auth.vo.LoginVO;
import com.wuye.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/admin/auth")
public class AdminAuthController {

    private final AuthService authService;

    public AdminAuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login/password")
    public ApiResponse<LoginVO> loginByPassword(@Valid @RequestBody AdminPasswordLoginDTO dto) {
        return ApiResponse.success(authService.loginAdmin(dto));
    }
}
