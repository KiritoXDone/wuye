package com.wuye.payment.dto;

import jakarta.validation.constraints.NotBlank;

public class InvoiceApplicationProcessDTO {

    @NotBlank(message = "status 不能为空")
    private String status;
    private String remark;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
