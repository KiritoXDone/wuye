package com.wuye;

import com.fasterxml.jackson.databind.JsonNode;
import com.wuye.payment.util.PaymentSignUtils;
import com.wuye.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminBillingAndOperationsIntegrationTest extends AbstractIntegrationTest {

    @Test
    void feeRuleBillReportDunningAndAlipayVoucherEndpointsWorkTogether() throws Exception {
        long tempRuleId = createFeeRule("WATER", "MONTH", 2034, "4.2000");
        mockMvc.perform(get("/api/v1/admin/fee-rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("communityId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id==%s)]".formatted(tempRuleId)).exists());

        mockMvc.perform(delete("/api/v1/admin/fee-rules/" + tempRuleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM fee_rule WHERE id = ?",
                Integer.class,
                tempRuleId)).isEqualTo(0);

        createFeeRule("PROPERTY", "YEAR", 2032, "2.5000");
        createFeeRule("PROPERTY", "YEAR", 2033, "2.5000");
        createFeeRule("WATER", "MONTH", 2032, "3.2500");

        mockMvc.perform(post("/api/v1/admin/bills/generate/property")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "year": 2032,
                                  "overwriteStrategy": "SKIP"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedCount").value(2));

        mockMvc.perform(post("/api/v1/admin/bills/generate/property-yearly")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "year": 2033,
                                  "overwriteStrategy": "SKIP"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedCount").value(2));

        long property2032BillId = findBillId(1001L, "PROPERTY", 2032, null);
        long property2033BillId = findBillId(1001L, "PROPERTY", 2033, null);

        mockMvc.perform(get("/api/v1/admin/bills")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("periodYear", "2032")
                        .param("feeType", "PROPERTY")
                        .param("status", "ISSUED")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.list[0].feeType").value("PROPERTY"));

        mockMvc.perform(get("/api/v1/admin/bills/" + property2032BillId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.billId").value(property2032BillId))
                .andExpect(jsonPath("$.data.roomId").value(1001))
                .andExpect(jsonPath("$.data.feeType").value("PROPERTY"))
                .andExpect(jsonPath("$.data.status").value("ISSUED"));

        MvcResult paymentResult = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billId": %s,
                                  "channel": "ALIPAY",
                                  "idempotencyKey": "alipay-voucher-2032"
                                }
                                """.formatted(property2032BillId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.channel").value("ALIPAY"))
                .andReturn();

        JsonNode paymentJson = read(paymentResult).path("data");
        String payOrderNo = paymentJson.path("payOrderNo").asText();
        BigDecimal payAmount = paymentJson.path("payAmount").decimalValue();

        mockMvc.perform(post("/api/v1/callbacks/alipay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "payOrderNo": "%s",
                                  "outTradeNo": "ALI-2032-0001",
                                  "merchantId": "alipay-test-mock-merchant",
                                  "totalAmount": %s,
                                  "sign": "%s"
                                }
                                """.formatted(
                                payOrderNo,
                                payAmount.toPlainString(),
                                PaymentSignUtils.sign(payOrderNo, "ALI-2032-0001", "alipay-test-mock-merchant", payAmount, "alipay-test-callback-secret"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true))
                .andExpect(jsonPath("$.data.alreadyProcessed").value(false));

        mockMvc.perform(get("/api/v1/payments/" + payOrderNo + "/voucher")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payOrderNo").value(payOrderNo))
                .andExpect(jsonPath("$.data.billId").value(property2032BillId))
                .andExpect(jsonPath("$.data.status").value("ISSUED"));

        mockMvc.perform(post("/api/v1/admin/water-meters")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1001,
                                  "meterNo": "WM-1001-CASE",
                                  "installAt": "2026-01-01"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult readingResult = mockMvc.perform(post("/api/v1/admin/water-readings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1001,
                                  "year": 2032,
                                  "month": 8,
                                  "prevReading": 10.000,
                                  "currReading": 16.000,
                                  "readAt": "2032-08-15T10:00:00",
                                  "remark": "water-report-case"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roomId").value(1001))
                .andExpect(jsonPath("$.data.billId").isNumber())
                .andReturn();

        long readingId = read(readingResult).path("data").path("id").asLong();
        long waterBillId = read(readingResult).path("data").path("billId").asLong();

        mockMvc.perform(get("/api/v1/admin/water-readings")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("periodYear", "2032")
                        .param("periodMonth", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].roomId").value(1001));

        mockMvc.perform(post("/api/v1/admin/bills/generate/water")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "year": 2032,
                                  "month": 8,
                                  "overwriteStrategy": "SKIP"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedCount").value(0));

        mockMvc.perform(get("/api/v1/admin/reports/property-yearly")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("periodYear", "2032"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.paidCount").value(1));

        mockMvc.perform(get("/api/v1/admin/reports/water-monthly")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("periodYear", "2032")
                        .param("periodMonth", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.paidCount").value(0));

        mockMvc.perform(get("/api/v1/admin/reports/monthly")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("periodYear", "2032")
                        .param("periodMonth", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalCount").value(2));

        mockMvc.perform(get("/api/v1/admin/dashboard/summary")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("periodYear", "2032")
                        .param("periodMonth", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.periodYear").value(2032))
                .andExpect(jsonPath("$.data.periodMonth").value(8))
                .andExpect(jsonPath("$.data.totalCount").value(2));

        long targetDunningBillId = findBillId(1002L, "PROPERTY", 2032, null);

        MvcResult dunningTriggerResult = mockMvc.perform(post("/api/v1/admin/dunning/trigger")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "triggerDate": "2033-01-15"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andReturn();
        boolean triggerContainsTarget = false;
        for (JsonNode item : read(dunningTriggerResult).path("data")) {
            if (item.path("billId").asLong() == targetDunningBillId) {
                triggerContainsTarget = true;
                break;
            }
        }
        assertThat(triggerContainsTarget).isTrue();

        MvcResult dunningTasksResult = mockMvc.perform(get("/api/v1/admin/dunning/tasks")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();
        boolean taskContainsTarget = false;
        for (JsonNode item : read(dunningTasksResult).path("data")) {
            if (item.path("billId").asLong() == targetDunningBillId
                    && "SENT".equals(item.path("status").asText())) {
                taskContainsTarget = true;
                break;
            }
        }
        assertThat(taskContainsTarget).isTrue();

        mockMvc.perform(get("/api/v1/admin/dunning/bills/" + targetDunningBillId + "/logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].billId").value(targetDunningBillId))
                .andExpect(jsonPath("$.data[0].status").value("SENT"));

        mockMvc.perform(delete("/api/v1/admin/water-readings/" + readingId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM water_meter_reading WHERE id = ?",
                String.class,
                readingId)).isEqualTo("DELETED");
        mockMvc.perform(get("/api/v1/admin/water-readings")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("periodYear", "2032")
                        .param("periodMonth", "8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM bill WHERE id = ?",
                String.class,
                waterBillId)).isEqualTo("CANCELLED");

        mockMvc.perform(delete("/api/v1/admin/bills/" + property2033BillId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM bill WHERE id = ?",
                String.class,
                property2033BillId)).isEqualTo("CANCELLED");
    }

    private long createFeeRule(String feeType, String cycleType, int year, String unitPrice) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/fee-rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "feeType": "%s",
                                  "unitPrice": %s,
                                  "cycleType": "%s",
                                  "effectiveFrom": "%s-01-01",
                                  "effectiveTo": "%s-12-31",
                                  "remark": "ops-integration"
                                }
                                """.formatted(feeType, unitPrice, cycleType, year, year)))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).path("data").path("id").asLong();
    }

    private long findBillId(long roomId, String feeType, int periodYear, Integer periodMonth) {
        if (periodMonth == null) {
            return jdbcTemplate.queryForObject("""
                    SELECT id
                    FROM bill
                    WHERE room_id = ?
                      AND fee_type = ?
                      AND period_year = ?
                    ORDER BY id DESC
                    LIMIT 1
                    """, Long.class, roomId, feeType, periodYear);
        }
        return jdbcTemplate.queryForObject("""
                SELECT id
                FROM bill
                WHERE room_id = ?
                  AND fee_type = ?
                  AND period_year = ?
                  AND period_month = ?
                ORDER BY id DESC
                LIMIT 1
                """, Long.class, roomId, feeType, periodYear, periodMonth);
    }
}
