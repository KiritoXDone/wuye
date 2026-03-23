package com.wuye;

import com.fasterxml.jackson.databind.JsonNode;
import com.wuye.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthAndResidentRoomIntegrationTest extends AbstractIntegrationTest {

    @Test
    void wechatLoginRefreshProfileAndLogoutAreAvailable() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "resident-lisi",
                                  "nickname": "李四"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountId").value(10002))
                .andExpect(jsonPath("$.data.accountType").value("RESIDENT"))
                .andExpect(jsonPath("$.data.productRole").value("USER"))
                .andExpect(jsonPath("$.data.roles[0]").value("USER"))
                .andReturn();

        JsonNode loginJson = read(loginResult).path("data");
        String accessToken = loginJson.path("accessToken").asText();
        String refreshToken = loginJson.path("refreshToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        mockMvc.perform(get("/api/v1/me/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountId").value(10002))
                .andExpect(jsonPath("$.data.accountType").value("RESIDENT"))
                .andExpect(jsonPath("$.data.productRole").value("USER"))
                .andExpect(jsonPath("$.data.roles[0]").value("USER"));

        MvcResult refreshResult = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountId").value(10002))
                .andExpect(jsonPath("$.data.accountType").value("RESIDENT"))
                .andReturn();

        String refreshedAccessToken = read(refreshResult).path("data").path("accessToken").asText();
        assertThat(refreshedAccessToken).isNotBlank();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + refreshedAccessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));
    }

    @Test
    void logoutImmediatelyRevokesIssuedAccessAndRefreshTokens() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "resident-lisi",
                                  "nickname": "李四"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = read(loginResult).path("data");
        String accessToken = loginJson.path("accessToken").asText();
        String refreshToken = loginJson.path("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        mockMvc.perform(get("/api/v1/me/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void disabledResidentAccountCannotKeepUsingOldTokens() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "resident-lisi",
                                  "nickname": "李四"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginJson = read(loginResult).path("data");
        String accessToken = loginJson.path("accessToken").asText();
        String refreshToken = loginJson.path("refreshToken").asText();

        jdbcTemplate.update("UPDATE account SET status = 0, token_invalid_before = CURRENT_TIMESTAMP WHERE id = 10002");

        mockMvc.perform(get("/api/v1/me/profile")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void residentRoomEndpointsExposeOwnedRoomAndSelectableOptions() throws Exception {
        mockMvc.perform(get("/api/v1/me/rooms")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].roomId").value(1001))
                .andExpect(jsonPath("$.data[0].bindingStatus").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/me/rooms/1001")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roomId").value(1001))
                .andExpect(jsonPath("$.data.roomLabel").value("1-2-301"))
                .andExpect(jsonPath("$.data.bindingStatus").value("ACTIVE"));

        mockMvc.perform(get("/api/v1/me/rooms/options/communities")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].communityId").value(100));

        mockMvc.perform(get("/api/v1/me/rooms/options/buildings")
                        .header("Authorization", "Bearer " + residentToken)
                        .param("communityId", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].buildingNo").value("1"));

        mockMvc.perform(get("/api/v1/me/rooms/options/units")
                        .header("Authorization", "Bearer " + residentToken)
                        .param("communityId", "100")
                        .param("buildingNo", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].unitNo").value("2"));

        mockMvc.perform(get("/api/v1/me/rooms/options/room-numbers")
                        .header("Authorization", "Bearer " + residentToken)
                        .param("communityId", "100")
                        .param("buildingNo", "1")
                        .param("unitNo", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].roomId").value(1001))
                .andExpect(jsonPath("$.data[1].roomId").value(1002));
    }

    @Test
    void residentCanApplyBindingAndQueryRoomBills() throws Exception {
        String lisiToken = loginResident("resident-lisi");
        createPropertyFeeRule(2031);
        generatePropertyBill(2031);

        mockMvc.perform(post("/api/v1/me/rooms")
                        .header("Authorization", "Bearer " + lisiToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": 1002
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roomId").value(1002))
                .andExpect(jsonPath("$.data.bindingStatus").value("ACTIVE"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM account_room WHERE account_id = 10002 AND room_id = 1002 AND status = 'ACTIVE' AND bind_source = 'SELF'",
                Integer.class)).isEqualTo(1);

        MvcResult roomBillsResult = mockMvc.perform(get("/api/v1/me/rooms/1002/bills")
                        .header("Authorization", "Bearer " + lisiToken)
                        .param("status", "ISSUED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].roomId").value(1002))
                .andExpect(jsonPath("$.data.list[0].feeType").value("PROPERTY"))
                .andExpect(jsonPath("$.data.list[0].cycleType").value("YEAR"))
                .andExpect(jsonPath("$.data.list[0].status").value("ISSUED"))
                .andReturn();

        long billId = read(roomBillsResult).path("data").path("list").get(0).path("billId").asLong();
        mockMvc.perform(get("/api/v1/bills/" + billId)
                        .header("Authorization", "Bearer " + lisiToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.billId").value(billId))
                .andExpect(jsonPath("$.data.roomId").value(1002))
                .andExpect(jsonPath("$.data.feeType").value("PROPERTY"))
                .andExpect(jsonPath("$.data.cycleType").value("YEAR"))
                .andExpect(jsonPath("$.data.status").value("ISSUED"));
    }

    @Test
    void residentCanConfirmPendingBindingAndLosesAccessAfterUnbind() throws Exception {
        String lisiToken = loginResident("resident-lisi");
        jdbcTemplate.update("""
                UPDATE account_room
                SET status = 'PENDING',
                    bind_source = 'SELF',
                    confirmed_at = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = 40002
                """);

        mockMvc.perform(post("/api/v1/me/rooms/1001/confirm")
                        .header("Authorization", "Bearer " + lisiToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.roomId").value(1001))
                .andExpect(jsonPath("$.data.bindingStatus").value("ACTIVE"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM account_room WHERE id = 40002",
                String.class)).isEqualTo("ACTIVE");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM account_room WHERE id = 40002 AND confirmed_at IS NOT NULL",
                Integer.class)).isEqualTo(1);

        mockMvc.perform(post("/api/v1/me/rooms/1001/unbind")
                        .header("Authorization", "Bearer " + lisiToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("0"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM account_room WHERE id = 40002",
                String.class)).isEqualTo("INACTIVE");

        mockMvc.perform(get("/api/v1/me/rooms")
                        .header("Authorization", "Bearer " + lisiToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].bindingStatus").value("INACTIVE"));

        mockMvc.perform(get("/api/v1/me/rooms/1001")
                        .header("Authorization", "Bearer " + lisiToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private void createPropertyFeeRule(int year) throws Exception {
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
                                  "remark": "resident room api test"
                                }
                                """.formatted(year, year)))
                .andExpect(status().isOk());
    }

    private void generatePropertyBill(int year) throws Exception {
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
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generatedCount").value(2));
    }
}
