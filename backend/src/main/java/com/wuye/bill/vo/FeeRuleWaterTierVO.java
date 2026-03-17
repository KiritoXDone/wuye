package com.wuye.bill.vo;

import java.math.BigDecimal;

public class FeeRuleWaterTierVO {

    private Integer tierOrder;
    private BigDecimal startUsage;
    private BigDecimal endUsage;
    private BigDecimal unitPrice;

    public Integer getTierOrder() {
        return tierOrder;
    }

    public void setTierOrder(Integer tierOrder) {
        this.tierOrder = tierOrder;
    }

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
