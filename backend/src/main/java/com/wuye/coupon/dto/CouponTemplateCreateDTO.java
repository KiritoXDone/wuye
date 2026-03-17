package com.wuye.coupon.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CouponTemplateCreateDTO {
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
}
