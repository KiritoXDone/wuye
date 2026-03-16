package com.wuye.auth.controller;

import com.wuye.auth.dto.RefreshTokenDTO;
import com.wuye.auth.dto.WechatLoginDTO;
import com.wuye.auth.service.AuthService;
import com.wuye.auth.vo.LoginVO;
import com.wuye.auth.vo.ProfileVO;
import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/auth/login/wechat")
    public ApiResponse<LoginVO> loginWechat(@Valid @RequestBody WechatLoginDTO dto) {
        return ApiResponse.success(authService.loginWechat(dto));
    }

    @PostMapping("/auth/refresh")
    public ApiResponse<LoginVO> refresh(@Valid @RequestBody RefreshTokenDTO dto) {
        return ApiResponse.success(authService.refresh(dto));
    }

    @GetMapping("/me/profile")
    public ApiResponse<ProfileVO> profile(@CurrentUser LoginUser loginUser) {
        return ApiResponse.success(authService.currentProfile(loginUser));
    }

    @PostMapping("/auth/logout")
    public ApiResponse<Void> logout(@CurrentUser LoginUser loginUser) {
        authService.logout(loginUser);
        return ApiResponse.success();
    }
}
