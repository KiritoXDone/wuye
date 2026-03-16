package com.wuye.auth.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminPasswordLoginDTO {

    @NotBlank(message = "username 不能为空")
    private String username;
    @NotBlank(message = "password 不能为空")
    private String password;
    private String captchaId;
    private String captchaCode;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public String getCaptchaCode() {
        return captchaCode;
    }

    public void setCaptchaCode(String captchaCode) {
        this.captchaCode = captchaCode;
    }
}
