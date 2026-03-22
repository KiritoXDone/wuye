package com.wuye.coupon.entity;

import java.math.BigDecimal;

public class CouponIssueRule {

    private Long id;
    private String ruleName;
    private String triggerType;
    private String feeType;
    private Long templateId;
    private BigDecimal minPayAmount;
    private Integer rewardCount;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public String getFeeType() { return feeType; }
    public void setFeeType(String feeType) { this.feeType = feeType; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public BigDecimal getMinPayAmount() { return minPayAmount; }
    public void setMinPayAmount(BigDecimal minPayAmount) { this.minPayAmount = minPayAmount; }
    public Integer getRewardCount() { return rewardCount; }
    public void setRewardCount(Integer rewardCount) { this.rewardCount = rewardCount; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
