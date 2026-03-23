package com.wuye.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.sql.SQLException;

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
        executeIgnoringMissingTable("DELETE FROM audit_log");
        executeIgnoringMissingTable("DELETE FROM dunning_log");
        executeIgnoringMissingTable("DELETE FROM dunning_task");
        executeIgnoringMissingTable("DELETE FROM invoice_application");
        executeIgnoringMissingTable("DELETE FROM payment_voucher");
        executeIgnoringMissingTable("DELETE FROM water_usage_alert");
        executeIgnoringMissingTable("DELETE FROM coupon_redemption");
        executeIgnoringMissingTable("DELETE FROM coupon_exchange_record");
        executeIgnoringMissingTable("DELETE FROM pay_transaction");
        executeIgnoringMissingTable("DELETE FROM pay_order_bill_cover");
        executeIgnoringMissingTable("DELETE FROM pay_order");
        executeIgnoringMissingTable("UPDATE coupon_instance SET status = 'NEW', source_ref_no = NULL, owner_account_id = CASE WHEN id = 92001 THEN 10001 ELSE owner_account_id END WHERE id >= 92001");
        executeIgnoringMissingTable("DELETE FROM coupon_instance WHERE id > 92001");
        executeIgnoringMissingTable("DELETE FROM coupon_issue_rule WHERE id > 91001");
        executeIgnoringMissingTable("DELETE FROM coupon_template WHERE id > 90002");
        executeIgnoringMissingTable("DELETE FROM export_job");
        executeIgnoringMissingTable("DELETE FROM import_row_error");
        executeIgnoringMissingTable("DELETE FROM import_batch");
        executeIgnoringMissingTable("DELETE FROM bill_line");
        executeIgnoringMissingTable("DELETE FROM bill");
        executeIgnoringMissingTable("DELETE FROM water_meter_reading");
        executeIgnoringMissingTable("DELETE FROM water_meter");
        executeIgnoringMissingTable("DELETE FROM fee_rule_water_tier");
        executeIgnoringMissingTable("DELETE FROM fee_rule");
        executeIgnoringMissingTable("DELETE FROM group_room WHERE room_id > 1002");
        executeIgnoringMissingTable("DELETE FROM account_room WHERE id > 40002");
        executeIgnoringMissingTable("DELETE FROM account_room WHERE room_id > 1002");
        executeIgnoringMissingTable("DELETE FROM room WHERE id > 1002");
        executeIgnoringMissingTable("DELETE FROM room_type");
        executeIgnoringMissingTable("DELETE FROM community WHERE id > 100");
        executeIgnoringMissingTable("DELETE FROM account_identity WHERE id > 30003");
        executeIgnoringMissingTable("DELETE FROM account WHERE id > 30001");
        executeIgnoringMissingTable("DELETE FROM ai_runtime_config");
        executeIgnoringMissingTable("INSERT INTO ai_runtime_config(id, enabled, api_base_url, provider, model, api_key_ciphertext, timeout_ms, max_tokens, temperature) VALUES (1, 0, 'https://api.openai.com/v1', 'openai', 'gpt-4o-mini', NULL, 30000, 4096, 0.20)");
        executeIgnoringMissingTable("UPDATE account SET status = 1, token_invalid_before = NULL WHERE id IN (10001, 10002, 20001, 30001)");
        executeIgnoringMissingTable("UPDATE agent_group SET status = 1 WHERE id = 80001");
        executeIgnoringMissingTable("UPDATE account_room SET status = 'ACTIVE', bind_source = 'IMPORT', confirmed_at = CURRENT_TIMESTAMP WHERE id IN (40001, 40002)");
    }

    private void executeIgnoringMissingTable(String sql) {
        try {
            jdbcTemplate.execute((StatementCallback<Void>) statement -> {
                statement.execute(sql);
                return null;
            });
        } catch (org.springframework.jdbc.BadSqlGrammarException ex) {
            if (!isMissingTable(ex)) {
                throw ex;
            }
        }
    }

    private boolean isMissingTable(org.springframework.jdbc.BadSqlGrammarException ex) {
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof SQLException sqlException) {
                if (sqlException.getErrorCode() == 42102 || sqlException.getErrorCode() == 1146) {
                    return true;
                }
                if ("42S02".equals(sqlException.getSQLState())) {
                    return true;
                }
            }
            cause = cause.getCause();
        }
        return false;
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
