package com.wuye.coupon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AdminCouponUpsertDTO {
    private Long id;
    @NotBlank(message = "templateCode 不能为空")
    private String templateCode;
    @NotBlank(message = "type 不能为空")
    private String type;
    private String feeType;
    @NotBlank(message = "name 不能为空")
    private String name;
    @NotBlank(message = "discountMode 不能为空")
    private String discountMode;
    @NotNull(message = "valueAmount 不能为空")
    private BigDecimal valueAmount;
    @NotNull(message = "thresholdAmount 不能为空")
    private BigDecimal thresholdAmount;
    @NotNull(message = "validFrom 不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validFrom;
    @NotNull(message = "validTo 不能为空")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime validTo;
    private Integer status;
    private String triggerType;
    @Min(value = 0, message = "minPayAmount 不能小于 0")
    private BigDecimal minPayAmount;
    @Min(value = 1, message = "rewardCount 至少为 1")
    private Integer rewardCount;
    private Integer ruleStatus;

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
    public String getTriggerType() { return triggerType; }
    public void setTriggerType(String triggerType) { this.triggerType = triggerType; }
    public BigDecimal getMinPayAmount() { return minPayAmount; }
    public void setMinPayAmount(BigDecimal minPayAmount) { this.minPayAmount = minPayAmount; }
    public Integer getRewardCount() { return rewardCount; }
    public void setRewardCount(Integer rewardCount) { this.rewardCount = rewardCount; }
    public Integer getRuleStatus() { return ruleStatus; }
    public void setRuleStatus(Integer ruleStatus) { this.ruleStatus = ruleStatus; }
}
