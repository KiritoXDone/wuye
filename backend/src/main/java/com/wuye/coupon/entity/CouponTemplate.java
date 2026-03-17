package com.wuye.coupon.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CouponTemplate {

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
    private Integer stackable;
    private Integer status;

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
    public Integer getStackable() { return stackable; }
    public void setStackable(Integer stackable) { this.stackable = stackable; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
