package com.wuye.coupon.vo;

import java.time.LocalDateTime;

public class AdminVoucherExchangeVO {
    private Long exchangeId;
    private Long couponInstanceId;
    private Long templateId;
    private String templateName;
    private Long ownerAccountId;
    private String ownerAccountName;
    private String goodsName;
    private String goodsSpec;
    private String exchangeStatus;
    private String pickupSite;
    private String remark;
    private LocalDateTime createdAt;

    public Long getExchangeId() { return exchangeId; }
    public void setExchangeId(Long exchangeId) { this.exchangeId = exchangeId; }
    public Long getCouponInstanceId() { return couponInstanceId; }
    public void setCouponInstanceId(Long couponInstanceId) { this.couponInstanceId = couponInstanceId; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public Long getOwnerAccountId() { return ownerAccountId; }
    public void setOwnerAccountId(Long ownerAccountId) { this.ownerAccountId = ownerAccountId; }
    public String getOwnerAccountName() { return ownerAccountName; }
    public void setOwnerAccountName(String ownerAccountName) { this.ownerAccountName = ownerAccountName; }
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
}
