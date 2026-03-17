package com.wuye.payment.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.bill.entity.Bill;
import com.wuye.bill.mapper.BillMapper;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.payment.entity.PayOrder;
import com.wuye.payment.entity.PaymentVoucher;
import com.wuye.payment.mapper.PayOrderMapper;
import com.wuye.payment.mapper.PaymentVoucherMapper;
import com.wuye.payment.vo.PaymentVoucherVO;
import com.wuye.room.service.RoomBindingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PaymentVoucherService {

    private final PaymentVoucherMapper paymentVoucherMapper;
    private final PayOrderMapper payOrderMapper;
    private final BillMapper billMapper;
    private final RoomBindingService roomBindingService;
    private final AccessGuard accessGuard;
    private final ObjectMapper objectMapper;

    public PaymentVoucherService(PaymentVoucherMapper paymentVoucherMapper,
                                 PayOrderMapper payOrderMapper,
                                 BillMapper billMapper,
                                 RoomBindingService roomBindingService,
                                 AccessGuard accessGuard,
                                 ObjectMapper objectMapper) {
        this.paymentVoucherMapper = paymentVoucherMapper;
        this.payOrderMapper = payOrderMapper;
        this.billMapper = billMapper;
        this.roomBindingService = roomBindingService;
        this.accessGuard = accessGuard;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public PaymentVoucher ensureVoucher(PayOrder payOrder, Bill bill, LocalDateTime issuedAt) {
        PaymentVoucher existed = paymentVoucherMapper.findByPayOrderNo(payOrder.getPayOrderNo());
        if (existed != null) {
            return existed;
        }
        PaymentVoucher voucher = new PaymentVoucher();
        voucher.setPayOrderNo(payOrder.getPayOrderNo());
        voucher.setBillId(payOrder.getBillId());
        voucher.setAccountId(payOrder.getAccountId());
        voucher.setVoucherNo("VCH-" + payOrder.getPayOrderNo());
        voucher.setAmount(payOrder.getPayAmount());
        voucher.setStatus("ISSUED");
        voucher.setIssuedAt(issuedAt);
        voucher.setContentJson(writeJson(buildContent(payOrder, bill, issuedAt)));
        paymentVoucherMapper.insert(voucher);
        return voucher;
    }

    public PaymentVoucherVO getVoucher(LoginUser loginUser, String payOrderNo) {
        PayOrder payOrder = payOrderMapper.findByPayOrderNo(payOrderNo);
        if (payOrder == null) {
            throw new BusinessException("NOT_FOUND", "支付单不存在", HttpStatus.NOT_FOUND);
        }
        Bill bill = billMapper.findById(payOrder.getBillId());
        if (!loginUser.hasRole("ADMIN")) {
            accessGuard.requireRole(loginUser, "RESIDENT");
            accessGuard.requireSelfRoom(loginUser, bill != null && roomBindingService.hasActiveBinding(loginUser.accountId(), bill.getRoomId()));
        }
        PaymentVoucher voucher = paymentVoucherMapper.findByPayOrderNo(payOrderNo);
        if (voucher == null) {
            throw new BusinessException("NOT_FOUND", "电子凭证不存在", HttpStatus.NOT_FOUND);
        }
        PaymentVoucherVO vo = new PaymentVoucherVO();
        vo.setPayOrderNo(voucher.getPayOrderNo());
        vo.setBillId(voucher.getBillId());
        vo.setVoucherNo(voucher.getVoucherNo());
        vo.setAmount(voucher.getAmount());
        vo.setStatus(voucher.getStatus());
        vo.setIssuedAt(voucher.getIssuedAt());
        vo.setContent(parseContent(voucher.getContentJson()));
        return vo;
    }

    public boolean hasVoucher(String payOrderNo) {
        return paymentVoucherMapper.findByPayOrderNo(payOrderNo) != null;
    }

    private Map<String, Object> buildContent(PayOrder payOrder, Bill bill, LocalDateTime issuedAt) {
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("billId", payOrder.getBillId());
        content.put("billNo", bill == null ? null : bill.getBillNo());
        content.put("feeType", bill == null ? null : bill.getFeeType());
        content.put("amount", payOrder.getPayAmount());
        content.put("issuedAt", issuedAt);
        return content;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to serialize voucher json", ex);
        }
    }

    private Map<String, Object> parseContent(String contentJson) {
        try {
            return objectMapper.readValue(contentJson, new TypeReference<>() {
            });
        } catch (IOException ex) {
            throw new IllegalStateException("failed to parse voucher json", ex);
        }
    }
}
