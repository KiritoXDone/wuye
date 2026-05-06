package com.wuye;

import com.fasterxml.jackson.databind.JsonNode;
import com.wuye.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CouponSeckillIntegrationTest extends AbstractIntegrationTest {

    @Test
    void seckillRequestCreatesCouponOnceAndDuplicateRequestReturnsSameOrder() throws Exception {
        long campaignId = createCampaignByApi(1);

        MvcResult first = mockMvc.perform(post("/api/v1/coupons/seckill/" + campaignId + "/orders")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestId": "seckill-req-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andReturn();

        JsonNode firstData = read(first).path("data");
        String orderNo = firstData.path("orderNo").asText();
        long couponInstanceId = firstData.path("couponInstanceId").asLong();

        mockMvc.perform(post("/api/v1/coupons/seckill/" + campaignId + "/orders")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestId": "seckill-req-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNo").value(orderNo))
                .andExpect(jsonPath("$.data.couponInstanceId").value(couponInstanceId));

        mockMvc.perform(get("/api/v1/coupons/seckill/orders/" + orderNo)
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.couponInstanceId").value(couponInstanceId));

        Integer stock = jdbcTemplate.queryForObject(
                "SELECT available_stock FROM coupon_seckill_campaign WHERE id = ?",
                Integer.class,
                campaignId);
        Integer orderCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM coupon_seckill_order WHERE campaign_id = ?",
                Integer.class,
                campaignId);
        Integer couponCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM coupon_instance WHERE source_type = 'SECKILL' AND source_ref_no = ?",
                Integer.class,
                orderNo);

        assertThat(stock).isZero();
        assertThat(orderCount).isEqualTo(1);
        assertThat(couponCount).isEqualTo(1);
    }

    @Test
    void secondRequestFailsWhenStockIsExhausted() throws Exception {
        long campaignId = createCampaign(1);
        submit(campaignId, residentToken, "seckill-req-101");
        String secondResidentToken = loginResident("resident-lisi");

        MvcResult second = submit(campaignId, secondResidentToken, "seckill-req-102");
        JsonNode data = read(second).path("data");

        assertThat(data.path("status").asText()).isEqualTo("FAILED");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT fail_reason FROM coupon_seckill_order WHERE order_no = ?",
                String.class,
                data.path("orderNo").asText())).isEqualTo("库存不足");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM coupon_instance WHERE source_type = 'SECKILL' AND template_id = 90001",
                Integer.class)).isEqualTo(1);
    }

    private MvcResult submit(long campaignId, String token, String requestId) throws Exception {
        return mockMvc.perform(post("/api/v1/coupons/seckill/" + campaignId + "/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requestId": "%s"
                                }
                                """.formatted(requestId)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private long createCampaign(int stock) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update("""
                        INSERT INTO coupon_seckill_campaign(campaign_code, template_id, title, total_stock, available_stock, per_user_limit, start_at, end_at, status)
                        VALUES (?, 90001, '限时优惠券抢购', ?, ?, 1, ?, ?, 1)
                        """,
                "SECKILL-" + System.nanoTime(),
                stock,
                stock,
                now.minusMinutes(1),
                now.plusMinutes(30));
        return jdbcTemplate.queryForObject("SELECT id FROM coupon_seckill_campaign ORDER BY id DESC LIMIT 1", Long.class);
    }

    private long createCampaignByApi(int stock) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/coupons/seckill-campaigns")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "campaignCode": "SECKILL-%s",
                                  "templateId": 90001,
                                  "title": "限时优惠券抢购",
                                  "totalStock": %d,
                                  "perUserLimit": 1,
                                  "startAt": "%s",
                                  "endAt": "%s",
                                  "status": 1
                                }
                                """.formatted(
                                System.nanoTime(),
                                stock,
                                LocalDateTime.now().minusMinutes(1).withNano(0).toString().replace('T', ' '),
                                LocalDateTime.now().plusMinutes(30).withNano(0).toString().replace('T', ' '))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.availableStock").value(stock))
                .andReturn();
        return read(result).path("data").path("id").asLong();
    }
}
