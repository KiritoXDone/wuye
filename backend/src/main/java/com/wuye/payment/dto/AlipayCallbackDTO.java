package com.wuye.payment.dto;

import jakarta.validation.constraints.NotBlank;

public class AlipayCallbackDTO {

    @NotBlank(message = "payOrderNo 不能为空")
    private String payOrderNo;
    private String outTradeNo;

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
}
