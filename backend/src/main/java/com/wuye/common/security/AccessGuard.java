package com.wuye.common.security;

import com.wuye.common.exception.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class AccessGuard {

    public void requireRole(LoginUser loginUser, String role) {
        if (loginUser == null || !loginUser.hasRole(role)) {
            throw new BusinessException("FORBIDDEN", "无权限访问该资源", HttpStatus.FORBIDDEN);
        }
    }

    public void requireSelfRoom(LoginUser loginUser, boolean allowed) {
        if (loginUser == null || !allowed) {
            throw new BusinessException("FORBIDDEN", "数据范围不足", HttpStatus.FORBIDDEN);
        }
    }

    public void requireAnyRole(LoginUser loginUser, String... roles) {
        if (loginUser == null || roles == null || Arrays.stream(roles).noneMatch(loginUser::hasRole)) {
            throw new BusinessException("FORBIDDEN", "无权限访问该资源", HttpStatus.FORBIDDEN);
        }
    }
}
