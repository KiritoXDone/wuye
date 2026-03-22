package com.wuye.coupon.vo;

import java.util.List;

public class AdminCouponManualIssueResultVO {
    private Long templateId;
    private Long ownerAccountId;
    private Integer issueCount;
    private List<Long> couponInstanceIds;

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public Long getOwnerAccountId() {
        return ownerAccountId;
    }

    public void setOwnerAccountId(Long ownerAccountId) {
        this.ownerAccountId = ownerAccountId;
    }

    public Integer getIssueCount() {
        return issueCount;
    }

    public void setIssueCount(Integer issueCount) {
        this.issueCount = issueCount;
    }

    public List<Long> getCouponInstanceIds() {
        return couponInstanceIds;
    }

    public void setCouponInstanceIds(List<Long> couponInstanceIds) {
        this.couponInstanceIds = couponInstanceIds;
    }
}
