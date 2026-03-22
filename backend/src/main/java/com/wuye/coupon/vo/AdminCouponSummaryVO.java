package com.wuye.coupon.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminCouponSummaryVO {
    private Long id;
    private String templateCode;
    private String type;
    private String feeType;
    private String name;
    private String discountMode;
    private BigDecimal valueAmount;
    private BigDecimal thresholdAmount;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer status;
    private Long ruleId;
    private String ruleName;
    private String triggerType;
    private BigDecimal minPayAmount;
    private Integer rewardCount;
    private Integer ruleStatus;
    private Integer issuedCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getFeeType() { return feeType; }
    public void setFeeType(String feeType) { this.feeType = feeType; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDiscountMode() { return discountMode; }
    public void setDiscountMode(String discountMode) { this.discountMode = discountMode; }
    public BigDecimal getValueAmount() { return valueAmount; }
    public void setValueAmount(BigDecimal valueAmount) { this.valueAmount = valueAmount; }
    public BigDecimal getThresholdAmount() { return thresholdAmount; }
    public void setThresholdAmount(BigDecimal thresholdAmount) { this.thresholdAmount = thresholdAmount; }
    public LocalDateTime getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDateTime validFrom) { this.validFrom = validFrom; }
    public LocalDateTime getValidTo() { return validTo; }
    public void setValidTo(LocalDateTime validTo) { this.validTo = validTo; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public BigDecimal getMinPayAmount() { return minPayAmount; }
    public void setMinPayAmount(BigDecimal minPayAmount) { this.minPayAmount = minPayAmount; }
    public Integer getRewardCount() { return rewardCount; }
    public void setRewardCount(Integer rewardCount) { this.rewardCount = rewardCount; }
    public Integer getRuleStatus() { return ruleStatus; }
    public void setRuleStatus(Integer ruleStatus) { this.ruleStatus = ruleStatus; }
    public Integer getIssuedCount() { return issuedCount; }
    public void setIssuedCount(Integer issuedCount) { this.issuedCount = issuedCount; }
}
