package com.wuye.coupon.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminVoucherExchangeStatusDTO {
    @NotBlank(message = "exchangeStatus 不能为空")
    private String exchangeStatus;
    private String remark;

    public String getExchangeStatus() {
        return exchangeStatus;
    }

    public void setExchangeStatus(String exchangeStatus) {
        this.exchangeStatus = exchangeStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
