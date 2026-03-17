package com.wuye.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    protected String adminToken;
    protected String residentToken;
    protected String agentToken;

    @BeforeEach
    void initTokens() throws Exception {
        resetDynamicData();
        adminToken = loginAdmin();
        residentToken = loginResident("resident-zhangsan");
        agentToken = loginResident("agent-a");
    }

    protected void resetDynamicData() {
        jdbcTemplate.update("DELETE FROM dunning_log");
        jdbcTemplate.update("DELETE FROM dunning_task");
        jdbcTemplate.update("DELETE FROM invoice_application");
        jdbcTemplate.update("DELETE FROM payment_voucher");
        jdbcTemplate.update("DELETE FROM water_usage_alert");
        jdbcTemplate.update("DELETE FROM coupon_redemption");
        jdbcTemplate.update("DELETE FROM pay_transaction");
        jdbcTemplate.update("DELETE FROM pay_order");
        jdbcTemplate.update("UPDATE coupon_instance SET status = 'NEW', source_ref_no = NULL, owner_account_id = CASE WHEN id = 92001 THEN 10001 ELSE owner_account_id END WHERE id >= 92001");
        jdbcTemplate.update("DELETE FROM coupon_instance WHERE id > 92001");
        jdbcTemplate.update("DELETE FROM coupon_issue_rule WHERE id > 91001");
        jdbcTemplate.update("DELETE FROM coupon_template WHERE id > 90002");
        jdbcTemplate.update("DELETE FROM export_job");
        jdbcTemplate.update("DELETE FROM import_row_error");
        jdbcTemplate.update("DELETE FROM import_batch");
        jdbcTemplate.update("DELETE FROM bill_line");
        jdbcTemplate.update("DELETE FROM bill");
        jdbcTemplate.update("DELETE FROM water_meter_reading");
        jdbcTemplate.update("DELETE FROM water_meter");
        jdbcTemplate.update("DELETE FROM fee_rule_water_tier");
        jdbcTemplate.update("DELETE FROM fee_rule");
        jdbcTemplate.update("UPDATE account_room SET status = 'ACTIVE', bind_source = 'IMPORT', confirmed_at = CURRENT_TIMESTAMP WHERE id IN (40001, 40002)");
    }

    protected String loginAdmin() throws Exception {
        String body = """
                {
                  "username": "admin",
                  "password": "123456"
                }
                """;
        MvcResult result = mockMvc.perform(post("/api/v1/admin/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).path("data").path("accessToken").asText();
    }

    protected String loginResident(String code) throws Exception {
        String body = """
                {
                  "code": "%s",
                  "nickname": "测试住户"
                }
                """.formatted(code);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).path("data").path("accessToken").asText();
    }

    protected JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
