package com.wuye.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.bill.entity.Bill;
import com.wuye.bill.mapper.BillMapper;
import com.wuye.common.exception.BusinessException;
import com.wuye.coupon.service.CouponService;
import com.wuye.payment.dto.AlipayCallbackDTO;
import com.wuye.payment.dto.WechatCallbackDTO;
import com.wuye.payment.entity.PayOrder;
import com.wuye.payment.entity.PayTransaction;
import com.wuye.payment.mapper.PayOrderMapper;
import com.wuye.payment.mapper.PayTransactionMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PaymentCallbackService {

    private final PayOrderMapper payOrderMapper;
    private final PayTransactionMapper payTransactionMapper;
    private final BillMapper billMapper;
    private final ObjectMapper objectMapper;
    private final CouponService couponService;

    public PaymentCallbackService(PayOrderMapper payOrderMapper,
                                  PayTransactionMapper payTransactionMapper,
                                  BillMapper billMapper,
                                  ObjectMapper objectMapper,
                                  CouponService couponService) {
        this.payOrderMapper = payOrderMapper;
        this.payTransactionMapper = payTransactionMapper;
        this.billMapper = billMapper;
        this.objectMapper = objectMapper;
        this.couponService = couponService;
    }

    @Transactional
    public Map<String, Object> handleWechatCallback(WechatCallbackDTO dto) {
        return handleSuccessCallback(dto.getPayOrderNo(), dto.getOutTradeNo(), dto, "WECHAT");
    }

    @Transactional
    public Map<String, Object> handleAlipayCallback(AlipayCallbackDTO dto) {
        return handleSuccessCallback(dto.getPayOrderNo(), dto.getOutTradeNo(), dto, "ALIPAY");
    }

    private Map<String, Object> handleSuccessCallback(String payOrderNo,
                                                      String outTradeNo,
                                                      Object request,
                                                      String channel) {
        PayOrder payOrder = payOrderMapper.findByPayOrderNo(payOrderNo);
        if (payOrder == null) {
            return Map.of("accepted", false, "message", "payOrderNo not found");
        }
        if (!channel.equalsIgnoreCase(payOrder.getChannel())) {
            throw new BusinessException("CONFLICT", "支付渠道与回调渠道不一致", HttpStatus.CONFLICT);
        }
        if ("SUCCESS".equals(payOrder.getStatus())) {
            if (outTradeNo != null
                    && payOrder.getChannelTradeNo() != null
                    && !outTradeNo.equals(payOrder.getChannelTradeNo())) {
                throw new BusinessException("CONFLICT", "重复回调的 outTradeNo 不一致", HttpStatus.CONFLICT);
            }
            insertTransaction(payOrder.getPayOrderNo(), channel + "_CALLBACK", request, Map.of("alreadyProcessed", true), "SUCCESS", null, null);
            return Map.of("accepted", true, "alreadyProcessed", true, "rewardIssuedCount", 0);
        }
        if (!"PAYING".equals(payOrder.getStatus()) && !"CREATED".equals(payOrder.getStatus())) {
            throw new BusinessException("CONFLICT", "当前支付单状态不允许回调入账", HttpStatus.CONFLICT);
        }
        if (outTradeNo == null || outTradeNo.isBlank()) {
            throw new BusinessException("INVALID_ARGUMENT", "outTradeNo 不能为空", HttpStatus.BAD_REQUEST);
        }
        LocalDateTime paidAt = LocalDateTime.now();
        payOrderMapper.updateSuccess(payOrder.getPayOrderNo(), "SUCCESS", outTradeNo, paidAt);
        Bill bill = billMapper.findById(payOrder.getBillId());
        if (bill != null && !"PAID".equals(bill.getStatus())) {
            billMapper.markPaid(bill.getId(), payOrder.getPayAmount(), paidAt);
            bill.setAmountPaid(payOrder.getPayAmount());
        }
        couponService.markCouponUsed(payOrder.getCouponInstanceId(), payOrder.getPayOrderNo(), payOrder.getAccountId());
        int rewardIssuedCount = bill == null ? 0 : couponService.issueRewardCoupons(bill, payOrder.getAccountId(), payOrder.getPayOrderNo());
        insertTransaction(payOrder.getPayOrderNo(), channel + "_CALLBACK", request,
                Map.of("paidAt", paidAt, "status", "SUCCESS", "rewardIssuedCount", rewardIssuedCount), "SUCCESS", null, null);
        return Map.of("accepted", true, "alreadyProcessed", false, "rewardIssuedCount", rewardIssuedCount);
    }

    private void insertTransaction(String payOrderNo,
                                   String tradeType,
                                   Object request,
                                   Object response,
                                   String status,
                                   String errorCode,
                                   String errorMessage) {
        PayTransaction transaction = new PayTransaction();
        transaction.setPayOrderNo(payOrderNo);
        transaction.setTradeType(tradeType);
        transaction.setRequestJson(writeJson(request));
        transaction.setResponseJson(writeJson(response));
        transaction.setTransactionStatus(status);
        transaction.setErrorCode(errorCode);
        transaction.setErrorMessage(errorMessage);
        payTransactionMapper.insert(transaction);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize callback json", ex);
        }
    }
}
