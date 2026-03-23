package com.wuye;

import com.fasterxml.jackson.databind.JsonNode;
import com.wuye.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class P3FlowIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private Environment environment;

    @Test
    void adminAuditLogsTrackThirdStageBackendEnhancements() throws Exception {
        mockMvc.perform(post("/api/v1/admin/fee-rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "feeType": "PROPERTY",
                                  "unitPrice": 2.80,
                                  "cycleType": "YEAR",
                                  "effectiveFrom": "2026-03-01",
                                  "effectiveTo": "2026-12-31",
                                  "remark": "P3 审计规则"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").isNumber());

        mockMvc.perform(post("/api/v1/admin/coupon-templates")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("X-Forwarded-For", "10.20.30.40")
                        .header("User-Agent", "P3FlowIntegrationTest/1.0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "templateCode": "PAY-AUDIT-20",
                                  "type": "PAYMENT",
                                  "feeType": "PROPERTY",
                                  "name": "审计测试券模板",
                                  "discountMode": "FIXED",
                                  "valueAmount": 20.00,
                                  "thresholdAmount": 100.00,
                                  "validFrom": "2026-03-01 00:00:00",
                                  "validTo": "2026-12-31 23:59:59"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateCode").value("PAY-AUDIT-20"));

        mockMvc.perform(post("/api/v1/admin/agent-groups")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "agentCode": "AGENT-A",
                                  "groupCode": "G-COMM001-1-2",
                                  "permission": "MANAGE",
                                  "status": 1
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupCode").value("G-COMM001-1-2"));

        Path importRoot = Path.of(environment.getProperty("app.import-export.import-dir"));
        Files.createDirectories(importRoot);
        Path importFile = Files.createTempFile(importRoot, "wuye-audit-import-", ".csv");
        Files.writeString(importFile, """
                bill_no,fee_type,period_year,period_month,community_code,building_no,unit_no,room_no,group_code,amount_due,due_date,remark
                B-IMP-AUDIT-001,PROPERTY,2026,,COMM-001,1,2,302,G-COMM001-1-2,220.00,2026-12-31,审计导入测试
                """, StandardCharsets.UTF_8);

        MvcResult importResult = mockMvc.perform(post("/api/v1/admin/imports/bills")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fileUrl": "%s"
                                }
                                """.formatted(importFile.toString().replace("\\", "\\\\"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andReturn();

        long importBatchId = read(importResult).path("data").path("id").asLong();
        String importBatchNo = read(importResult).path("data").path("batchNo").asText();

        mockMvc.perform(get("/api/v1/admin/imports/" + importBatchId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(importBatchId))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.successCount").value(1))
                .andExpect(jsonPath("$.data.failCount").value(0));

        mockMvc.perform(get("/api/v1/admin/imports/" + importBatchId + "/errors")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));

        MvcResult exportResult = mockMvc.perform(post("/api/v1/admin/exports/bills")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "periodYear": 2026,
                                  "feeType": "PROPERTY",
                                  "status": "ISSUED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.fileUrl").isString())
                .andReturn();

        long exportJobId = read(exportResult).path("data").path("id").asLong();

        mockMvc.perform(get("/api/v1/admin/exports/" + exportJobId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(exportJobId))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.fileUrl").isString());

        mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("pageNo", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(5));

        MvcResult couponAuditResult = mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("bizType", "COUPON")
                        .param("bizId", "PAY-AUDIT-20")
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].action").value("CREATE"))
                .andExpect(jsonPath("$.data.list[0].ip").value("10.20.30.40"))
                .andExpect(jsonPath("$.data.list[0].userAgent").value("P3FlowIntegrationTest/1.0"))
                .andReturn();

        JsonNode couponAuditJson = read(couponAuditResult);
        assertThat(couponAuditJson.path("data").path("list").get(0).path("detailJson").asText()).contains("PAY-AUDIT-20");

        MvcResult importAuditResult = mockMvc.perform(get("/api/v1/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("bizType", "IMPORT")
                        .param("bizId", importBatchNo)
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].action").value("IMPORT"))
                .andReturn();

        JsonNode importAuditJson = read(importAuditResult);
        assertThat(importAuditJson.path("data").path("list").get(0).path("detailJson").asText()).contains("SUCCESS");
    }
}
