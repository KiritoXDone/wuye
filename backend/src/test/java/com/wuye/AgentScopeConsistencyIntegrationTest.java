package com.wuye;

import com.fasterxml.jackson.databind.JsonNode;
import com.wuye.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AgentScopeConsistencyIntegrationTest extends AbstractIntegrationTest {

    @Test
    void authorizedAgentScopeUsesConsistentOrgUnitMetadata() throws Exception {
        createFeeRule("PROPERTY", "2.5000");

        mockMvc.perform(post("/api/v1/admin/bills/generate/property")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "year": 2026,
                                  "month": 6,
                                  "overwriteStrategy": "SKIP"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult groupsResult = mockMvc.perform(get("/api/v1/agent/groups")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].groupId").value(5001))
                .andExpect(jsonPath("$.data[0].orgUnitId").value(10002))
                .andExpect(jsonPath("$.data[0].orgUnitName").value("阳光花园服务中心"))
                .andExpect(jsonPath("$.data[0].tenantCode").value("TENANT-DEMO"))
                .andReturn();

        JsonNode firstGroup = read(groupsResult).path("data").get(0);

        mockMvc.perform(get("/api/v1/agent/reports/monthly")
                        .header("Authorization", "Bearer " + agentToken)
                        .param("groupId", "5001")
                        .param("periodYear", "2026")
                        .param("periodMonth", "6"))
                .andExpect(status().isOk())
                .andReturn();

        MvcResult reportResult = mockMvc.perform(get("/api/v1/agent/reports/monthly")
                        .header("Authorization", "Bearer " + agentToken)
                        .param("groupId", "5001")
                        .param("periodYear", "2026")
                        .param("periodMonth", "6"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupId").value(5001))
                .andReturn();

        JsonNode report = read(reportResult).path("data");
        assertThat(report.path("orgUnitId").asLong()).isEqualTo(firstGroup.path("orgUnitId").asLong());
        assertThat(report.path("orgUnitName").asText()).isEqualTo(firstGroup.path("orgUnitName").asText());
        assertThat(report.path("tenantCode").asText()).isEqualTo(firstGroup.path("tenantCode").asText());

        mockMvc.perform(get("/api/v1/agent/reports/monthly")
                        .header("Authorization", "Bearer " + agentToken)
                        .param("groupId", "9999")
                        .param("periodYear", "2026")
                        .param("periodMonth", "6"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void disabledAgentGroupImmediatelyRemovesOldTokenScope() throws Exception {
        mockMvc.perform(get("/api/v1/agent/groups")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].groupId").value(5001));

        jdbcTemplate.update("UPDATE agent_group SET status = 0 WHERE id = 80001");

        mockMvc.perform(get("/api/v1/agent/groups")
                        .header("Authorization", "Bearer " + agentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        mockMvc.perform(get("/api/v1/agent/reports/monthly")
                        .header("Authorization", "Bearer " + agentToken)
                        .param("groupId", "5001")
                        .param("periodYear", "2026")
                        .param("periodMonth", "6"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
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
                                  "remark": "scope test"
                                }
                                """.formatted(feeType, unitPrice, "PROPERTY".equals(feeType) ? "YEAR" : "MONTH")))
                .andExpect(status().isOk());
    }
}
