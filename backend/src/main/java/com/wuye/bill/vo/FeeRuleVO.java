package com.wuye.bill.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FeeRuleVO {

    private Long id;
    private Long communityId;
    private String feeType;
    private String ruleName;
    private BigDecimal unitPrice;
    private String cycleType;
    private String pricingMode;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private String remark;
    private List<FeeRuleWaterTierVO> waterTiers;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
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

    public List<FeeRuleWaterTierVO> getWaterTiers() {
        return waterTiers;
    }

    public void setWaterTiers(List<FeeRuleWaterTierVO> waterTiers) {
        this.waterTiers = waterTiers;
    }
}
