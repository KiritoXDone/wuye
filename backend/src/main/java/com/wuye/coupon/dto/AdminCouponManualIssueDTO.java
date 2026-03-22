package com.wuye.coupon.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class AdminCouponManualIssueDTO {
    @NotNull(message = "templateId 不能为空")
    private Long templateId;
    @NotNull(message = "ownerAccountId 不能为空")
    private Long ownerAccountId;
    @NotNull(message = "issueCount 不能为空")
    @Min(value = 1, message = "issueCount 至少为 1")
    private Integer issueCount;
    private String remark;

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

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
