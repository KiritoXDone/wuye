package com.wuye;

import com.fasterxml.jackson.databind.JsonNode;
import com.wuye.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CouponInterfaceIntegrationTest extends AbstractIntegrationTest {

    @Test
    void adminAndResidentCouponEndpointsSupportTemplateRuleIssueValidateAndExchange() throws Exception {
        long removableTemplateId = createTemplate("PAY-TMP-DEL", "PAYMENT", "PROPERTY", "支付抵扣券");
        mockMvc.perform(get("/api/v1/admin/coupon-templates")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id==%s)]".formatted(removableTemplateId)).exists());

        long managedTemplateId = saveCouponTemplate("VCH-MANAGED-001", "VOUCHER", "停车权益券");
        mockMvc.perform(get("/api/v1/admin/coupons")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id==%s)]".formatted(managedTemplateId)).exists());

        long ruleId = createCouponRule("登录送券", "LOGIN", "VCH-MANAGED-001");
        mockMvc.perform(get("/api/v1/admin/coupon-rules")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.id==%s)]".formatted(ruleId)).exists());

        long propertyBillId = createIssuedPropertyBill(2035);
        mockMvc.perform(get("/api/v1/me/coupons")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].couponInstanceId").value(92001));

        mockMvc.perform(post("/api/v1/coupons/validate")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billId": %s,
                                  "couponInstanceId": 92001
                                }
                                """.formatted(propertyBillId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.couponInstanceId").value(92001))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.discountAmount").value(10));

        MvcResult issueResult = mockMvc.perform(post("/api/v1/admin/coupon-instances/manual-issue")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateId": %s,
                                  "ownerAccountId": 10001,
                                  "issueCount": 1,
                                  "remark": "manual voucher issue"
                                }
                                """.formatted(managedTemplateId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateId").value(managedTemplateId))
                .andExpect(jsonPath("$.data.ownerAccountId").value(10001))
                .andExpect(jsonPath("$.data.issueCount").value(1))
                .andReturn();

        long couponInstanceId = read(issueResult).path("data").path("couponInstanceIds").get(0).asLong();

        mockMvc.perform(get("/api/v1/admin/coupon-instances")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("templateId", String.valueOf(managedTemplateId))
                        .param("ownerAccountId", "10001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].couponInstanceId").value(couponInstanceId));

        mockMvc.perform(get("/api/v1/admin/coupons/" + managedTemplateId + "/instances")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].couponInstanceId").value(couponInstanceId));

        mockMvc.perform(post("/api/v1/vouchers/" + couponInstanceId + "/exchange")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "前台兑换"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.couponInstanceId").value(couponInstanceId))
                .andExpect(jsonPath("$.data.exchangeStatus").value("PENDING"));

        mockMvc.perform(get("/api/v1/me/vouchers")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].couponInstanceId").value(couponInstanceId))
                .andExpect(jsonPath("$.data[0].exchangeStatus").value("PENDING"));

        MvcResult exchangeListResult = mockMvc.perform(get("/api/v1/admin/coupons/" + managedTemplateId + "/exchanges")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].couponInstanceId").value(couponInstanceId))
                .andReturn();

        long exchangeId = read(exchangeListResult).path("data").get(0).path("exchangeId").asLong();

        mockMvc.perform(post("/api/v1/admin/voucher-exchanges/" + exchangeId + "/status")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "exchangeStatus": "FULFILLED",
                                  "remark": "线下已履约"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/me/vouchers")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].couponInstanceId").value(couponInstanceId))
                .andExpect(jsonPath("$.data[0].exchangeStatus").value("FULFILLED"));

        mockMvc.perform(delete("/api/v1/admin/coupon-rules/" + ruleId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM coupon_issue_rule WHERE id = ?",
                Integer.class,
                ruleId)).isEqualTo(0);

        mockMvc.perform(delete("/api/v1/admin/coupon-templates/" + removableTemplateId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM coupon_template WHERE id = ?",
                Integer.class,
                removableTemplateId)).isEqualTo(0);
    }

    private long createTemplate(String templateCode, String type, String feeType, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/coupon-templates")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "%s",
                                  "type": "%s",
                                  "feeType": "%s",
                                  "name": "%s",
                                  "discountMode": "FIXED",
                                  "valueAmount": 12.00,
                                  "thresholdAmount": 100.00,
                                  "validFrom": "2026-01-01 00:00:00",
                                  "validTo": "2036-12-31 23:59:59"
                                }
                                """.formatted(templateCode, type, feeType, name)))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).path("data").path("id").asLong();
    }

    private long saveCouponTemplate(String templateCode, String type, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/coupons")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "%s",
                                  "type": "%s",
                                  "name": "%s",
                                  "discountMode": "FIXED",
                                  "valueAmount": 0.00,
                                  "thresholdAmount": 0.00,
                                  "validFrom": "2026-01-01 00:00:00",
                                  "validTo": "2036-12-31 23:59:59",
                                  "status": 1
                                }
                                """.formatted(templateCode, type, name)))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).path("data").path("id").asLong();
    }

    private long createCouponRule(String name, String triggerType, String templateCode) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/coupon-rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "triggerType": "%s",
                                  "feeType": "PROPERTY",
                                  "templateCode": "%s",
                                  "minPayAmount": 0.00,
                                  "rewardCount": 1
                                }
                                """.formatted(name, triggerType, templateCode)))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).path("data").path("id").asLong();
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
                                  "remark": "coupon-validate"
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
                .andExpect(status().isOk());

        return jdbcTemplate.queryForObject("""
                SELECT id
                FROM bill
                WHERE room_id = 1001
                  AND fee_type = 'PROPERTY'
                  AND period_year = ?
                ORDER BY id DESC
                LIMIT 1
                """, Long.class, year);
    }
}
