package com.wuye.coupon.dto;

public class AdminCouponInstanceQuery {
    private Long templateId;
    private String templateKeyword;
    private String status;
    private String sourceType;
    private Long ownerAccountId;

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getTemplateKeyword() {
        return templateKeyword;
    }

    public void setTemplateKeyword(String templateKeyword) {
        this.templateKeyword = templateKeyword;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public Long getOwnerAccountId() {
        return ownerAccountId;
    }

    public void setOwnerAccountId(Long ownerAccountId) {
        this.ownerAccountId = ownerAccountId;
    }
}
