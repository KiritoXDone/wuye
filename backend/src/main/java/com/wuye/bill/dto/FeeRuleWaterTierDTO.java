package com.wuye.bill.dto;

import java.math.BigDecimal;

public class FeeRuleWaterTierDTO {

    private BigDecimal startUsage;
    private BigDecimal endUsage;
    private BigDecimal unitPrice;

    public BigDecimal getStartUsage() {
        return startUsage;
    }

    public void setStartUsage(BigDecimal startUsage) {
        this.startUsage = startUsage;
    }

    public BigDecimal getEndUsage() {
        return endUsage;
    }

    public void setEndUsage(BigDecimal endUsage) {
        this.endUsage = endUsage;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }
}
