package com.wuye.coupon.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AvailableCouponVO {
    private Long couponInstanceId;
    private String templateCode;
    private String name;
    private BigDecimal discountAmount;
    private LocalDateTime expiresAt;

    public Long getCouponInstanceId() { return couponInstanceId; }
    public void setCouponInstanceId(Long couponInstanceId) { this.couponInstanceId = couponInstanceId; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
