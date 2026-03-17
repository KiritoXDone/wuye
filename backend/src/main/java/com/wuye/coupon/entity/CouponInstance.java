package com.wuye.coupon.entity;

import java.time.LocalDateTime;

public class CouponInstance {

    private Long id;
    private Long templateId;
    private Long ownerAccountId;
    private Long ownerGroupId;
    private String sourceType;
    private String sourceRefNo;
    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime expiresAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public Long getOwnerAccountId() { return ownerAccountId; }
    public void setOwnerAccountId(Long ownerAccountId) { this.ownerAccountId = ownerAccountId; }
    public Long getOwnerGroupId() { return ownerGroupId; }
    public void setOwnerGroupId(Long ownerGroupId) { this.ownerGroupId = ownerGroupId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceRefNo() { return sourceRefNo; }
    public void setSourceRefNo(String sourceRefNo) { this.sourceRefNo = sourceRefNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getIssuedAt() { return issuedAt; }
    public void setIssuedAt(LocalDateTime issuedAt) { this.issuedAt = issuedAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
}
