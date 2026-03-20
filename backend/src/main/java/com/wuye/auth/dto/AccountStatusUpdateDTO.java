package com.wuye.auth.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class AccountStatusUpdateDTO {

    @NotNull(message = "状态不能为空")
    @Pattern(regexp = "0|1", message = "状态仅支持 0 或 1")
    private String status;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getStatusValue() {
        return Integer.valueOf(status);
    }
}
