package com.wuye;

import com.wuye.common.infra.mq.PaymentEventPublisher;
import com.wuye.common.infra.redis.RedisCallbackLock;
import com.wuye.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doReturn;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class InfraAndAiRuntimeConfigIntegrationTest extends AbstractIntegrationTest {

    @MockBean
    private PaymentEventPublisher paymentEventPublisher;

    @MockBean
    private RedisCallbackLock mockedRedisCallbackLock;

    @Test
    void paymentSuccessCallbackStillWorksWithInfraAbstractionsEnabledInCodePath() throws Exception {
        doReturn(true).when(mockedRedisCallbackLock).acquire(anyString());
        doNothing().when(mockedRedisCallbackLock).release(anyString());

        createFeeRule("PROPERTY", "2.5000");

        mockMvc.perform(post("/api/v1/admin/bills/generate/property")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "year": 2026,
                                  "month": 9,
                                  "overwriteStrategy": "SKIP"
                                }
                                """))
                .andExpect(status().isOk());

        var billsResult = mockMvc.perform(get("/api/v1/me/bills")
                        .param("pageNo", "1")
                        .param("pageSize", "20")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andReturn();

        long billId = read(billsResult).path("data").path("list").get(0).path("billId").asLong();
        BigDecimal amountDue = read(billsResult).path("data").path("list").get(0).path("amountDue").decimalValue();

        var paymentResult = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billId": %s,
                                  "channel": "WECHAT",
                                  "idempotencyKey": "infra-payment-key"
                                }
                                """.formatted(billId)))
                .andExpect(status().isOk())
                .andReturn();

        String payOrderNo = read(paymentResult).path("data").path("payOrderNo").asText();
        mockMvc.perform(post("/api/v1/callbacks/wechatpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "payOrderNo": "%s",
                                  "outTradeNo": "WX-INFRA-0001",
                                  "merchantId": "wx-test-mock-merchant",
                                  "totalAmount": %s,
                                  "sign": "%s"
                                }
                                """.formatted(payOrderNo,
                                amountDue.toPlainString(),
                                com.wuye.payment.util.PaymentSignUtils.sign(payOrderNo, "WX-INFRA-0001", "wx-test-mock-merchant", amountDue, "wechat-test-callback-secret"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true));

        verify(paymentEventPublisher, times(1)).publishPaymentSuccess(any());
    }

    @Test
    void paymentSuccessCallbackShouldNotBreakWhenRedisLockDegrades() throws Exception {
        doReturn(true).when(mockedRedisCallbackLock).acquire(anyString());
        doThrow(new RuntimeException("redis down")).when(mockedRedisCallbackLock).release(anyString());

        createFeeRule("PROPERTY", "2.5000");

        mockMvc.perform(post("/api/v1/admin/bills/generate/property")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "year": 2026,
                                  "month": 10,
                                  "overwriteStrategy": "SKIP"
                                }
                                """))
                .andExpect(status().isOk());

        var billsResult = mockMvc.perform(get("/api/v1/me/bills")
                        .param("pageNo", "1")
                        .param("pageSize", "20")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andReturn();

        long billId = findBillIdByFeeTypeAndPeriod(read(billsResult), "PROPERTY", "2026-10");
        BigDecimal amountDue = findBillAmountById(read(billsResult), billId);

        var paymentResult = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billId": %s,
                                  "channel": "WECHAT",
                                  "idempotencyKey": "infra-payment-key-2"
                                }
                                """.formatted(billId)))
                .andExpect(status().isOk())
                .andReturn();

        String payOrderNo = read(paymentResult).path("data").path("payOrderNo").asText();
        mockMvc.perform(post("/api/v1/callbacks/wechatpay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "payOrderNo": "%s",
                                  "outTradeNo": "WX-INFRA-0002",
                                  "merchantId": "wx-test-mock-merchant",
                                  "totalAmount": %s,
                                  "sign": "%s"
                                }
                                """.formatted(payOrderNo,
                                amountDue.toPlainString(),
                                com.wuye.payment.util.PaymentSignUtils.sign(payOrderNo, "WX-INFRA-0002", "wx-test-mock-merchant", amountDue, "wechat-test-callback-secret"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accepted").value(true));
    }

    private long findBillIdByFeeTypeAndPeriod(com.fasterxml.jackson.databind.JsonNode billsJson, String feeType, String period) {
        for (com.fasterxml.jackson.databind.JsonNode item : billsJson.path("data").path("list")) {
            if (feeType.equals(item.path("feeType").asText())
                    && period.equals(item.path("period").asText())) {
                return item.path("billId").asLong();
            }
        }
        assertThat(period + ":" + feeType).as("未找到指定账期账单").isBlank();
        return -1L;
    }

    private BigDecimal findBillAmountById(com.fasterxml.jackson.databind.JsonNode billsJson, long billId) {
        for (com.fasterxml.jackson.databind.JsonNode item : billsJson.path("data").path("list")) {
            if (billId == item.path("billId").asLong()) {
                return item.path("amountDue").decimalValue();
            }
        }
        assertThat(billId).as("未找到指定账单金额").isNegative();
        return BigDecimal.ZERO;
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
                                  "remark": "infra test"
                                }
                                """.formatted(feeType, unitPrice)))
                .andExpect(status().isOk());
    }
}
