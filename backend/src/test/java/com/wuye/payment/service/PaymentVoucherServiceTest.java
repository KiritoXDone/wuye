package com.wuye.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.bill.entity.Bill;
import com.wuye.bill.mapper.BillMapper;
import com.wuye.common.security.LoginUser;
import com.wuye.payment.entity.PayOrder;
import com.wuye.payment.entity.PayOrderBillCover;
import com.wuye.payment.entity.PaymentVoucher;
import com.wuye.payment.mapper.PayOrderBillCoverMapper;
import com.wuye.payment.mapper.PayOrderMapper;
import com.wuye.payment.mapper.PaymentVoucherMapper;
import com.wuye.payment.vo.PaymentVoucherVO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PaymentVoucherServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void ensureVoucherShouldFormatYearlyCoverageWithoutMonth() {
        PaymentVoucherMapper paymentVoucherMapper = mock(PaymentVoucherMapper.class);
        PayOrderMapper payOrderMapper = mock(PayOrderMapper.class);
        PayOrderBillCoverMapper payOrderBillCoverMapper = mock(PayOrderBillCoverMapper.class);
        BillMapper billMapper = mock(BillMapper.class);
        PaymentVoucherService paymentVoucherService = new PaymentVoucherService(
                paymentVoucherMapper,
                payOrderMapper,
                payOrderBillCoverMapper,
                billMapper,
                new PaymentAccessService(null, null),
                objectMapper
        );

        PayOrder payOrder = buildPayOrder("PAY-ANNUAL-001");
        Bill bill = buildBill();
        LocalDateTime issuedAt = LocalDateTime.of(2026, 3, 23, 11, 0, 0);

        when(paymentVoucherMapper.findByPayOrderNo(payOrder.getPayOrderNo())).thenReturn(null);
        when(payOrderBillCoverMapper.findByPayOrderNo(payOrder.getPayOrderNo()))
                .thenReturn(List.of(buildCover(2028, null), buildCover(2029, 3)));
        doAnswer(invocation -> 1).when(paymentVoucherMapper).insert(any(PaymentVoucher.class));

        PaymentVoucher voucher = paymentVoucherService.ensureVoucher(payOrder, bill, issuedAt);

        assertThat(voucher.getContentJson()).contains("\"coveredPeriods\":[\"2028\",\"2029-03\"]");
        assertThat(voucher.getContentJson()).contains("\"annualPayment\":true");
    }

    @Test
    void getVoucherShouldReturnEmptyContentWhenStoredContentIsBlank() {
        PaymentVoucherMapper paymentVoucherMapper = mock(PaymentVoucherMapper.class);
        PayOrderMapper payOrderMapper = mock(PayOrderMapper.class);
        PayOrderBillCoverMapper payOrderBillCoverMapper = mock(PayOrderBillCoverMapper.class);
        BillMapper billMapper = mock(BillMapper.class);
        PaymentVoucherService paymentVoucherService = new PaymentVoucherService(
                paymentVoucherMapper,
                payOrderMapper,
                payOrderBillCoverMapper,
                billMapper,
                new PaymentAccessService(null, null),
                objectMapper
        );

        String payOrderNo = "PAY-ADMIN-001";
        PayOrder payOrder = buildPayOrder(payOrderNo);
        PaymentVoucher storedVoucher = new PaymentVoucher();
        storedVoucher.setPayOrderNo(payOrderNo);
        storedVoucher.setBillId(10001L);
        storedVoucher.setVoucherNo("VCH-" + payOrderNo);
        storedVoucher.setAmount(new BigDecimal("88.50"));
        storedVoucher.setStatus("ISSUED");
        storedVoucher.setIssuedAt(LocalDateTime.of(2026, 3, 23, 11, 30, 0));
        storedVoucher.setContentJson("   ");

        when(payOrderMapper.findByPayOrderNo(payOrderNo)).thenReturn(payOrder);
        when(billMapper.findById(payOrder.getBillId())).thenReturn(buildBill());
        when(paymentVoucherMapper.findByPayOrderNo(payOrderNo)).thenReturn(storedVoucher);

        PaymentVoucherVO voucher = paymentVoucherService.getVoucher(
                new LoginUser(1L, "ADMIN", "ADMIN", "admin", List.of("ADMIN"), "ALL", List.of()),
                payOrderNo
        );

        assertThat(voucher.getContent()).isEmpty();
    }

    private PayOrder buildPayOrder(String payOrderNo) {
        PayOrder payOrder = new PayOrder();
        payOrder.setPayOrderNo(payOrderNo);
        payOrder.setBillId(10001L);
        payOrder.setAccountId(20001L);
        payOrder.setAnnualPayment(true);
        payOrder.setCoveredBillCount(2);
        payOrder.setPayAmount(new BigDecimal("88.50"));
        return payOrder;
    }

    private Bill buildBill() {
        Bill bill = new Bill();
        bill.setId(10001L);
        bill.setBillNo("BILL-10001");
        bill.setFeeType("PROPERTY");
        return bill;
    }

    private PayOrderBillCover buildCover(int periodYear, Integer periodMonth) {
        PayOrderBillCover cover = new PayOrderBillCover();
        cover.setPeriodYear(periodYear);
        cover.setPeriodMonth(periodMonth);
        return cover;
    }
}
