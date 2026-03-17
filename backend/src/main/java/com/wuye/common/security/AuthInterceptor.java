package com.wuye.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.common.api.ApiResponse;
import com.wuye.auth.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class AuthInterceptor implements HandlerInterceptor {

    public static final String LOGIN_USER_ATTR = "loginUser";

    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/v1/auth/login/wechat",
            "/api/v1/auth/refresh",
            "/api/v1/admin/auth/login/password",
            "/api/v1/callbacks/wechatpay",
            "/api/v1/callbacks/alipay",
            "/error"
    );

    private final JwtService jwtService;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public AuthInterceptor(JwtService jwtService, ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        if (PUBLIC_PATHS.stream().anyMatch(pattern -> pathMatcher.match(pattern, path))) {
            return true;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            writeUnauthorized(response, "UNAUTHORIZED", "未登录或 Token 缺失");
            return false;
        }

        try {
            LoginUser loginUser = jwtService.parseAccessToken(authorization.substring(7));
            request.setAttribute(LOGIN_USER_ATTR, loginUser);
            return true;
        } catch (Exception ex) {
            writeUnauthorized(response, "UNAUTHORIZED", "Token 无效或已过期");
            return false;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String code, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.failure(code, message)));
    }
}
