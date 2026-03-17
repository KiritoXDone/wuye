package com.wuye.bill.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FeeRuleCreateDTO {

    @NotNull(message = "communityId 不能为空")
    private Long communityId;
    @NotBlank(message = "feeType 不能为空")
    private String feeType;
    @NotNull(message = "unitPrice 不能为空")
    @DecimalMin(value = "0.00", message = "unitPrice 不能小于 0")
    private BigDecimal unitPrice;
    @NotBlank(message = "cycleType 不能为空")
    private String cycleType;
    private String pricingMode;
    @NotNull(message = "effectiveFrom 不能为空")
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String remark;
    private BigDecimal abnormalAbsThreshold;
    private BigDecimal abnormalMultiplierThreshold;
    private List<FeeRuleWaterTierDTO> waterTiers;

    public Long getCommunityId() {
        return communityId;
    }

    public void setCommunityId(Long communityId) {
        this.communityId = communityId;
    }

    public String getFeeType() {
        return feeType;
    }

    public void setFeeType(String feeType) {
        this.feeType = feeType;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String getCycleType() {
        return cycleType;
    }

    public void setCycleType(String cycleType) {
        this.cycleType = cycleType;
    }

    public String getPricingMode() {
        return pricingMode;
    }

    public void setPricingMode(String pricingMode) {
        this.pricingMode = pricingMode;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public BigDecimal getAbnormalAbsThreshold() {
        return abnormalAbsThreshold;
    }

    public void setAbnormalAbsThreshold(BigDecimal abnormalAbsThreshold) {
        this.abnormalAbsThreshold = abnormalAbsThreshold;
    }

    public BigDecimal getAbnormalMultiplierThreshold() {
        return abnormalMultiplierThreshold;
    }

    public void setAbnormalMultiplierThreshold(BigDecimal abnormalMultiplierThreshold) {
        this.abnormalMultiplierThreshold = abnormalMultiplierThreshold;
    }

    public List<FeeRuleWaterTierDTO> getWaterTiers() {
        return waterTiers;
    }

    public void setWaterTiers(List<FeeRuleWaterTierDTO> waterTiers) {
        this.waterTiers = waterTiers;
    }
}
