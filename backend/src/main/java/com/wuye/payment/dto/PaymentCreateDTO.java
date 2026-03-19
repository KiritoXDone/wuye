package com.wuye.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PaymentCreateDTO {

    @NotNull(message = "billId 不能为空")
    private Long billId;
    @NotBlank(message = "channel 不能为空")
    private String channel;
    private Long couponInstanceId;
    @NotBlank(message = "idempotencyKey 不能为空")
    private String idempotencyKey;
    private Boolean annualPayment;

    public Long getBillId() {
        return billId;
    }

    public void setBillId(Long billId) {
        this.billId = billId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public Long getCouponInstanceId() {
        return couponInstanceId;
    }

    public void setCouponInstanceId(Long couponInstanceId) {
        this.couponInstanceId = couponInstanceId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public Boolean getAnnualPayment() {
        return annualPayment;
    }

    public void setAnnualPayment(Boolean annualPayment) {
        this.annualPayment = annualPayment;
    }
}
