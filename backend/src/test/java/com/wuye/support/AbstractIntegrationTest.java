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

    @BeforeEach
    void initTokens() throws Exception {
        resetDynamicData();
        adminToken = loginAdmin();
        residentToken = loginResident("resident-zhangsan");
    }

    protected void resetDynamicData() {
        jdbcTemplate.update("DELETE FROM pay_transaction");
        jdbcTemplate.update("DELETE FROM pay_order");
        jdbcTemplate.update("DELETE FROM bill_line");
        jdbcTemplate.update("DELETE FROM bill");
        jdbcTemplate.update("DELETE FROM water_meter_reading");
        jdbcTemplate.update("DELETE FROM water_meter");
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
