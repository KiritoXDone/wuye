package com.wuye.coupon.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class CouponRuleCreateDTO {
    @NotBlank(message = "name 不能为空")
    private String name;
    @NotBlank(message = "feeType 不能为空")
    private String feeType;
    @NotBlank(message = "templateCode 不能为空")
    private String templateCode;
    @NotNull(message = "minPayAmount 不能为空")
    private BigDecimal minPayAmount;
    @NotNull(message = "rewardCount 不能为空")
    private Integer rewardCount;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getFeeType() { return feeType; }
    public void setFeeType(String feeType) { this.feeType = feeType; }
    public String getTemplateCode() { return templateCode; }
    public void setTemplateCode(String templateCode) { this.templateCode = templateCode; }
    public BigDecimal getMinPayAmount() { return minPayAmount; }
    public void setMinPayAmount(BigDecimal minPayAmount) { this.minPayAmount = minPayAmount; }
    public Integer getRewardCount() { return rewardCount; }
    public void setRewardCount(Integer rewardCount) { this.rewardCount = rewardCount; }
}
