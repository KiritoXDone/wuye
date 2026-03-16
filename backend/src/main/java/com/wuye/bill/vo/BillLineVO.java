package com.wuye.bill.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.math.BigDecimal;
import java.util.Map;

public class BillLineVO {

    private Integer lineNo;
    private String lineType;
    private String itemName;
    private BigDecimal unitPrice;
    private BigDecimal quantity;
    private BigDecimal lineAmount;
    @JsonIgnore
    private String extJson;
    private Map<String, Object> ext;

    public Integer getLineNo() {
        return lineNo;
    }

    public void setLineNo(Integer lineNo) {
        this.lineNo = lineNo;
    }

    public String getLineType() {
        return lineType;
    }

    public void setLineType(String lineType) {
        this.lineType = lineType;
    }

    public String getItemName() {
        return itemName;
    }

    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getLineAmount() {
        return lineAmount;
    }

    public void setLineAmount(BigDecimal lineAmount) {
        this.lineAmount = lineAmount;
    }

    public String getExtJson() {
        return extJson;
    }

    public void setExtJson(String extJson) {
        this.extJson = extJson;
    }

    public Map<String, Object> getExt() {
        return ext;
    }

    public void setExt(Map<String, Object> ext) {
        this.ext = ext;
    }
}
