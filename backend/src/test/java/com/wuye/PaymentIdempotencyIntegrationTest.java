package com.wuye;

import com.fasterxml.jackson.databind.JsonNode;
import com.wuye.payment.util.PaymentSignUtils;
import com.wuye.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PaymentIdempotencyIntegrationTest extends AbstractIntegrationTest {

    @Test
    void sameIdempotencyKeyWithSameSemanticsReturnsSamePayOrder() throws Exception {
        long billId = createIssuedPropertyBill(2026, 11);

        MvcResult firstCreate = createPayment(billId, "WECHAT", "idem-payment-001");
        String payOrderNo = read(firstCreate).path("data").path("payOrderNo").asText();

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billId": %s,
                                  "channel": "WECHAT",
                                  "idempotencyKey": "idem-payment-001"
                                }
                                """.formatted(billId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payOrderNo").value(payOrderNo))
                .andExpect(jsonPath("$.data.channel").value("WECHAT"))
                .andExpect(jsonPath("$.data.coveredBillCount").value(1));

        Integer payOrderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM pay_order WHERE idempotency_key = ?",
                Integer.class,
                "idem-payment-001");
        Integer unifiedOrderTransactionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM pay_transaction WHERE pay_order_no = ? AND trade_type = 'UNIFIED_ORDER'",
                Integer.class,
                payOrderNo);

        assertThat(payOrderCount).isEqualTo(1);
        assertThat(unifiedOrderTransactionCount).isEqualTo(1);
    }

    @Test
    void sameIdempotencyKeyWithDifferentSemanticsIsRejected() throws Exception {
        long billId = createIssuedPropertyBill(2026, 12);

        MvcResult firstCreate = createPayment(billId, "WECHAT", "idem-payment-002");
        String payOrderNo = read(firstCreate).path("data").path("payOrderNo").asText();

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billId": %s,
                                  "channel": "ALIPAY",
                                  "idempotencyKey": "idem-payment-002"
                                }
                                """.formatted(billId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        Integer payOrderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM pay_order WHERE idempotency_key = ?",
                Integer.class,
                "idem-payment-002");
        String persistedChannel = jdbcTemplate.queryForObject(
                "SELECT channel FROM pay_order WHERE pay_order_no = ?",
                String.class,
                payOrderNo);

        assertThat(payOrderCount).isEqualTo(1);
        assertThat(persistedChannel).isEqualTo("WECHAT");
    }

    @Test
    void secondBoundResidentCannotCreateAnotherActivePayOrderForSameBill() throws Exception {
        long billId = createIssuedPropertyBill(2026, 10);
        String secondResidentToken = loginResident("resident-lisi");

        MvcResult firstCreate = createPayment(billId, "WECHAT", "idem-shared-room-001");
        String payOrderNo = read(firstCreate).path("data").path("payOrderNo").asText();

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + secondResidentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billId": %s,
                                  "channel": "WECHAT",
                                  "idempotencyKey": "idem-shared-room-002"
                                }
                                """.formatted(billId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));

        Integer payOrderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM pay_order WHERE bill_id = ? AND status IN ('CREATED', 'PAYING')",
                Integer.class,
                billId);
        String persistedPayOrderNo = jdbcTemplate.queryForObject(
                "SELECT pay_order_no FROM pay_order WHERE bill_id = ? ORDER BY id DESC LIMIT 1",
                String.class,
                billId);

        assertThat(payOrderCount).isEqualTo(1);
        assertThat(persistedPayOrderNo).isEqualTo(payOrderNo);
    }

    @Test
    void expiredPayingOrderIsClosedAndBillCanCreateNewPayment() throws Exception {
        long billId = createIssuedPropertyBill(2027, 3);

        String expiredPayOrderNo = read(createPayment(billId, "WECHAT", "idem-expired-001"))
                .path("data")
                .path("payOrderNo")
                .asText();
        jdbcTemplate.update("""
                        UPDATE pay_order
                        SET expired_at = DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 MINUTE)
                        WHERE pay_order_no = ?
                        """,
                expiredPayOrderNo);

        String newPayOrderNo = read(createPayment(billId, "ALIPAY", "idem-expired-002"))
                .path("data")
                .path("payOrderNo")
                .asText();

        assertThat(newPayOrderNo).isNotEqualTo(expiredPayOrderNo);
        String expiredStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM pay_order WHERE pay_order_no = ?",
                String.class,
                expiredPayOrderNo);
        String closeReason = jdbcTemplate.queryForObject(
                "SELECT close_reason FROM pay_order WHERE pay_order_no = ?",
                String.class,
                expiredPayOrderNo);
        String newStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM pay_order WHERE pay_order_no = ?",
                String.class,
                newPayOrderNo);
        String billStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM bill WHERE id = ?",
                String.class,
                billId);
        Integer activePayOrderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM pay_order WHERE bill_id = ? AND status IN ('CREATED', 'PAYING')",
                Integer.class,
                billId);

        assertThat(expiredStatus).isEqualTo("CLOSED");
        assertThat(closeReason).isEqualTo("PAYMENT_TIMEOUT");
        assertThat(newStatus).isEqualTo("PAYING");
        assertThat(billStatus).isEqualTo("ISSUED");
        assertThat(activePayOrderCount).isEqualTo(1);
    }

    @Test
    void expiredPayingOrderReleasesCouponAndClearsBillDiscountBeforeNewPayment() throws Exception {
        long billId = createIssuedPropertyBill(2027, 4);

        String expiredPayOrderNo = read(createPayment(billId, "WECHAT", "idem-expired-coupon-001", 92001L))
                .path("data")
                .path("payOrderNo")
                .asText();
        jdbcTemplate.update("""
                        UPDATE pay_order
                        SET expired_at = DATE_SUB(CURRENT_TIMESTAMP, INTERVAL 1 MINUTE)
                        WHERE pay_order_no = ?
                        """,
                expiredPayOrderNo);

        String newPayOrderNo = read(createPayment(billId, "ALIPAY", "idem-expired-coupon-002"))
                .path("data")
                .path("payOrderNo")
                .asText();

        String expiredStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM pay_order WHERE pay_order_no = ?",
                String.class,
                expiredPayOrderNo);
        String couponStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM coupon_instance WHERE id = 92001",
                String.class);
        BigDecimal discountAmount = jdbcTemplate.queryForObject(
                "SELECT discount_amount_total FROM bill WHERE id = ?",
                BigDecimal.class,
                billId);
        BigDecimal newOrderDiscountAmount = jdbcTemplate.queryForObject(
                "SELECT discount_amount FROM pay_order WHERE pay_order_no = ?",
                BigDecimal.class,
                newPayOrderNo);

        assertThat(expiredStatus).isEqualTo("CLOSED");
        assertThat(couponStatus).isEqualTo("NEW");
        assertThat(discountAmount).isEqualByComparingTo("0.00");
        assertThat(newOrderDiscountAmount).isEqualByComparingTo("0.00");
    }

    @Test
    void duplicateCallbackIsIdempotentAndDoesNotDuplicateVoucher() throws Exception {
        long billId = createIssuedPropertyBill(2027, 1);

        MvcResult createResult = createPayment(billId, "WECHAT", "idem-callback-001");
        JsonNode createJson = read(createResult).path("data");
        String payOrderNo = createJson.path("payOrderNo").asText();
        BigDecimal payAmount = createJson.path("payAmount").decimalValue();

        String callbackBody = """
                {
                  "payOrderNo": "%s",
                  "outTradeNo": "WX-IDEM-0001",
                  "merchantId": "wx-test-mock-merchant",
                  "totalAmount": %s,
                  "sign": "%s"
                }
                """.formatted(
                payOrderNo,
                payAmount.toPlainString(),
                PaymentSignUtils.sign(payOrderNo, "WX-IDEM-0001", "wx-test-mock-merchant", payAmount, "wechat-test-callback-secret"));

        mockMvc.perform(post("/api/v1/callbacks/wechatpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true))
                .andExpect(jsonPath("$.data.alreadyProcessed").value(false))
                .andExpect(jsonPath("$.data.rewardIssuedCount").value(1));

        mockMvc.perform(post("/api/v1/callbacks/wechatpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true))
                .andExpect(jsonPath("$.data.alreadyProcessed").value(true))
                .andExpect(jsonPath("$.data.rewardIssuedCount").value(0));

        mockMvc.perform(get("/api/v1/payments/" + payOrderNo)
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.voucherIssued").value(true));

        Integer voucherCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM payment_voucher WHERE pay_order_no = ?",
                Integer.class,
                payOrderNo);
        Integer callbackTransactionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM pay_transaction WHERE pay_order_no = ? AND trade_type = 'WECHAT_CALLBACK'",
                Integer.class,
                payOrderNo);
        Integer rewardCouponCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM coupon_instance WHERE source_type = 'PAYMENT_REWARD' AND source_ref_no LIKE CONCAT(?, '-%')",
                Integer.class,
                payOrderNo);
        String payOrderStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM pay_order WHERE pay_order_no = ?",
                String.class,
                payOrderNo);
        String billStatus = jdbcTemplate.queryForObject(
                "SELECT status FROM bill WHERE id = ?",
                String.class,
                billId);

        assertThat(voucherCount).isEqualTo(1);
        assertThat(callbackTransactionCount).isEqualTo(2);
        assertThat(rewardCouponCount).isEqualTo(1);
        assertThat(payOrderStatus).isEqualTo("SUCCESS");
        assertThat(billStatus).isEqualTo("PAID");
    }

    private long createIssuedPropertyBill(int year, int month) throws Exception {
        mockMvc.perform(post("/api/v1/admin/fee-rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "feeType": "PROPERTY",
                                  "unitPrice": 2.5000,
                                  "cycleType": "YEAR",
                                  "effectiveFrom": "%s-01-01",
                                  "effectiveTo": "%s-12-31",
                                  "remark": "payment idempotency test"
                                }
                                """.formatted(year, year)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/bills/generate/property")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "year": %s,
                                  "month": %s,
                                  "overwriteStrategy": "SKIP"
                                }
                                """.formatted(year, month)))
                .andExpect(status().isOk());

        List<Long> billIds = jdbcTemplate.queryForList("""
                        SELECT b.id
                        FROM bill b
                        JOIN account_room ar ON ar.room_id = b.room_id
                        WHERE ar.account_id = 10001
                          AND ar.status = 'ACTIVE'
                          AND b.fee_type = 'PROPERTY'
                          AND b.period_year = ?
                          AND b.period_month IS NULL
                          AND b.status = 'ISSUED'
                        ORDER BY b.id DESC
                        """,
                Long.class,
                year);
        if (billIds.isEmpty()) {
            throw new AssertionError("未找到已生成的物业费账单, year=" + year);
        }
        return billIds.get(0);
    }

    private MvcResult createPayment(long billId, String channel, String idempotencyKey) throws Exception {
        return createPayment(billId, channel, idempotencyKey, null);
    }

    private MvcResult createPayment(long billId, String channel, String idempotencyKey, Long couponInstanceId) throws Exception {
        String couponJson = couponInstanceId == null ? "" : """
                                  "couponInstanceId": %s,
                                """.formatted(couponInstanceId);
        return mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billId": %s,
                                  "channel": "%s",
                %s
                                  "idempotencyKey": "%s"
                                }
                                """.formatted(billId, channel, couponJson, idempotencyKey)))
                .andExpect(status().isOk())
                .andReturn();
    }
}
