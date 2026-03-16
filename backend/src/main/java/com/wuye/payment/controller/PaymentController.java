package com.wuye.payment.controller;

import com.wuye.common.api.ApiResponse;
import com.wuye.common.security.CurrentUser;
import com.wuye.common.security.LoginUser;
import com.wuye.payment.dto.PaymentCreateDTO;
import com.wuye.payment.service.PaymentService;
import com.wuye.payment.vo.PaymentCreateVO;
import com.wuye.payment.vo.PaymentStatusVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public ApiResponse<PaymentCreateVO> create(@CurrentUser LoginUser loginUser,
                                               @Valid @RequestBody PaymentCreateDTO dto) {
        return ApiResponse.success(paymentService.create(loginUser, dto));
    }

    @GetMapping("/{payOrderNo}")
    public ApiResponse<PaymentStatusVO> query(@CurrentUser LoginUser loginUser, @PathVariable String payOrderNo) {
        return ApiResponse.success(paymentService.query(loginUser, payOrderNo));
    }
}
