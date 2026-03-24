package com.wuye.bill.dto;

import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class AdminBillMarkPaidDTO {

    private LocalDateTime paidAt;

    @Size(max = 200)
    private String remark;

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
