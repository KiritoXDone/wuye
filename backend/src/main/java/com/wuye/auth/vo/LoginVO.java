package com.wuye.auth.vo;

import java.util.List;

public class LoginVO {

    private String accessToken;
    private String refreshToken;
    private long expiresIn;
    private Long accountId;
    private String accountType;
    private List<String> roles;
    private Boolean needResetPassword;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getAccountType() {
        return accountType;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public Boolean getNeedResetPassword() {
        return needResetPassword;
    }

    public void setNeedResetPassword(Boolean needResetPassword) {
        this.needResetPassword = needResetPassword;
    }
}
