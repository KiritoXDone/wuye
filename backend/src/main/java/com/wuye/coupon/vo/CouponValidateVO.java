package com.wuye.coupon.vo;

import java.math.BigDecimal;

public class CouponValidateVO {
    private Long couponInstanceId;
    private boolean valid;
    private BigDecimal discountAmount;
    private String message;

    public Long getCouponInstanceId() { return couponInstanceId; }
    public void setCouponInstanceId(Long couponInstanceId) { this.couponInstanceId = couponInstanceId; }
    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
