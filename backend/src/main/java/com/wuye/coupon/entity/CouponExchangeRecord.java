package com.wuye.coupon.entity;

import java.time.LocalDateTime;

public class CouponExchangeRecord {
    private Long id;
    private Long couponInstanceId;
    private Long templateId;
    private Long ownerAccountId;
    private String goodsName;
    private String goodsSpec;
    private String exchangeStatus;
    private String pickupSite;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCouponInstanceId() { return couponInstanceId; }
    public void setCouponInstanceId(Long couponInstanceId) { this.couponInstanceId = couponInstanceId; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public Long getOwnerAccountId() { return ownerAccountId; }
    public void setOwnerAccountId(Long ownerAccountId) { this.ownerAccountId = ownerAccountId; }
    public String getGoodsName() { return goodsName; }
    public void setGoodsName(String goodsName) { this.goodsName = goodsName; }
    public String getGoodsSpec() { return goodsSpec; }
    public void setGoodsSpec(String goodsSpec) { this.goodsSpec = goodsSpec; }
    public String getExchangeStatus() { return exchangeStatus; }
    public void setExchangeStatus(String exchangeStatus) { this.exchangeStatus = exchangeStatus; }
    public String getPickupSite() { return pickupSite; }
    public void setPickupSite(String pickupSite) { this.pickupSite = pickupSite; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
