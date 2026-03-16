package com.wuye.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.bill.entity.Bill;
import com.wuye.bill.mapper.BillMapper;
import com.wuye.common.exception.BusinessException;
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

    public PaymentCallbackService(PayOrderMapper payOrderMapper,
                                  PayTransactionMapper payTransactionMapper,
                                  BillMapper billMapper,
                                  ObjectMapper objectMapper) {
        this.payOrderMapper = payOrderMapper;
        this.payTransactionMapper = payTransactionMapper;
        this.billMapper = billMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> handleWechatCallback(WechatCallbackDTO dto) {
        PayOrder payOrder = payOrderMapper.findByPayOrderNo(dto.getPayOrderNo());
        if (payOrder == null) {
            return Map.of("accepted", false, "message", "payOrderNo not found");
        }
        if ("SUCCESS".equals(payOrder.getStatus())) {
            if (dto.getOutTradeNo() != null
                    && payOrder.getChannelTradeNo() != null
                    && !dto.getOutTradeNo().equals(payOrder.getChannelTradeNo())) {
                throw new BusinessException("CONFLICT", "重复回调的 outTradeNo 不一致", HttpStatus.CONFLICT);
            }
            insertTransaction(payOrder.getPayOrderNo(), "CALLBACK", dto, Map.of("alreadyProcessed", true), "SUCCESS", null, null);
            return Map.of("accepted", true, "alreadyProcessed", true);
        }
        if (!"PAYING".equals(payOrder.getStatus()) && !"CREATED".equals(payOrder.getStatus())) {
            throw new BusinessException("CONFLICT", "当前支付单状态不允许回调入账", HttpStatus.CONFLICT);
        }
        if (dto.getOutTradeNo() == null || dto.getOutTradeNo().isBlank()) {
            throw new BusinessException("INVALID_ARGUMENT", "outTradeNo 不能为空", HttpStatus.BAD_REQUEST);
        }
        LocalDateTime paidAt = LocalDateTime.now();
        payOrderMapper.updateSuccess(payOrder.getPayOrderNo(), "SUCCESS", dto.getOutTradeNo(), paidAt);
        Bill bill = billMapper.findById(payOrder.getBillId());
        if (bill != null && !"PAID".equals(bill.getStatus())) {
            billMapper.markPaid(bill.getId(), payOrder.getPayAmount(), paidAt);
        }
        insertTransaction(payOrder.getPayOrderNo(), "CALLBACK", dto, Map.of("paidAt", paidAt, "status", "SUCCESS"), "SUCCESS", null, null);
        return Map.of("accepted", true, "alreadyProcessed", false);
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
