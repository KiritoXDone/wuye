package com.wuye.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.bill.entity.Bill;
import com.wuye.bill.mapper.BillMapper;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.infra.mq.PaymentEventPublisher;
import com.wuye.common.infra.redis.RedisCallbackLock;
import com.wuye.coupon.service.CouponService;
import com.wuye.payment.event.PaymentSuccessEvent;
import com.wuye.payment.dto.AlipayCallbackDTO;
import com.wuye.payment.dto.WechatCallbackDTO;
import com.wuye.payment.entity.PayOrder;
import com.wuye.payment.entity.PayTransaction;
import com.wuye.payment.mapper.PayOrderMapper;
import com.wuye.payment.mapper.PayTransactionMapper;
import com.wuye.payment.util.PaymentSignUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class PaymentCallbackService {

    private static final Logger log = LoggerFactory.getLogger(PaymentCallbackService.class);

    private final PayOrderMapper payOrderMapper;
    private final PayTransactionMapper payTransactionMapper;
    private final BillMapper billMapper;
    private final ObjectMapper objectMapper;
    private final CouponService couponService;
    private final PaymentVoucherService paymentVoucherService;
    private final RedisCallbackLock redisCallbackLock;
    private final PaymentEventPublisher paymentEventPublisher;
    private final String wechatMerchantId;
    private final String wechatCallbackSecret;
    private final String alipayMerchantId;
    private final String alipayCallbackSecret;

    public PaymentCallbackService(PayOrderMapper payOrderMapper,
                                  PayTransactionMapper payTransactionMapper,
                                  BillMapper billMapper,
                                  ObjectMapper objectMapper,
                                  CouponService couponService,
                                  PaymentVoucherService paymentVoucherService,
                                  RedisCallbackLock redisCallbackLock,
                                  PaymentEventPublisher paymentEventPublisher,
                                  @Value("${app.payment.wechat.merchant-id}") String wechatMerchantId,
                                  @Value("${app.payment.wechat.callback-secret}") String wechatCallbackSecret,
                                  @Value("${app.payment.alipay.merchant-id}") String alipayMerchantId,
                                  @Value("${app.payment.alipay.callback-secret}") String alipayCallbackSecret) {
        this.payOrderMapper = payOrderMapper;
        this.payTransactionMapper = payTransactionMapper;
        this.billMapper = billMapper;
        this.objectMapper = objectMapper;
        this.couponService = couponService;
        this.paymentVoucherService = paymentVoucherService;
        this.redisCallbackLock = redisCallbackLock;
        this.paymentEventPublisher = paymentEventPublisher;
        this.wechatMerchantId = wechatMerchantId;
        this.wechatCallbackSecret = wechatCallbackSecret;
        this.alipayMerchantId = alipayMerchantId;
        this.alipayCallbackSecret = alipayCallbackSecret;
    }

    @Transactional
    public Map<String, Object> handleWechatCallback(WechatCallbackDTO dto) {
        return handleSuccessCallback(dto.getPayOrderNo(), dto.getOutTradeNo(), dto.getMerchantId(), dto.getTotalAmount(), dto.getSign(), dto, "WECHAT");
    }

    @Transactional
    public Map<String, Object> handleAlipayCallback(AlipayCallbackDTO dto) {
        return handleSuccessCallback(dto.getPayOrderNo(), dto.getOutTradeNo(), dto.getMerchantId(), dto.getTotalAmount(), dto.getSign(), dto, "ALIPAY");
    }

    private Map<String, Object> handleSuccessCallback(String payOrderNo,
                                                      String outTradeNo,
                                                      String merchantId,
                                                      BigDecimal totalAmount,
                                                      String sign,
                                                      Object request,
                                                      String channel) {
        String callbackLockKey = channel.toLowerCase() + ":callback:" + payOrderNo;
        if (!redisCallbackLock.acquire(callbackLockKey)) {
            throw new BusinessException("CONFLICT", "回调正在处理中，请稍后重试", HttpStatus.CONFLICT);
        }
        try {
            return handleSuccessCallbackInternal(payOrderNo, outTradeNo, merchantId, totalAmount, sign, request, channel);
        } finally {
            try {
                redisCallbackLock.release(callbackLockKey);
            } catch (RuntimeException ex) {
                log.warn("callback lock release degraded for {}", callbackLockKey, ex);
            }
        }
    }

    private Map<String, Object> handleSuccessCallbackInternal(String payOrderNo,
                                                              String outTradeNo,
                                                              String merchantId,
                                                              BigDecimal totalAmount,
                                                              String sign,
                                                              Object request,
                                                              String channel) {
        PayOrder payOrder = payOrderMapper.findByPayOrderNo(payOrderNo);
        if (payOrder == null) {
            return Map.of("accepted", false, "message", "payOrderNo not found");
        }
        if (!channel.equalsIgnoreCase(payOrder.getChannel())) {
            throw new BusinessException("CONFLICT", "支付渠道与回调渠道不一致", HttpStatus.CONFLICT);
        }
        validateCallbackSecurity(payOrder, outTradeNo, merchantId, totalAmount, sign, channel);
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
        paymentVoucherService.ensureVoucher(payOrder, bill, paidAt);
        couponService.markCouponUsed(payOrder.getCouponInstanceId(), payOrder.getPayOrderNo(), payOrder.getAccountId());
        int rewardIssuedCount = bill == null ? 0 : couponService.issueRewardCoupons(bill, payOrder.getAccountId(), payOrder.getPayOrderNo());
        PaymentSuccessEvent event = new PaymentSuccessEvent();
        event.setPayOrderNo(payOrder.getPayOrderNo());
        event.setBillId(payOrder.getBillId());
        event.setAccountId(payOrder.getAccountId());
        event.setChannel(payOrder.getChannel());
        event.setPayAmount(payOrder.getPayAmount());
        event.setPaidAt(paidAt);
        paymentEventPublisher.publishPaymentSuccess(event);
        insertTransaction(payOrder.getPayOrderNo(), channel + "_CALLBACK", request,
                Map.of("paidAt", paidAt, "status", "SUCCESS", "rewardIssuedCount", rewardIssuedCount), "SUCCESS", null, null);
        return Map.of("accepted", true, "alreadyProcessed", false, "rewardIssuedCount", rewardIssuedCount);
    }

    private void validateCallbackSecurity(PayOrder payOrder,
                                          String outTradeNo,
                                          String merchantId,
                                          BigDecimal totalAmount,
                                          String sign,
                                          String channel) {
        String expectedMerchantId = "WECHAT".equals(channel) ? wechatMerchantId : alipayMerchantId;
        String expectedSecret = "WECHAT".equals(channel) ? wechatCallbackSecret : alipayCallbackSecret;
        if (merchantId == null || merchantId.isBlank()) {
            throw new BusinessException("INVALID_ARGUMENT", "merchantId 不能为空", HttpStatus.BAD_REQUEST);
        }
        if (!expectedMerchantId.equals(merchantId)) {
            throw new BusinessException("CONFLICT", "merchantId 校验失败", HttpStatus.CONFLICT);
        }
        if (totalAmount == null) {
            throw new BusinessException("INVALID_ARGUMENT", "totalAmount 不能为空", HttpStatus.BAD_REQUEST);
        }
        if (payOrder.getPayAmount().compareTo(totalAmount) != 0) {
            throw new BusinessException("CONFLICT", "totalAmount 校验失败", HttpStatus.CONFLICT);
        }
        String expectedSign = PaymentSignUtils.sign(payOrder.getPayOrderNo(), outTradeNo, merchantId, totalAmount, expectedSecret);
        if (!expectedSign.equals(sign)) {
            throw new BusinessException("CONFLICT", "sign 校验失败", HttpStatus.CONFLICT);
        }
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
