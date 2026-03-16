package com.wuye.common.security;

import java.util.List;

public record LoginUser(Long accountId,
                        String accountType,
                        String realName,
                        List<String> roles,
                        String dataScope,
                        List<Long> groupIds) {

    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }
}
