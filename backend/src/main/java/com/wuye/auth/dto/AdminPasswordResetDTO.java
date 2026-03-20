package com.wuye.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AdminPasswordResetDTO {

    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, max = 32, message = "密码长度需为8-32位")
    private String newPassword;

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
