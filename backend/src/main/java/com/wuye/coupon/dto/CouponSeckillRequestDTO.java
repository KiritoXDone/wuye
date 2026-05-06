package com.wuye.coupon.dto;

import jakarta.validation.constraints.NotBlank;

public class CouponSeckillRequestDTO {

    @NotBlank(message = "requestId 不能为空")
    private String requestId;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
}
