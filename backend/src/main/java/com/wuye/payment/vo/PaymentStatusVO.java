package com.wuye.payment.vo;

import java.time.LocalDateTime;

public class PaymentStatusVO {

    private String payOrderNo;
    private Long billId;
    private String status;
    private LocalDateTime paidAt;
    private Integer rewardIssuedCount;

    public String getPayOrderNo() {
        return payOrderNo;
    }

    public void setPayOrderNo(String payOrderNo) {
        this.payOrderNo = payOrderNo;
    }

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }

    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }

    public Integer getRewardIssuedCount() {
        return rewardIssuedCount;
    }

    public void setRewardIssuedCount(Integer rewardIssuedCount) {
        this.rewardIssuedCount = rewardIssuedCount;
    }
}
