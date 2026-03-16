package com.wuye;

import com.fasterxml.jackson.databind.JsonNode;
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
class MvpFlowIntegrationTest extends AbstractIntegrationTest {

    @Test
    void authAndRoomBindingFlowWorks() throws Exception {
        mockMvc.perform(get("/api/v1/me/profile")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountType").value("RESIDENT"))
                .andExpect(jsonPath("$.data.accountId").value(10001));

        mockMvc.perform(get("/api/v1/me/rooms")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].roomId").value(1001))
                .andExpect(jsonPath("$.data[0].bindingStatus").value("ACTIVE"));

        String applyBody = """
                {
                  "communityId": 100,
                  "buildingNo": "1",
                  "unitNo": "2",
                  "roomNo": "301",
                  "relationType": "OWNER",
                  "applyRemark": "重新申请绑定"
                }
                """;

        mockMvc.perform(post("/api/v1/me/rooms")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(applyBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bindingStatus").value("PENDING"));

        mockMvc.perform(post("/api/v1/me/rooms/1001/confirm")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bindingStatus").value("ACTIVE"));
    }

    @Test
    void billingFlowSupportsPropertyAndWaterBills() throws Exception {
        createFeeRule("PROPERTY", "2.5000");
        createFeeRule("WATER", "3.2000");

        mockMvc.perform(post("/api/v1/admin/bills/generate/property")
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
                .andExpect(jsonPath("$.data.generatedCount").value(2));

        mockMvc.perform(post("/api/v1/admin/water-meters")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1001,
                                  "meterNo": "WM-1001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roomId").value(1001));

        mockMvc.perform(post("/api/v1/admin/water-readings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1001,
                                  "year": 2026,
                                  "month": 3,
                                  "prevReading": 1034.2,
                                  "currReading": 1023.5,
                                  "readAt": "2026-03-31T09:00:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ARGUMENT"));

        mockMvc.perform(post("/api/v1/admin/water-readings")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1001,
                                  "year": 2026,
                                  "month": 3,
                                  "prevReading": 1023.5,
                                  "currReading": 1034.2,
                                  "readAt": "2026-03-31T09:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.usageAmount").value(10.7));

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
                .andExpect(jsonPath("$.data.generatedCount").value(1));

        MvcResult billsResult = mockMvc.perform(get("/api/v1/me/bills")
                        .param("pageNo", "1")
                        .param("pageSize", "20")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andReturn();

        JsonNode billsJson = read(billsResult);
        long waterBillId = findBillIdByFeeType(billsJson, "WATER");

        mockMvc.perform(get("/api/v1/bills/" + waterBillId)
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feeType").value("WATER"))
                .andExpect(jsonPath("$.data.billLines[0].ext.usage").value(10.7));
    }

    @Test
    void paymentCallbackAndMonthlyReportWork() throws Exception {
        createFeeRule("PROPERTY", "2.5000");

        mockMvc.perform(post("/api/v1/admin/bills/generate/property")
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
                .andExpect(jsonPath("$.data.generatedCount").value(2));

        MvcResult billsResult = mockMvc.perform(get("/api/v1/me/bills")
                        .param("pageNo", "1")
                        .param("pageSize", "20")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode billsJson = read(billsResult);
        long propertyBillId = findBillIdByFeeType(billsJson, "PROPERTY");
        BigDecimal selectedBillAmount = findBillAmountById(billsJson, propertyBillId);
        BigDecimal totalDue = sumBillAmounts(billsJson);
        BigDecimal unpaidAmount = totalDue.subtract(selectedBillAmount);

        String createPaymentBody = """
                {
                  "billId": %s,
                  "channel": "WECHAT",
                  "idempotencyKey": "idem-202603-001"
                }
                """.formatted(propertyBillId);

        MvcResult paymentResult = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPaymentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.channel").value("WECHAT"))
                .andReturn();
        String payOrderNo = read(paymentResult).path("data").path("payOrderNo").asText();

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createPaymentBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.payOrderNo").value(payOrderNo));

        String callbackBody = """
                {
                  "payOrderNo": "%s",
                  "outTradeNo": "WX-202603160001"
                }
                """.formatted(payOrderNo);

        mockMvc.perform(post("/api/v1/callbacks/wechatpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true))
                .andExpect(jsonPath("$.data.alreadyProcessed").value(false));

        mockMvc.perform(post("/api/v1/callbacks/wechatpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(callbackBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alreadyProcessed").value(true));

        mockMvc.perform(get("/api/v1/payments/" + payOrderNo)
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        mockMvc.perform(get("/api/v1/admin/reports/monthly")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("periodYear", "2026")
                        .param("periodMonth", "3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.paidCount").value(1))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.payRate").value(0.5))
                .andExpect(jsonPath("$.data.paidAmount").value(selectedBillAmount.doubleValue()))
                .andExpect(jsonPath("$.data.unpaidAmount").value(unpaidAmount.doubleValue()));

        mockMvc.perform(get("/api/v1/admin/reports/monthly")
                        .header("Authorization", "Bearer " + residentToken)
                        .param("periodYear", "2026")
                        .param("periodMonth", "3"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void paymentIdempotencyKeyMustMatchSameRequestSemantics() throws Exception {
        createFeeRule("PROPERTY", "2.5000");
        String period = "2026-04";

        mockMvc.perform(post("/api/v1/admin/bills/generate/property")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "year": 2026,
                                  "month": 4,
                                  "overwriteStrategy": "SKIP"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/me/rooms")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "buildingNo": "1",
                                  "unitNo": "2",
                                  "roomNo": "302",
                                  "relationType": "TENANT",
                                  "applyRemark": "bind second room"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/me/rooms/1002/confirm")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk());

        MvcResult billsResult = mockMvc.perform(get("/api/v1/me/bills")
                        .param("pageNo", "1")
                        .param("pageSize", "50")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode billsJson = read(billsResult);
        long firstBillId = findBillIdByFeeTypeAndRoom(billsJson, "PROPERTY", 1001L);
        long secondBillId = findBillIdByFeeTypeAndRoom(billsJson, "PROPERTY", 1002L);

        String sameKey = "idem-same-key";

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billId": %s,
                                  "channel": "WECHAT",
                                  "idempotencyKey": "%s"
                                }
                                """.formatted(firstBillId, sameKey)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billId": %s,
                                  "channel": "WECHAT",
                                  "idempotencyKey": "%s"
                                }
                                """.formatted(secondBillId, sameKey)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
    }

    @Test
    void duplicateCallbackWithDifferentTradeNoShouldBeRejected() throws Exception {
        createFeeRule("PROPERTY", "2.5000");
        String period = "2026-05";

        mockMvc.perform(post("/api/v1/admin/bills/generate/property")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "year": 2026,
                                  "month": 5,
                                  "overwriteStrategy": "SKIP"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult billsResult = mockMvc.perform(get("/api/v1/me/bills")
                        .param("pageNo", "1")
                        .param("pageSize", "50")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode billsJson = read(billsResult);
        long propertyBillId = findBillIdByFeeTypeAndPeriod(billsJson, "PROPERTY", period);

        MvcResult paymentResult = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billId": %s,
                                  "channel": "WECHAT",
                                  "idempotencyKey": "idem-callback-key"
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
                                  "outTradeNo": "WX-ORDER-1"
                                }
                                """.formatted(payOrderNo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.alreadyProcessed").value(false));

        mockMvc.perform(post("/api/v1/callbacks/wechatpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "payOrderNo": "%s",
                                  "outTradeNo": "WX-ORDER-2"
                                }
                                """.formatted(payOrderNo)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"));
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
                                  "cycleType": "MONTH",
                                  "effectiveFrom": "2026-03-01",
                                  "effectiveTo": "2026-12-31",
                                  "remark": "测试规则"
                                }
                                """.formatted(feeType, unitPrice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.feeType").value(feeType));
    }

    private long findBillIdByFeeType(JsonNode billsJson, String feeType) {
        for (JsonNode item : billsJson.path("data").path("list")) {
            if (feeType.equals(item.path("feeType").asText())) {
                return item.path("billId").asLong();
            }
        }
        assertThat(feeType).as("未找到账单类型").isBlank();
        return -1L;
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

    private long findBillIdByFeeTypeAndRoom(JsonNode billsJson, String feeType, Long roomId) {
        for (JsonNode item : billsJson.path("data").path("list")) {
            if (feeType.equals(item.path("feeType").asText())
                    && roomId.longValue() == item.path("roomId").asLong()) {
                return item.path("billId").asLong();
            }
        }
        assertThat(roomId + ":" + feeType).as("未找到指定房间账单").isBlank();
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

    private BigDecimal sumBillAmounts(JsonNode billsJson) {
        BigDecimal total = BigDecimal.ZERO;
        for (JsonNode item : billsJson.path("data").path("list")) {
            total = total.add(item.path("amountDue").decimalValue());
        }
        return total;
    }
}
