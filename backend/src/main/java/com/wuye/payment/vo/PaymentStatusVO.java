package com.wuye.payment.vo;

import java.time.LocalDateTime;

public class PaymentStatusVO {

    private String payOrderNo;
    private Long billId;
    private String status;
    private LocalDateTime paidAt;
    private Boolean annualPayment;
    private Integer coveredBillCount;
    private Integer rewardIssuedCount;
    private Boolean voucherIssued;

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

    public Boolean getAnnualPayment() {
        return annualPayment;
    }

    public void setAnnualPayment(Boolean annualPayment) {
        this.annualPayment = annualPayment;
    }

    public Integer getCoveredBillCount() {
        return coveredBillCount;
    }

    public void setCoveredBillCount(Integer coveredBillCount) {
        this.coveredBillCount = coveredBillCount;
    }

    public Integer getRewardIssuedCount() {
        return rewardIssuedCount;
    }

    public void setRewardIssuedCount(Integer rewardIssuedCount) {
        this.rewardIssuedCount = rewardIssuedCount;
    }

    public Boolean getVoucherIssued() {
        return voucherIssued;
    }

    public void setVoucherIssued(Boolean voucherIssued) {
        this.voucherIssued = voucherIssued;
    }
}
