package com.wuye.payment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.bill.entity.Bill;
import com.wuye.bill.mapper.BillMapper;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.common.util.NoGenerator;
import com.wuye.coupon.service.CouponService;
import com.wuye.payment.dto.PaymentCreateDTO;
import com.wuye.payment.entity.PayOrder;
import com.wuye.payment.entity.PayOrderBillCover;
import com.wuye.payment.entity.PayTransaction;
import com.wuye.payment.mapper.PayOrderBillCoverMapper;
import com.wuye.payment.mapper.PayOrderMapper;
import com.wuye.payment.mapper.PayTransactionMapper;
import com.wuye.payment.vo.PaymentCreateVO;
import com.wuye.payment.vo.PaymentStatusVO;
import com.wuye.room.service.RoomBindingService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class PaymentService {

    private final BillMapper billMapper;
    private final PayOrderMapper payOrderMapper;
    private final PayOrderBillCoverMapper payOrderBillCoverMapper;
    private final PayTransactionMapper payTransactionMapper;
    private final RoomBindingService roomBindingService;
    private final AccessGuard accessGuard;
    private final ObjectMapper objectMapper;
    private final CouponService couponService;
    private final PaymentVoucherService paymentVoucherService;

    public PaymentService(BillMapper billMapper,
                          PayOrderMapper payOrderMapper,
                          PayOrderBillCoverMapper payOrderBillCoverMapper,
                          PayTransactionMapper payTransactionMapper,
                          RoomBindingService roomBindingService,
                          AccessGuard accessGuard,
                          ObjectMapper objectMapper,
                          CouponService couponService,
                          PaymentVoucherService paymentVoucherService) {
        this.billMapper = billMapper;
        this.payOrderMapper = payOrderMapper;
        this.payOrderBillCoverMapper = payOrderBillCoverMapper;
        this.payTransactionMapper = payTransactionMapper;
        this.roomBindingService = roomBindingService;
        this.accessGuard = accessGuard;
        this.objectMapper = objectMapper;
        this.couponService = couponService;
        this.paymentVoucherService = paymentVoucherService;
    }

    @Transactional
    public PaymentCreateVO create(LoginUser loginUser, PaymentCreateDTO dto) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        if (!"WECHAT".equalsIgnoreCase(dto.getChannel()) && !"ALIPAY".equalsIgnoreCase(dto.getChannel())) {
            throw new BusinessException("INVALID_ARGUMENT", "当前仅支持 WECHAT 或 ALIPAY 渠道", HttpStatus.BAD_REQUEST);
        }
        Bill bill = billMapper.findById(dto.getBillId());
        if (bill == null) {
            throw new BusinessException("NOT_FOUND", "账单不存在", HttpStatus.NOT_FOUND);
        }
        accessGuard.requireSelfRoom(loginUser, roomBindingService.hasActiveBinding(loginUser.accountId(), bill.getRoomId()));
        if ("PAID".equals(bill.getStatus())) {
            throw new BusinessException("CONFLICT", "账单已支付，禁止再次创建支付单", HttpStatus.CONFLICT);
        }
        boolean annualPayment = Boolean.TRUE.equals(dto.getAnnualPayment());
        PayOrder existed = payOrderMapper.findByIdempotencyKey(dto.getIdempotencyKey());
        if (existed != null) {
            if (!existed.getAccountId().equals(loginUser.accountId())
                    || !existed.getBillId().equals(dto.getBillId())
                    || !existed.getChannel().equalsIgnoreCase(dto.getChannel())
                    || !Objects.equals(existed.getCouponInstanceId(), dto.getCouponInstanceId())
                    || !Objects.equals(Boolean.TRUE.equals(existed.getAnnualPayment()), annualPayment)) {
                throw new BusinessException("CONFLICT", "idempotencyKey 已被其他支付请求占用", HttpStatus.CONFLICT);
            }
            return toCreateVO(existed);
        }

        List<Bill> coveredBills = annualPayment ? resolveAnnualPropertyBills(bill, dto) : List.of(bill);

        PayOrder payOrder = new PayOrder();
        payOrder.setPayOrderNo(NoGenerator.payOrderNo());
        payOrder.setBillId(bill.getId());
        payOrder.setAccountId(loginUser.accountId());
        payOrder.setChannel(dto.getChannel().toUpperCase());
        payOrder.setAnnualPayment(annualPayment);
        payOrder.setCoveredBillCount(coveredBills.size());
        payOrder.setOriginAmount(sumOriginAmount(coveredBills));
        BigDecimal discountAmount = annualPayment
                ? BigDecimal.ZERO.setScale(2)
                : couponService.lockCoupon(loginUser.accountId(), bill, dto.getCouponInstanceId());
        payOrder.setDiscountAmount(discountAmount);
        payOrder.setCouponInstanceId(dto.getCouponInstanceId());
        payOrder.setPayAmount(payOrder.getOriginAmount().subtract(discountAmount));
        payOrder.setIdempotencyKey(dto.getIdempotencyKey());
        payOrder.setStatus("PAYING");
        payOrder.setExpiredAt(LocalDateTime.now().plusMinutes(30));
        payOrderMapper.insert(payOrder);
        if (!annualPayment) {
            billMapper.updateDiscountAmount(bill.getId(), discountAmount);
        }
        for (Bill coveredBill : coveredBills) {
            PayOrderBillCover cover = new PayOrderBillCover();
            cover.setPayOrderNo(payOrder.getPayOrderNo());
            cover.setBillId(coveredBill.getId());
            cover.setRoomId(coveredBill.getRoomId());
            cover.setPeriodYear(coveredBill.getPeriodYear());
            cover.setPeriodMonth(coveredBill.getPeriodMonth());
            payOrderBillCoverMapper.insert(cover);
        }

        PayTransaction transaction = new PayTransaction();
        transaction.setPayOrderNo(payOrder.getPayOrderNo());
        transaction.setTradeType("UNIFIED_ORDER");
        transaction.setRequestJson(writeJson(dto));
        transaction.setResponseJson(writeJson(buildPayParams(payOrder)));
        transaction.setTransactionStatus("SUCCESS");
        payTransactionMapper.insert(transaction);
        return toCreateVO(payOrder);
    }

    public PaymentStatusVO query(LoginUser loginUser, String payOrderNo) {
        PayOrder payOrder = payOrderMapper.findByPayOrderNo(payOrderNo);
        if (payOrder == null) {
            throw new BusinessException("NOT_FOUND", "支付单不存在", HttpStatus.NOT_FOUND);
        }
        if (!loginUser.hasRole("ADMIN")) {
            accessGuard.requireRole(loginUser, "RESIDENT");
            Bill bill = billMapper.findById(payOrder.getBillId());
            accessGuard.requireSelfRoom(loginUser, bill != null && roomBindingService.hasActiveBinding(loginUser.accountId(), bill.getRoomId()));
        }
        PaymentStatusVO status = payOrderMapper.findStatus(payOrderNo);
        status.setRewardIssuedCount(couponService.countRewardIssuedByPayOrderNo(payOrderNo));
        status.setVoucherIssued(paymentVoucherService.hasVoucher(payOrderNo));
        return status;
    }

    private PaymentCreateVO toCreateVO(PayOrder payOrder) {
        PaymentCreateVO vo = new PaymentCreateVO();
        vo.setPayOrderNo(payOrder.getPayOrderNo());
        vo.setOriginAmount(payOrder.getOriginAmount());
        vo.setDiscountAmount(payOrder.getDiscountAmount());
        vo.setPayAmount(payOrder.getPayAmount());
        vo.setChannel(payOrder.getChannel());
        vo.setAnnualPayment(Boolean.TRUE.equals(payOrder.getAnnualPayment()));
        vo.setCoveredBillCount(payOrder.getCoveredBillCount());
        vo.setPayParams(buildPayParams(payOrder));
        return vo;
    }

    private List<Bill> resolveAnnualPropertyBills(Bill anchorBill, PaymentCreateDTO dto) {
        if (!"PROPERTY".equals(anchorBill.getFeeType())) {
            throw new BusinessException("INVALID_ARGUMENT", "按年缴费仅支持物业费账单", HttpStatus.BAD_REQUEST);
        }
        if (dto.getCouponInstanceId() != null) {
            throw new BusinessException("INVALID_ARGUMENT", "按年缴费暂不支持优惠券", HttpStatus.BAD_REQUEST);
        }
        if (!"YEAR".equals(anchorBill.getCycleType()) || anchorBill.getPeriodMonth() != null) {
            throw new BusinessException("CONFLICT", "当前物业费账单仍是旧月度口径，暂不支持按新年度口径缴纳", HttpStatus.CONFLICT);
        }
        if (!"ISSUED".equals(anchorBill.getStatus())) {
            throw new BusinessException("CONFLICT", "当前房间该年度物业费账单存在非待缴状态，暂不支持按年缴纳", HttpStatus.CONFLICT);
        }
        Bill yearlyBill = billMapper.findByUniqueKey(anchorBill.getRoomId(), "PROPERTY", anchorBill.getPeriodYear(), null);
        if (yearlyBill == null) {
            throw new BusinessException("NOT_FOUND", "当前房间该年度物业费账单不存在", HttpStatus.NOT_FOUND);
        }
        if (!"ISSUED".equals(yearlyBill.getStatus())) {
            throw new BusinessException("CONFLICT", "当前房间该年度物业费账单存在非待缴状态，暂不支持按年缴纳", HttpStatus.CONFLICT);
        }
        return List.of(yearlyBill);
    }

    private BigDecimal sumOriginAmount(List<Bill> bills) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Bill current : bills) {
            total = total.add(current.getAmountDue());
        }
        return total;
    }

    private Map<String, Object> buildPayParams(PayOrder payOrder) {
        Map<String, Object> payParams = new LinkedHashMap<>();
        if ("ALIPAY".equals(payOrder.getChannel())) {
            payParams.put("appId", "alipay-dev-mock");
            payParams.put("tradeNo", "trade_" + payOrder.getPayOrderNo());
            payParams.put("orderString", "app_id=alipay-dev-mock&out_trade_no=" + payOrder.getPayOrderNo());
            payParams.put("paySign", "mock-alipay-signature");
        } else {
            payParams.put("appId", "wx-dev-mock");
            payParams.put("timeStamp", String.valueOf(System.currentTimeMillis() / 1000));
            payParams.put("nonceStr", payOrder.getPayOrderNo());
            payParams.put("package", "prepay_id=" + payOrder.getPayOrderNo());
            payParams.put("signType", "RSA");
            payParams.put("paySign", "mock-signature");
        }
        return payParams;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("failed to serialize payment json", ex);
        }
    }
}
