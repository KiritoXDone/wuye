package com.wuye.coupon.dto;

import jakarta.validation.constraints.NotNull;

public class CouponValidateDTO {
    @NotNull(message = "billId 不能为空")
    private Long billId;
    @NotNull(message = "couponInstanceId 不能为空")
    private Long couponInstanceId;

    public Long getBillId() { return billId; }
    public void setBillId(Long billId) { this.billId = billId; }
    public Long getCouponInstanceId() { return couponInstanceId; }
    public void setCouponInstanceId(Long couponInstanceId) { this.couponInstanceId = couponInstanceId; }
}
