package com.wuye.payment.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public class AlipayCallbackDTO {

    @NotBlank(message = "payOrderNo 不能为空")
    private String payOrderNo;
    private String outTradeNo;
    @NotBlank(message = "merchantId 不能为空")
    private String merchantId;
    private BigDecimal totalAmount;
    @NotBlank(message = "sign 不能为空")
    private String sign;

    public String getPayOrderNo() {
        return payOrderNo;
    }

    public void setPayOrderNo(String payOrderNo) {
        this.payOrderNo = payOrderNo;
    }

    public String getOutTradeNo() {
        return outTradeNo;
    }

    public void setOutTradeNo(String outTradeNo) {
        this.outTradeNo = outTradeNo;
    }

    public String getMerchantId() {
        return merchantId;
    }

    public void setMerchantId(String merchantId) {
        this.merchantId = merchantId;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getSign() {
        return sign;
    }

    public void setSign(String sign) {
        this.sign = sign;
    }
}
