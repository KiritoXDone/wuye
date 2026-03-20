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
class P2FlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void tieredWaterPricingWorks() throws Exception {
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
                                  "remark": "分段水价测试",
                                  "waterTiers": [
                                    {"startUsage": 0.000, "endUsage": 5.000, "unitPrice": 2.0000},
                                    {"startUsage": 5.000, "endUsage": 10.000, "unitPrice": 3.0000},
                                    {"startUsage": 10.000, "unitPrice": 4.0000}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pricingMode").value("TIERED"))
                .andExpect(jsonPath("$.data.waterTiers[0].unitPrice").value(2.0));

        mockMvc.perform(post("/api/v1/admin/water-meters")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1001,
                                  "meterNo": "WM-P2-1001"
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
                .andExpect(jsonPath("$.data.status").value("NORMAL"));

        mockMvc.perform(post("/api/v1/admin/bills/generate/water")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "year": 2026,
                                  "month": 3,
                                  "overwriteStrategy": "SKIP"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedCount").value(0));

        MvcResult billsResult = mockMvc.perform(get("/api/v1/me/bills")
                        .param("pageNo", "1")
                        .param("pageSize", "20")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode billsJson = read(billsResult);
        long waterBillId = findBillIdByFeeTypeAndPeriod(billsJson, "WATER", "2026-03");

        mockMvc.perform(get("/api/v1/bills/" + waterBillId)
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.billLines[0].lineAmount").value(37.0))
                .andExpect(jsonPath("$.data.billLines[0].ext.pricingMode").value("TIERED"))
                .andExpect(jsonPath("$.data.billLines[0].ext.tierBreakdown[2].amount").value(12.0));
    }

    @Test
    void paymentVoucherInvoiceDunningAndOrgUnitFlowsWork() throws Exception {
        createFeeRule("PROPERTY", "2.5000");

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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedCount").value(2));

        MvcResult billsResult = mockMvc.perform(get("/api/v1/me/bills")
                        .param("pageNo", "1")
                        .param("pageSize", "20")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode billsJson = read(billsResult);
        long propertyBillId = billsJson.path("data").path("list").get(0).path("billId").asLong();
        BigDecimal amountDue = findBillAmountById(billsJson, propertyBillId);

        MvcResult paymentResult = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billId": %s,
                                  "channel": "WECHAT",
                                  "idempotencyKey": "idem-p2-payment-001"
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
                                  "outTradeNo": "WX-P2-0001",
                                  "merchantId": "wx-test-mock-merchant",
                                  "totalAmount": %s,
                                  "sign": "%s"
                                }
                                """.formatted(payOrderNo,
                                amountDue.toPlainString(),
                                PaymentSignUtils.sign(payOrderNo, "WX-P2-0001", "wx-test-mock-merchant", amountDue, "wechat-test-callback-secret"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true));

        mockMvc.perform(get("/api/v1/payments/" + payOrderNo)
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.voucherIssued").value(true));

        mockMvc.perform(get("/api/v1/payments/" + payOrderNo + "/voucher")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.voucherNo").value("VCH-" + payOrderNo))
                .andExpect(jsonPath("$.data.content.feeType").value("PROPERTY"));

        MvcResult invoiceResult = mockMvc.perform(post("/api/v1/me/invoices/applications")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billId": %s,
                                  "payOrderNo": "%s",
                                  "invoiceTitle": "张三",
                                  "taxNo": "TAX-001"
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
                                  "status": "APPROVED",
                                  "remark": "已开具电子发票"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));

        mockMvc.perform(get("/api/v1/me/invoices/applications")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("APPROVED"));

        mockMvc.perform(get("/api/v1/admin/org-units")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].tenantCode").value("TENANT-DEMO"));

        mockMvc.perform(get("/api/v1/agent/groups")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].tenantCode").value("TENANT-DEMO"))
                .andExpect(jsonPath("$.data[0].orgUnitId").value(10002));

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
                .andExpect(jsonPath("$.data[0].tenantCode").value("TENANT-DEMO"))
                .andExpect(jsonPath("$.data[0].groupId").value(5001));

        mockMvc.perform(get("/api/v1/admin/dunning/bills/" + dunningBillId + "/logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("SENT"));

        Long billGroupId = jdbcTemplate.queryForObject("SELECT group_id FROM bill WHERE id = ?", Long.class, propertyBillId);
        assertThat(billGroupId).isEqualTo(5001L);
    }

    private void createFeeRule(String feeType, String unitPrice) throws Exception {
        mockMvc.perform(post("/api/v1/admin/fee-rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "feeType": "%s",
                                  "unitPrice": %s,
                                  "cycleType": "%s",
                                  "effectiveFrom": "2026-03-01",
                                  "effectiveTo": "2026-12-31",
                                  "remark": "测试规则"
                                }
                                """.formatted(feeType, unitPrice, "PROPERTY".equals(feeType) ? "YEAR" : "MONTH")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feeType").value(feeType));
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
