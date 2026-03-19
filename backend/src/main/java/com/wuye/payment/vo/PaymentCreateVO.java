package com.wuye.payment.vo;

import java.math.BigDecimal;
import java.util.Map;

public class PaymentCreateVO {

    private String payOrderNo;
    private BigDecimal originAmount;
    private BigDecimal discountAmount;
    private BigDecimal payAmount;
    private String channel;
    private Boolean annualPayment;
    private Integer coveredBillCount;
    private Map<String, Object> payParams;

    public String getPayOrderNo() {
        return payOrderNo;
    }

    public void setPayOrderNo(String payOrderNo) {
        this.payOrderNo = payOrderNo;
    }

    public BigDecimal getOriginAmount() {
        return originAmount;
    }

    public void setOriginAmount(BigDecimal originAmount) {
        this.originAmount = originAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getPayAmount() {
        return payAmount;
    }

    public void setPayAmount(BigDecimal payAmount) {
        this.payAmount = payAmount;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
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

    public Map<String, Object> getPayParams() {
        return payParams;
    }

    public void setPayParams(Map<String, Object> payParams) {
        this.payParams = payParams;
    }
}
