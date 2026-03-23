package com.wuye;

import com.fasterxml.jackson.databind.JsonNode;
import com.wuye.payment.util.PaymentSignUtils;
import com.wuye.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PaymentAnnualPropertyIntegrationTest extends AbstractIntegrationTest {

    @Test
    void annualPropertyPaymentCreatePersistsAnnualFlagAndCoverage() throws Exception {
        long billId = createIssuedPropertyBill(2028);

        MvcResult createResult = createAnnualPayment(billId, "annual-payment-001", null);
        JsonNode createJson = read(createResult).path("data");
        String payOrderNo = createJson.path("payOrderNo").asText();

        mockMvc.perform(get("/api/v1/payments/" + payOrderNo)
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payOrderNo").value(payOrderNo))
                .andExpect(jsonPath("$.data.annualPayment").value(true))
                .andExpect(jsonPath("$.data.coveredBillCount").value(1))
                .andExpect(jsonPath("$.data.status").value("PAYING"));

        assertThat(createJson.path("annualPayment").asBoolean()).isTrue();
        assertThat(createJson.path("coveredBillCount").asInt()).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT is_annual_payment FROM pay_order WHERE pay_order_no = ?",
                Integer.class,
                payOrderNo)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT covered_bill_count FROM pay_order WHERE pay_order_no = ?",
                Integer.class,
                payOrderNo)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM pay_order_bill_cover WHERE pay_order_no = ? AND bill_id = ?",
                Integer.class,
                payOrderNo,
                billId)).isEqualTo(1);
    }

    @Test
    void annualPropertyPaymentCallbackMarksBillPaidAndIssuesVoucher() throws Exception {
        long billId = createIssuedPropertyBill(2029);

        MvcResult createResult = createAnnualPayment(billId, "annual-payment-002", null);
        JsonNode createJson = read(createResult).path("data");
        String payOrderNo = createJson.path("payOrderNo").asText();
        BigDecimal payAmount = createJson.path("payAmount").decimalValue();

        String callbackBody = """
                {
                  "payOrderNo": "%s",
                  "outTradeNo": "WX-ANNUAL-0001",
                  "merchantId": "wx-test-mock-merchant",
                  "totalAmount": %s,
                  "sign": "%s"
                }
                """.formatted(
                payOrderNo,
                payAmount.toPlainString(),
                PaymentSignUtils.sign(payOrderNo, "WX-ANNUAL-0001", "wx-test-mock-merchant", payAmount, "wechat-test-callback-secret"));

        mockMvc.perform(post("/api/v1/callbacks/wechatpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true))
                .andExpect(jsonPath("$.data.alreadyProcessed").value(false))
                .andExpect(jsonPath("$.data.rewardIssuedCount").value(1));

        mockMvc.perform(get("/api/v1/payments/" + payOrderNo)
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.annualPayment").value(true))
                .andExpect(jsonPath("$.data.coveredBillCount").value(1))
                .andExpect(jsonPath("$.data.voucherIssued").value(true))
                .andExpect(jsonPath("$.data.rewardIssuedCount").value(1));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM pay_order WHERE pay_order_no = ?",
                String.class,
                payOrderNo)).isEqualTo("SUCCESS");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM bill WHERE id = ?",
                String.class,
                billId)).isEqualTo("PAID");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM payment_voucher WHERE pay_order_no = ?",
                Integer.class,
                payOrderNo)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM coupon_instance WHERE source_type = 'PAYMENT_REWARD' AND source_ref_no LIKE CONCAT(?, '-%')",
                Integer.class,
                payOrderNo)).isEqualTo(1);
    }

    @Test
    void annualPropertyPaymentRejectsCouponUsage() throws Exception {
        long billId = createIssuedPropertyBill(2030);

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billId": %s,
                                  "channel": "WECHAT",
                                  "couponInstanceId": 92001,
                                  "annualPayment": true,
                                  "idempotencyKey": "annual-payment-003"
                                }
                                """.formatted(billId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM coupon_instance WHERE id = 92001",
                String.class)).isEqualTo("NEW");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM pay_order WHERE idempotency_key = 'annual-payment-003'",
                Integer.class)).isEqualTo(0);
    }

    private long createIssuedPropertyBill(int year) throws Exception {
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
                                  "remark": "annual payment test"
                                }
                                """.formatted(year, year)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/bills/generate/property-yearly")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "year": %s,
                                  "overwriteStrategy": "SKIP"
                                }
                                """.formatted(year)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedCount").value(2));

        MvcResult billsResult = mockMvc.perform(get("/api/v1/me/bills")
                        .param("pageNo", "1")
                        .param("pageSize", "20")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andReturn();

        for (JsonNode item : read(billsResult).path("data").path("list")) {
            if ("PROPERTY".equals(item.path("feeType").asText()) && item.path("period").asText().contains(String.valueOf(year))) {
                return item.path("billId").asLong();
            }
        }
        throw new AssertionError("未找到已生成的年度物业费账单, year=" + year);
    }

    private MvcResult createAnnualPayment(long billId, String idempotencyKey, Long couponInstanceId) throws Exception {
        String couponJson = couponInstanceId == null ? "" : """
                                  "couponInstanceId": %s,
                                """.formatted(couponInstanceId);
        return mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billId": %s,
                                  "channel": "WECHAT",
                %s                  "annualPayment": true,
                                  "idempotencyKey": "%s"
                                }
                                """.formatted(billId, couponJson, idempotencyKey)))
                .andExpect(status().isOk())
                .andReturn();
    }
}
