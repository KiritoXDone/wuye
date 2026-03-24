package com.wuye;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.ai.service.AiRuntimeSettings;
import com.wuye.ai.service.OaiChatClient;
import com.wuye.common.config.AppAiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.StatementCallback;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class BuiltInAgentCommandIntegrationTest {

    private static final String COMMUNITY_NAME = "阳光花园";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private AppAiProperties appAiProperties;

    @Autowired
    private ScriptedOaiChatClient scriptedOaiChatClient;

    @LocalServerPort
    private int port;

    private String adminToken;
    private String residentToken;

    @BeforeEach
    void setUp() throws Exception {
        resetDynamicData();
        appAiProperties.getRuntime().setBackendBaseUrl("http://127.0.0.1:" + port);
        adminToken = loginAdmin();
        residentToken = loginResident("resident-zhangsan");
    }

    @Test
    void residentBillSummaryCanBeQueriedFromAgentRoute() throws Exception {
        createPropertyFeeRule(2026);
        generatePropertyBill(2026);

        mockMvc.perform(get("/api/v1/ai/agent/me/bill-summary")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accountId").value(10001))
                .andExpect(jsonPath("$.data.roomCount").value(1))
                .andExpect(jsonPath("$.data.activeRoomCount").value(1))
                .andExpect(jsonPath("$.data.issuedBillCount").value(1))
                .andExpect(jsonPath("$.data.unpaidBillCount").value(1))
                .andExpect(jsonPath("$.data.recentBills[0].feeType").value("PROPERTY"));
    }

    @Test
    void adminBillStatsCanBeQueriedFromAgentRoute() throws Exception {
        createPropertyFeeRule(2026);
        generatePropertyBill(2026);
        createWaterFeeRule(2026);
        createWaterMeter(1001L);

        mockAgentCommand("""
                {
                  "action": "WATER_READING_CREATE",
                  "arguments": {
                    "communityName": "%s",
                    "buildingNo": "1",
                    "unitNo": "2",
                    "roomNo": "301",
                    "year": 2026,
                    "month": 8,
                    "prevReading": 12.000,
                    "currReading": 18.500,
                    "readAt": "2026-08-15T10:00:00",
                    "remark": "admin stats seed"
                  },
                  "warnings": [],
                  "riskLevel": "L2",
                  "summary": "录入抄表"
                }
                """.formatted(COMMUNITY_NAME));
        previewCommand(adminToken, "录入 2026 年 8 月抄表");

        mockMvc.perform(get("/api/v1/ai/agent/admin/bill-stats")
                        .param("periodYear", "2026")
                        .param("periodMonth", "8")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.periodYear").value(2026))
                .andExpect(jsonPath("$.data.periodMonth").value(8))
                .andExpect(jsonPath("$.data.summary.totalCount").value(2))
                .andExpect(jsonPath("$.data.propertyYearly.totalCount").value(2))
                .andExpect(jsonPath("$.data.waterMonthly.totalCount").value(1));
    }

    @Test
    void roomCreateCanBeExecutedThroughAgentPreview() throws Exception {
        mockAgentCommand("""
                {
                  "action": "ROOM_CREATE",
                  "arguments": {
                    "communityName": "%s",
                    "buildingNo": "9",
                    "unitNo": "1",
                    "roomNo": "901",
                    "areaM2": 108.88
                  },
                  "warnings": [],
                  "riskLevel": "L2",
                  "summary": "创建房间"
                }
                """.formatted(COMMUNITY_NAME));

        JsonNode preview = previewCommand(adminToken, "在阳光花园新增 9 栋 1 单元 901");
        assertThat(preview.path("resolvedContext").path("communityId").asLong()).isEqualTo(100L);

        JsonNode execution = fetchCommand(adminToken, preview.path("commandId").asText());
        long createdRoomId = execution.path("result").path("id").asLong();

        assertThat(createdRoomId).isPositive();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM room WHERE id = ? AND community_id = 100 AND building_no = '9' AND unit_no = '1' AND room_no = '901' AND status = 1",
                Integer.class,
                createdRoomId)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM water_meter WHERE room_id = ?",
                Integer.class,
                createdRoomId)).isEqualTo(1);
    }

    @Test
    void roomDisableCanBeExecutedThroughAgentPreview() throws Exception {
        long roomId = createAdminRoom("9", "2", "902", new BigDecimal("99.90"));

        mockAgentCommand("""
                {
                  "action": "ROOM_DISABLE",
                  "arguments": {
                    "communityName": "%s",
                    "buildingNo": "9",
                    "unitNo": "2",
                    "roomNo": "902"
                  },
                  "warnings": [],
                  "riskLevel": "L3",
                  "summary": "停用房间"
                }
                """.formatted(COMMUNITY_NAME));

        JsonNode preview = previewCommand(adminToken, "停用阳光花园 9 栋 2 单元 902");
        assertThat(preview.path("resolvedContext").path("roomId").asLong()).isEqualTo(roomId);

        JsonNode execution = fetchCommand(adminToken, preview.path("commandId").asText());
        assertThat(execution.path("result").path("roomId").asLong()).isEqualTo(roomId);
        assertThat(execution.path("result").path("mode").asText()).isEqualTo("DISABLE");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM room WHERE id = ?",
                Integer.class,
                roomId)).isEqualTo(0);
    }

    @Test
    void billDetailCanBeQueriedThroughAgentPreview() throws Exception {
        createPropertyFeeRule(2026);
        generatePropertyBill(2026);
        long billId = findBillId("PROPERTY", 2026, null);

        mockAgentCommand("""
                {
                  "action": "BILL_DETAIL",
                  "arguments": {
                    "billId": %s
                  },
                  "warnings": [],
                  "riskLevel": "L1",
                  "summary": "查询账单详情"
                }
                """.formatted(billId));

        JsonNode preview = previewCommand(residentToken, "查看账单详情");
        JsonNode execution = fetchCommand(residentToken, preview.path("commandId").asText());

        assertThat(execution.path("result").path("billId").asLong()).isEqualTo(billId);
        assertThat(execution.path("result").path("feeType").asText()).isEqualTo("PROPERTY");
        assertThat(execution.path("result").path("cycleType").asText()).isEqualTo("YEAR");
    }

    @Test
    void billListByRoomCanBeQueriedThroughAgentPreview() throws Exception {
        createPropertyFeeRule(2026);
        generatePropertyBill(2026);

        mockAgentCommand("""
                {
                  "action": "BILL_LIST_BY_ROOM",
                  "arguments": {
                    "communityName": "%s",
                    "buildingNo": "1",
                    "unitNo": "2",
                    "roomNo": "301",
                    "status": "ISSUED"
                  },
                  "warnings": [],
                  "riskLevel": "L1",
                  "summary": "按房间查询账单"
                }
                """.formatted(COMMUNITY_NAME));

        JsonNode preview = previewCommand(adminToken, "查询阳光花园 1 栋 2 单元 301 的未缴账单");
        JsonNode execution = fetchCommand(adminToken, preview.path("commandId").asText());

        assertThat(execution.path("result").path("list").isArray()).isTrue();
        assertThat(execution.path("result").path("total").asLong()).isEqualTo(1L);
        assertThat(execution.path("result").path("list").get(0).path("roomId").asLong()).isEqualTo(1001L);
        assertThat(execution.path("result").path("list").get(0).path("status").asText()).isEqualTo("ISSUED");
    }

    @Test
    void paymentCreateCanBeExecutedThroughAgentPreview() throws Exception {
        createPropertyFeeRule(2026);
        generatePropertyBill(2026);
        long billId = findBillId("PROPERTY", 2026, null);

        mockAgentCommand("""
                {
                  "action": "PAYMENT_CREATE",
                  "arguments": {
                    "billId": %s,
                    "channel": "WECHAT",
                    "annualPayment": false
                  },
                  "warnings": [],
                  "riskLevel": "L2",
                  "summary": "创建支付单"
                }
                """.formatted(billId));

        JsonNode preview = previewCommand(residentToken, "为账单创建支付单");
        JsonNode execution = fetchCommand(residentToken, preview.path("commandId").asText());
        String payOrderNo = execution.path("result").path("payOrderNo").asText();

        assertThat(payOrderNo).isNotBlank();
        assertThat(execution.path("result").path("channel").asText()).isEqualTo("WECHAT");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM pay_order WHERE pay_order_no = ?",
                Integer.class,
                payOrderNo)).isEqualTo(1);
    }

    @Test
    void paymentQueryCanBeExecutedThroughAgentPreview() throws Exception {
        createPropertyFeeRule(2026);
        generatePropertyBill(2026);
        long billId = findBillId("PROPERTY", 2026, null);
        String payOrderNo = createPaymentDirect(billId, "query-payment-001");

        mockAgentCommand("""
                {
                  "action": "PAYMENT_QUERY",
                  "arguments": {
                    "payOrderNo": "%s"
                  },
                  "warnings": [],
                  "riskLevel": "L1",
                  "summary": "查询支付单"
                }
                """.formatted(payOrderNo));

        JsonNode preview = previewCommand(residentToken, "查询支付单状态");
        JsonNode execution = fetchCommand(residentToken, preview.path("commandId").asText());

        assertThat(execution.path("result").path("payOrderNo").asText()).isEqualTo(payOrderNo);
        assertThat(execution.path("result").path("status").asText()).isEqualTo("PAYING");
        assertThat(execution.path("result").path("billId").asLong()).isEqualTo(billId);
    }

    @Test
    void waterReadingCreateCanBeExecutedThroughAgentPreview() throws Exception {
        createWaterFeeRule(2026);
        createWaterMeter(1001L);

        mockAgentCommand("""
                {
                  "action": "WATER_READING_CREATE",
                  "arguments": {
                    "communityName": "%s",
                    "buildingNo": "1",
                    "unitNo": "2",
                    "roomNo": "301",
                    "year": 2026,
                    "month": 8,
                    "prevReading": 10.000,
                    "currReading": 16.000,
                    "readAt": "2026-08-15T10:00:00",
                    "remark": "agent water reading"
                  },
                  "warnings": [],
                  "riskLevel": "L2",
                  "summary": "录入抄表"
                }
                """.formatted(COMMUNITY_NAME));

        JsonNode preview = previewCommand(adminToken, "录入阳光花园 1 栋 2 单元 301 2026 年 8 月水表");
        assertThat(preview.path("resolvedContext").path("roomId").asLong()).isEqualTo(1001L);

        JsonNode execution = fetchCommand(adminToken, preview.path("commandId").asText());
        long billId = execution.path("result").path("billId").asLong();

        assertThat(execution.path("result").path("roomId").asLong()).isEqualTo(1001L);
        assertThat(execution.path("result").path("usageAmount").decimalValue()).isEqualByComparingTo("6.000");
        assertThat(billId).isPositive();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM water_meter_reading WHERE room_id = 1001 AND period_year = 2026 AND period_month = 8",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT fee_type FROM bill WHERE id = ?",
                String.class,
                billId)).isEqualTo("WATER");
    }

    @Test
    void highRiskPreviewRequiresConfirmBeforeExecution() throws Exception {
        mockAgentCommand("""
                {
                  "action": "ROOM_CREATE",
                  "arguments": {
                    "communityName": "%s",
                    "buildingNo": "9",
                    "unitNo": "3",
                    "roomNo": "903",
                    "areaM2": 88.80
                  },
                  "warnings": [],
                  "riskLevel": "L4",
                  "summary": "创建高风险房间"
                }
                """.formatted(COMMUNITY_NAME));

        JsonNode preview = previewCommandExpectingConfirmation(adminToken, "新增 9 栋 3 单元 903");
        String commandId = preview.path("commandId").asText();
        String confirmationToken = preview.path("confirmationToken").asText();

        assertThat(confirmationToken).isNotBlank();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM room WHERE community_id = 100 AND building_no = '9' AND unit_no = '3' AND room_no = '903'",
                Integer.class)).isEqualTo(0);

        mockMvc.perform(get("/api/v1/ai/agent/commands/" + commandId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_CONFIRMATION"))
                .andExpect(jsonPath("$.data.result").doesNotExist());

        JsonNode execution = confirmCommand(adminToken, confirmationToken);
        assertThat(execution.path("status").asText()).isEqualTo("EXECUTED");
        assertThat(execution.path("action").asText()).isEqualTo("ROOM_CREATE");
        assertThat(execution.path("result").path("id").asLong()).isPositive();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM room WHERE community_id = 100 AND building_no = '9' AND unit_no = '3' AND room_no = '903' AND status = 1",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void conversationChatReplyIsStoredAndListed() throws Exception {
        scriptedOaiChatClient.setNextChat("你好，我可以帮你查询账单、支付和房间信息。");

        MvcResult result = mockMvc.perform(post("/api/v1/ai/agent/conversation")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "你好"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageCount").value(2))
                .andExpect(jsonPath("$.data.messages[0].role").value("user"))
                .andExpect(jsonPath("$.data.messages[1].role").value("assistant"))
                .andExpect(jsonPath("$.data.messages[1].mode").value("CHAT"))
                .andExpect(jsonPath("$.data.messages[1].content").value("你好，我可以帮你查询账单、支付和房间信息。"))
                .andReturn();

        JsonNode conversation = read(result).path("data");
        String sessionId = conversation.path("sessionId").asText();

        mockMvc.perform(get("/api/v1/ai/agent/conversation/sessions")
                        .header("Authorization", "Bearer " + residentToken)
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.list[0].sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.list[0].messageCount").value(2));

        mockMvc.perform(get("/api/v1/ai/agent/conversation/" + sessionId)
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(sessionId))
                .andExpect(jsonPath("$.data.messages[1].content").value("你好，我可以帮你查询账单、支付和房间信息。"));
    }

    @Test
    void conversationRiskyCommandReturnsConfirmationTokenAndCanConfirm() throws Exception {
        mockAgentCommand("""
                {
                  "action": "ROOM_CREATE",
                  "arguments": {
                    "communityName": "%s",
                    "buildingNo": "9",
                    "unitNo": "4",
                    "roomNo": "904",
                    "areaM2": 118.20
                  },
                  "warnings": [],
                  "riskLevel": "L4",
                  "summary": "会话创建房间"
                }
                """.formatted(COMMUNITY_NAME));

        MvcResult result = mockMvc.perform(post("/api/v1/ai/agent/conversation")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "请新增 9 栋 4 单元 904"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageCount").value(2))
                .andExpect(jsonPath("$.data.messages[1].mode").value("ACTION"))
                .andExpect(jsonPath("$.data.messages[1].confirmationRequired").value(true))
                .andExpect(jsonPath("$.data.messages[1].confirmationToken").isString())
                .andReturn();

        JsonNode conversation = read(result).path("data");
        String sessionId = conversation.path("sessionId").asText();
        String confirmationToken = conversation.path("messages").get(1).path("confirmationToken").asText();

        mockMvc.perform(get("/api/v1/ai/agent/conversation/" + sessionId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages[1].confirmationRequired").value(true))
                .andExpect(jsonPath("$.data.messages[1].confirmationToken").value(confirmationToken));

        JsonNode execution = confirmCommand(adminToken, confirmationToken);
        assertThat(execution.path("action").asText()).isEqualTo("ROOM_CREATE");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM room WHERE community_id = 100 AND building_no = '9' AND unit_no = '4' AND room_no = '904' AND status = 1",
                Integer.class)).isEqualTo(1);
    }

    @Test
    void conversationCanGuideAndContinueWhenCommandArgumentsAreMissing() throws Exception {
        mockAgentCommand("""
                {
                  "action": "ROOM_CREATE",
                  "arguments": {
                    "communityName": "%s",
                    "buildingNo": "9",
                    "unitNo": "6",
                    "roomNo": "906"
                  },
                  "warnings": [],
                  "riskLevel": "L2",
                  "summary": "创建房间"
                }
                """.formatted(COMMUNITY_NAME));

        MvcResult firstResult = mockMvc.perform(post("/api/v1/ai/agent/conversation")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "请新增 9 栋 6 单元 906"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageCount").value(2))
                .andExpect(jsonPath("$.data.messages[1].mode").value("ACTION"))
                .andExpect(jsonPath("$.data.messages[1].content").value(org.hamcrest.Matchers.containsString("面积")))
                .andExpect(jsonPath("$.data.messages[1].payload.missingArguments[0]").value("面积"))
                .andExpect(jsonPath("$.data.messages[1].payload.executable").value(false))
                .andReturn();

        JsonNode firstConversation = read(firstResult).path("data");
        String sessionId = firstConversation.path("sessionId").asText();

        mockAgentCommand("""
                {
                  "action": "UNKNOWN",
                  "arguments": {
                    "areaM2": 109.66
                  },
                  "warnings": [],
                  "riskLevel": "L1",
                  "summary": "补充面积"
                }
                """);

        MvcResult secondResult = mockMvc.perform(post("/api/v1/ai/agent/conversation")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId": "%s",
                                  "message": "面积 109.66 平方米"
                                }
                                """.formatted(sessionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messageCount").value(4))
                .andExpect(jsonPath("$.data.messages[3].action").value("ROOM_CREATE"))
                .andExpect(jsonPath("$.data.messages[3].payload.executable").value(true))
                .andExpect(jsonPath("$.data.messages[3].payload.result.id").isNumber())
                .andReturn();

        long createdRoomId = read(secondResult).path("data").path("messages").get(3).path("payload").path("result").path("id").asLong();
        assertThat(createdRoomId).isPositive();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM room WHERE id = ? AND community_id = 100 AND building_no = '9' AND unit_no = '6' AND room_no = '906' AND area_m2 = 109.66 AND status = 1",
                Integer.class,
                createdRoomId)).isEqualTo(1);
    }

    @Test
    void conversationStreamCommandPreviewEmitsSseEventsAndPersistsSession() throws Exception {
        mockAgentCommand("""
                {
                  "action": "ROOM_CREATE",
                  "arguments": {
                    "communityName": "%s",
                    "buildingNo": "9",
                    "unitNo": "5",
                    "roomNo": "905",
                    "areaM2": 109.50
                  },
                  "warnings": [],
                  "riskLevel": "L4",
                  "summary": "流式预览创建房间"
                }
                """.formatted(COMMUNITY_NAME));

        MvcResult asyncResult = mockMvc.perform(post("/api/v1/ai/agent/conversation/stream")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "message": "请新增 9 栋 5 单元 905"
                                }
                                """))
                .andExpect(request().asyncStarted())
                .andReturn();

        String response = waitForSse(asyncResult, "event:done");
        assertThat(asyncResult.getResponse().getContentType()).startsWith(MediaType.TEXT_EVENT_STREAM_VALUE);
        assertThat(response).contains("event:session");
        assertThat(response).contains("event:command-preview");
        assertThat(response).contains("event:done");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM room WHERE community_id = 100 AND building_no = '9' AND unit_no = '5' AND room_no = '905'",
                Integer.class)).isEqualTo(0);

        MvcResult sessionsResult = mockMvc.perform(get("/api/v1/ai/agent/conversation/sessions")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("pageNo", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andReturn();

        String sessionId = read(sessionsResult).path("data").path("list").get(0).path("sessionId").asText();
        mockMvc.perform(get("/api/v1/ai/agent/conversation/" + sessionId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.messages[0].content").value("请新增 9 栋 5 单元 905"))
                .andExpect(jsonPath("$.data.messages[1].mode").value("ACTION"))
                .andExpect(jsonPath("$.data.messages[1].action").value("ROOM_CREATE"))
                .andExpect(jsonPath("$.data.messages[1].confirmationRequired").value(true))
                .andExpect(jsonPath("$.data.messages[1].confirmationToken").isString());
    }

    private void mockAgentCommand(String json) throws Exception {
        scriptedOaiChatClient.setNextJson(objectMapper.readTree(json));
    }

    private JsonNode previewCommand(String token, String prompt) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/ai/agent/commands/preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prompt": "%s"
                                }
                                """.formatted(prompt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmationRequired").value(false))
                .andReturn();
        return read(result).path("data");
    }

    private JsonNode previewCommandExpectingConfirmation(String token, String prompt) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/ai/agent/commands/preview")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prompt": "%s"
                                }
                                """.formatted(prompt)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmationRequired").value(true))
                .andReturn();
        return read(result).path("data");
    }

    private JsonNode fetchCommand(String token, String commandId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/ai/agent/commands/" + commandId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EXECUTED"))
                .andReturn();
        return read(result).path("data");
    }

    private JsonNode confirmCommand(String token, String confirmationToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/ai/agent/commands/confirm")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "confirmationToken": "%s"
                                }
                                """.formatted(confirmationToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("EXECUTED"))
                .andReturn();
        return read(result).path("data");
    }

    private long createAdminRoom(String buildingNo, String unitNo, String roomNo, BigDecimal areaM2) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/rooms")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "buildingNo": "%s",
                                  "unitNo": "%s",
                                  "roomNo": "%s",
                                  "areaM2": %s
                                }
                                """.formatted(buildingNo, unitNo, roomNo, areaM2.toPlainString())))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).path("data").path("id").asLong();
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
                                  "remark": "agent property rule"
                                }
                                """.formatted(year, year)))
                .andExpect(status().isOk());
    }

    private void createWaterFeeRule(int year) throws Exception {
        mockMvc.perform(post("/api/v1/admin/fee-rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "communityId": 100,
                                  "feeType": "WATER",
                                  "unitPrice": 3.2500,
                                  "cycleType": "MONTH",
                                  "effectiveFrom": "%s-01-01",
                                  "effectiveTo": "%s-12-31",
                                  "remark": "agent water rule"
                                }
                                """.formatted(year, year)))
                .andExpect(status().isOk());
    }

    private void createWaterMeter(long roomId) throws Exception {
        mockMvc.perform(post("/api/v1/admin/water-meters")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomId": %s,
                                  "meterNo": "WM-%s",
                                  "installAt": "2025-01-01"
                                }
                                """.formatted(roomId, roomId)))
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

    private long findBillId(String feeType, int periodYear, Integer periodMonth) {
        if (periodMonth == null) {
            return jdbcTemplate.queryForObject(
                    "SELECT id FROM bill WHERE room_id = 1001 AND fee_type = ? AND period_year = ? ORDER BY id DESC LIMIT 1",
                    Long.class,
                    feeType,
                    periodYear);
        }
        return jdbcTemplate.queryForObject(
                "SELECT id FROM bill WHERE room_id = 1001 AND fee_type = ? AND period_year = ? AND period_month = ? ORDER BY id DESC LIMIT 1",
                Long.class,
                feeType,
                periodYear,
                periodMonth);
    }

    private String createPaymentDirect(long billId, String idempotencyKey) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + residentToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "billId": %s,
                                  "channel": "WECHAT",
                                  "idempotencyKey": "%s"
                                }
                                """.formatted(billId, idempotencyKey)))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).path("data").path("payOrderNo").asText();
    }

    private void resetDynamicData() {
        executeIgnoringMissingTable("DELETE FROM ai_agent_conversation_message");
        executeIgnoringMissingTable("DELETE FROM ai_agent_conversation");
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
        executeIgnoringMissingTable("UPDATE room SET status = 1 WHERE id IN (1001, 1002)");
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

    private String loginAdmin() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/admin/auth/login/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).path("data").path("accessToken").asText();
    }

    private String loginResident(String code) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login/wechat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "%s",
                                  "nickname": "agent 集成测试"
                                }
                                """.formatted(code)))
                .andExpect(status().isOk())
                .andReturn();
        return read(result).path("data").path("accessToken").asText();
    }

    private JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private String waitForSse(MvcResult result, String expectedFragment) throws Exception {
        long deadline = System.currentTimeMillis() + 3_000;
        String body = result.getResponse().getContentAsString();
        while (!body.contains(expectedFragment) && System.currentTimeMillis() < deadline) {
            Thread.sleep(50);
            body = result.getResponse().getContentAsString();
        }
        assertThat(body).contains(expectedFragment);
        return body;
    }

    @TestConfiguration
    static class AgentTestConfig {

        @Bean
        @Primary
        ScriptedOaiChatClient scriptedOaiChatClient(ObjectMapper objectMapper) {
            return new ScriptedOaiChatClient(objectMapper);
        }
    }

    static class ScriptedOaiChatClient extends OaiChatClient {

        private JsonNode nextJson;
        private String nextChat;

        ScriptedOaiChatClient(ObjectMapper objectMapper) {
            super(new RestTemplateBuilder(), objectMapper);
        }

        void setNextJson(JsonNode nextJson) {
            this.nextJson = nextJson;
        }

        void setNextChat(String nextChat) {
            this.nextChat = nextChat;
        }

        @Override
        public JsonNode completeJson(AiRuntimeSettings settings, String systemPrompt, String userPrompt) {
            if (nextJson == null) {
                throw new IllegalStateException("nextJson not configured");
            }
            JsonNode response = nextJson;
            nextJson = null;
            return response;
        }

        @Override
        public String completeChat(AiRuntimeSettings settings, List<java.util.Map<String, String>> messages) {
            if (nextChat == null) {
                throw new IllegalStateException("nextChat not configured");
            }
            String response = nextChat;
            nextChat = null;
            return response;
        }
    }
}
