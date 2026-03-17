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
class OperationsAcceptanceAlignmentIntegrationTest extends AbstractIntegrationTest {

    @Test
    void dunningInvoiceAndAlertFlowsMatchCurrentAcceptanceContract() throws Exception {
        mockMvc.perform(post("/api/v1/admin/fee-rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "feeType": "WATER",
                                  "unitPrice": 3.2000,
                                  "cycleType": "MONTH",
                                  "pricingMode": "TIERED",
                                  "effectiveFrom": "2026-03-01",
                                  "effectiveTo": "2026-12-31",
                                  "abnormalAbsThreshold": 20.000,
                                  "abnormalMultiplierThreshold": 1.50,
                                  "remark": "验收口径测试",
                                  "waterTiers": [
                                    {"startUsage": 0.000, "endUsage": 5.000, "unitPrice": 2.0000},
                                    {"startUsage": 5.000, "endUsage": 10.000, "unitPrice": 3.0000},
                                    {"startUsage": 10.000, "unitPrice": 4.0000}
                                  ]
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/water-meters")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1001,
                                  "meterNo": "WM-ACCEPT-1001"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/water-readings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1001,
                                  "year": 2026,
                                  "month": 3,
                                  "prevReading": 100.000,
                                  "currReading": 113.000,
                                  "readAt": "2026-03-31T09:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("NORMAL"));

        mockMvc.perform(post("/api/v1/admin/water-readings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1001,
                                  "year": 2026,
                                  "month": 4,
                                  "prevReading": 113.000,
                                  "currReading": 137.000,
                                  "readAt": "2026-04-30T09:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ABNORMAL"));

        mockMvc.perform(get("/api/v1/admin/water-alerts")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("periodYear", "2026")
                        .param("periodMonth", "4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].alertCode").value("ABS_THRESHOLD"))
                .andExpect(jsonPath("$.data[0].status").value("OPEN"));

        mockMvc.perform(post("/api/v1/admin/fee-rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "feeType": "PROPERTY",
                                  "unitPrice": 2.5000,
                                  "cycleType": "MONTH",
                                  "effectiveFrom": "2026-03-01",
                                  "effectiveTo": "2026-12-31",
                                  "remark": "发票与催缴验收"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/admin/bills/generate/property")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "year": 2026,
                                  "month": 8,
                                  "overwriteStrategy": "SKIP"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult billsResult = mockMvc.perform(get("/api/v1/me/bills")
                        .param("pageNo", "1")
                        .param("pageSize", "20")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode billsJson = read(billsResult);
        long propertyBillId = findBillIdByFeeTypeAndPeriod(billsJson, "PROPERTY", "2026-08");
        BigDecimal amountDue = findBillAmountById(billsJson, propertyBillId);

        MvcResult paymentResult = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billId": %s,
                                  "channel": "WECHAT",
                                  "idempotencyKey": "idem-ops-accept-001"
                                }
                                """.formatted(propertyBillId)))
                .andExpect(status().isOk())
                .andReturn();
        String payOrderNo = read(paymentResult).path("data").path("payOrderNo").asText();

        mockMvc.perform(post("/api/v1/callbacks/wechatpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "payOrderNo": "%s",
                                  "outTradeNo": "WX-OPS-0001",
                                  "merchantId": "wx-test-mock-merchant",
                                  "totalAmount": %s,
                                  "sign": "%s"
                                }
                                """.formatted(payOrderNo,
                                amountDue.toPlainString(),
                                PaymentSignUtils.sign(payOrderNo, "WX-OPS-0001", "wx-test-mock-merchant", amountDue, "wechat-test-callback-secret"))))
                .andExpect(status().isOk());

        MvcResult invoiceResult = mockMvc.perform(post("/api/v1/me/invoices/applications")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billId": %s,
                                  "payOrderNo": "%s",
                                  "invoiceTitle": "张三",
                                  "taxNo": "TAX-OPS-001"
                                }
                                """.formatted(propertyBillId, payOrderNo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPLIED"))
                .andReturn();
        long applicationId = read(invoiceResult).path("data").path("id").asLong();

        mockMvc.perform(post("/api/v1/admin/invoices/applications/" + applicationId + "/process")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "REJECTED",
                                  "remark": "票面信息待补充"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REJECTED"))
                .andExpect(jsonPath("$.data.remark").value("票面信息待补充"));

        mockMvc.perform(get("/api/v1/me/invoices/applications")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("REJECTED"));

        Long dunningBillId = jdbcTemplate.queryForObject(
                "SELECT id FROM bill WHERE id <> ? AND fee_type = 'PROPERTY' AND status = 'ISSUED' ORDER BY id LIMIT 1",
                Long.class,
                propertyBillId
        );
        assertThat(dunningBillId).isNotNull();
        jdbcTemplate.update("UPDATE bill SET due_date = '2026-07-31' WHERE id = ?", dunningBillId);

        mockMvc.perform(post("/api/v1/admin/dunning/trigger")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "triggerDate": "2026-08-15"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/dunning/tasks")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("SENT"))
                .andExpect(jsonPath("$.data[0].triggerType").value("MANUAL"));

        mockMvc.perform(get("/api/v1/admin/dunning/bills/" + dunningBillId + "/logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].sendChannel").value("SYSTEM"))
                .andExpect(jsonPath("$.data[0].status").value("SENT"));
    }

    private long findBillIdByFeeTypeAndPeriod(JsonNode billsJson, String feeType, String period) {
        for (JsonNode item : billsJson.path("data").path("list")) {
            if (feeType.equals(item.path("feeType").asText())
                    && period.equals(item.path("period").asText())) {
                return item.path("billId").asLong();
            }
        }
        assertThat(period + ":" + feeType).as("未找到指定账期账单").isBlank();
        return -1L;
    }

    private BigDecimal findBillAmountById(JsonNode billsJson, long billId) {
        for (JsonNode item : billsJson.path("data").path("list")) {
            if (billId == item.path("billId").asLong()) {
                return item.path("amountDue").decimalValue();
            }
        }
        assertThat(billId).as("未找到指定账单金额").isNegative();
        return BigDecimal.ZERO;
    }
}
