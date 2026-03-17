package com.wuye.payment.controller;

import com.wuye.common.api.ApiResponse;
import com.wuye.payment.dto.AlipayCallbackDTO;
import com.wuye.payment.dto.WechatCallbackDTO;
import com.wuye.payment.service.PaymentCallbackService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/callbacks")
public class PaymentCallbackController {

    private final PaymentCallbackService paymentCallbackService;

    public PaymentCallbackController(PaymentCallbackService paymentCallbackService) {
        this.paymentCallbackService = paymentCallbackService;
    }

    @PostMapping("/wechatpay")
    public ApiResponse<Map<String, Object>> wechatCallback(@Valid @RequestBody WechatCallbackDTO dto) {
        return ApiResponse.success(paymentCallbackService.handleWechatCallback(dto));
    }

    @PostMapping("/alipay")
    public ApiResponse<Map<String, Object>> alipayCallback(@Valid @RequestBody AlipayCallbackDTO dto) {
        return ApiResponse.success(paymentCallbackService.handleAlipayCallback(dto));
    }
}
