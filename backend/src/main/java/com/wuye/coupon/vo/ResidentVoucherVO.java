package com.wuye.coupon.vo;

import java.time.LocalDateTime;

public class ResidentVoucherVO {
    private Long couponInstanceId;
    private Long templateId;
    private String templateCode;
    private String name;
    private String goodsName;
    private String goodsSpec;
    private String redeemInstruction;
    private String status;
    private String exchangeStatus;
    private String pickupSite;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;

    public Long getCouponInstanceId() { return couponInstanceId; }
    public void setCouponInstanceId(Long couponInstanceId) { this.couponInstanceId = couponInstanceId; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getGoodsName() { return goodsName; }
    public void setGoodsName(String goodsName) { this.goodsName = goodsName; }
    public String getGoodsSpec() { return goodsSpec; }
    public void setGoodsSpec(String goodsSpec) { this.goodsSpec = goodsSpec; }
    public String getRedeemInstruction() { return redeemInstruction; }
    public void setRedeemInstruction(String redeemInstruction) { this.redeemInstruction = redeemInstruction; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getExchangeStatus() { return exchangeStatus; }
    public void setExchangeStatus(String exchangeStatus) { this.exchangeStatus = exchangeStatus; }
    public String getPickupSite() { return pickupSite; }
    public void setPickupSite(String pickupSite) { this.pickupSite = pickupSite; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
