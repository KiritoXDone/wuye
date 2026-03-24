package com.wuye.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.ai.dto.AgentCommandConfirmRequest;
import com.wuye.ai.dto.AgentCommandPreviewRequest;
import com.wuye.ai.dto.AgentConversationListQuery;
import com.wuye.ai.dto.AgentConversationRequest;
import com.wuye.ai.entity.AgentConversationEntity;
import com.wuye.ai.entity.AgentConversationMessageEntity;
import com.wuye.ai.entity.AiRuntimeConfig;
import com.wuye.ai.mapper.AgentConversationMapper;
import com.wuye.ai.mapper.AgentConversationMessageMapper;
import com.wuye.ai.mapper.AiRuntimeConfigMapper;
import com.wuye.ai.vo.AgentAdminBillStatsVO;
import com.wuye.ai.vo.AgentCommandExecutionVO;
import com.wuye.ai.vo.AgentCommandPreviewVO;
import com.wuye.ai.vo.AgentConversationListItemVO;
import com.wuye.ai.vo.AgentConversationMessageVO;
import com.wuye.ai.vo.AgentConversationVO;
import com.wuye.ai.vo.AgentResidentBillSummaryVO;
import com.wuye.audit.service.AuditLogService;
import com.wuye.bill.dto.BillListQuery;
import com.wuye.bill.entity.Bill;
import com.wuye.bill.service.BillQueryService;
import com.wuye.bill.vo.BillDetailVO;
import com.wuye.bill.vo.BillListItemVO;
import com.wuye.common.api.PageResponse;
import com.wuye.common.config.AppAiProperties;
import com.wuye.common.exception.BusinessException;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import com.wuye.common.security.SensitiveConfigCipher;
import com.wuye.payment.dto.PaymentCreateDTO;
import com.wuye.payment.vo.PaymentCreateVO;
import com.wuye.payment.vo.PaymentStatusVO;
import com.wuye.report.service.AdminDashboardService;
import com.wuye.report.service.AdminMonthlyReportService;
import com.wuye.report.vo.AdminDashboardSummaryVO;
import com.wuye.room.dto.AdminRoomCreateDTO;
import com.wuye.room.dto.AdminRoomListQuery;
import com.wuye.room.service.RoomBindingService;
import com.wuye.room.vo.AdminCommunityVO;
import com.wuye.room.vo.AdminRoomVO;
import com.wuye.room.vo.RoomTypeVO;
import com.wuye.room.vo.RoomVO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class BuiltInAgentService {

    private static final String AGENT_PARSE_SYSTEM_PROMPT = """
            你是物业管理系统的指令解析器。请把用户自然语言解析为 JSON。
            只允许返回以下 action 之一：ROOM_CREATE、ROOM_DISABLE、BILL_DETAIL、BILL_LIST_BY_ROOM、PAYMENT_CREATE、PAYMENT_QUERY、WATER_READING_CREATE。
            你必须严格按照当前系统可调用的 /api/v1 接口能力来决定 action 和参数，不要假设存在未接入接口。
            已接入的调用与用途如下：
            - POST /api/v1/admin/rooms：创建房间。需要 communityId、buildingNo、unitNo、roomNo、可选 roomTypeId、areaM2。
            - DELETE /api/v1/admin/rooms/{roomId}：停用房间。若用户给的是小区+楼栋+单元+房号，需要先定位 roomId。
            - GET /api/v1/bills/{billId} 或 GET /api/v1/admin/bills/{billId}：查询账单详情。
            - GET /api/v1/me/rooms/{roomId}/bills?status=...：居民按房间查账单。
            - GET /api/v1/admin/bills?roomId=...&status=...：管理员按房间查账单。
            - POST /api/v1/payments：创建支付单。需要 billId、channel、annualPayment、idempotencyKey。
            - GET /api/v1/payments/{payOrderNo}：查询支付单状态。
            - POST /api/v1/admin/water-readings：录入水表抄表。需要 roomId、year、month、prevReading、currReading、readAt，可选 remark。
            已接入的辅助查询与用途如下：
            - GET /api/v1/admin/communities：通过小区名匹配 communityId。
            - GET /api/v1/admin/room-types?communityId=...：通过户型名匹配 roomTypeId 和 areaM2。
            - GET /api/v1/admin/rooms?communityId=...&buildingNo=...&unitNo=...&roomNo=...：精确定位房间 roomId。
            对于 BILL_LIST_BY_ROOM，如果用户说“未缴”，应优先理解为 status=ISSUED。
            对于 WATER_READING_CREATE，如果用户表达的是“录入抄表/提交抄表/新增抄表”，应优先识别为该 action。
            必须返回 JSON 对象，字段至少包含：action、summary、riskLevel、arguments、warnings。
            riskLevel 只允许：L1、L2、L3、L4。
            arguments 是对象。
            当 action=ROOM_CREATE 时，尽量提取：communityName、communityCode、buildingNo、unitNo、roomNo、roomTypeName、areaM2。
            当 action=ROOM_DISABLE 时，尽量提取：roomId、communityName、buildingNo、unitNo、roomNo。
            当 action=BILL_DETAIL 时，尽量提取：billId。
            当 action=BILL_LIST_BY_ROOM 时，尽量提取：roomId、communityName、buildingNo、unitNo、roomNo、status。
            当 action=PAYMENT_CREATE 时，尽量提取：billId、channel、annualPayment。
            当 action=PAYMENT_QUERY 时，尽量提取：payOrderNo。
            当 action=WATER_READING_CREATE 时，尽量提取：communityName、buildingNo、unitNo、roomNo、year、month、prevReading、currReading、readAt、remark。
            如果用户提到强删/硬删除/物理删除，统一改写为停用语义，不要输出物理删除动作。
            如果无法确定，action 返回 UNKNOWN，并在 warnings 中说明缺少什么。
            """;

    private static final String AGENT_CHAT_SYSTEM_PROMPT = """
            你是物业管理系统内置助手，回答要像正常智能助手一样自然、连续、简洁。
            你可以进行普通聊天、解释系统业务规则、帮助理解账单和支付流程，也可以引导用户改成明确的业务操作指令。
            必须遵守以下事实：
            1. 账单主体始终是房间，不是账号。
            2. 物业费按面积计费、按年开单、按年缴纳。
            3. 水费按月抄表，录入后立即出账。
            4. 不提供硬删除，涉及删除语义时只能表达为停用并保留历史数据。
            5. 不要编造系统中不存在的操作结果；如果用户是在闲聊，就正常回复；如果用户的问题像具体查询/创建/停用/支付动作，但信息不足，可以直接指出还缺什么。
            6. 默认使用中文回答。
            """;

    private final AccessGuard accessGuard;
    private final RoomBindingService roomBindingService;
    private final BillQueryService billQueryService;
    private final AdminDashboardService adminDashboardService;
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final AdminMonthlyReportService adminMonthlyReportService;
    private final AiRuntimeConfigMapper aiRuntimeConfigMapper;
    private final AppAiProperties appAiProperties;
    private final SensitiveConfigCipher sensitiveConfigCipher;
    private final OaiChatClient oaiChatClient;
    private final ApiV1AgentClient apiV1AgentClient;
    private final AuditLogService auditLogService;
    private final AgentConversationMapper agentConversationMapper;
    private final AgentConversationMessageMapper agentConversationMessageMapper;
    private final AgentConversationCacheService agentConversationCacheService;
    private final ObjectMapper objectMapper;
    private final Map<String, AgentCommandSession> sessions = new ConcurrentHashMap<>();

    public BuiltInAgentService(AccessGuard accessGuard,
                               RoomBindingService roomBindingService,
                               BillQueryService billQueryService,
                               AdminDashboardService adminDashboardService,
                               AdminMonthlyReportService adminMonthlyReportService,
                               AiRuntimeConfigMapper aiRuntimeConfigMapper,
                               AppAiProperties appAiProperties,
                               SensitiveConfigCipher sensitiveConfigCipher,
                               OaiChatClient oaiChatClient,
                               ApiV1AgentClient apiV1AgentClient,
                               AuditLogService auditLogService,
                               AgentConversationMapper agentConversationMapper,
                               AgentConversationMessageMapper agentConversationMessageMapper,
                               AgentConversationCacheService agentConversationCacheService,
                               ObjectMapper objectMapper) {
        this.accessGuard = accessGuard;
        this.roomBindingService = roomBindingService;
        this.billQueryService = billQueryService;
        this.adminDashboardService = adminDashboardService;
        this.adminMonthlyReportService = adminMonthlyReportService;
        this.aiRuntimeConfigMapper = aiRuntimeConfigMapper;
        this.appAiProperties = appAiProperties;
        this.sensitiveConfigCipher = sensitiveConfigCipher;
        this.oaiChatClient = oaiChatClient;
        this.apiV1AgentClient = apiV1AgentClient;
        this.auditLogService = auditLogService;
        this.agentConversationMapper = agentConversationMapper;
        this.agentConversationMessageMapper = agentConversationMessageMapper;
        this.agentConversationCacheService = agentConversationCacheService;
        this.objectMapper = objectMapper;
    }

    public AgentResidentBillSummaryVO residentBillSummary(LoginUser loginUser) {
        accessGuard.requireRole(loginUser, "RESIDENT");
        List<RoomVO> rooms = roomBindingService.myRooms(loginUser);
        BillListQuery query = new BillListQuery();
        query.setPageNo(1);
        query.setPageSize(10);
        PageResponse<BillListItemVO> page = billQueryService.listMyBills(loginUser, query);
        List<BillListItemVO> recentBills = page.list();
        int activeRoomCount = (int) rooms.stream().filter(room -> "ACTIVE".equals(room.getBindingStatus())).count();
        int unpaidBillCount = (int) recentBills.stream().filter(bill -> !"PAID".equals(bill.getStatus())).count();
        int issuedBillCount = recentBills.size();
        BigDecimal unpaidAmountTotal = recentBills.stream()
                .filter(bill -> !"PAID".equals(bill.getStatus()))
                .map(BillListItemVO::getAmountDue)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        AgentResidentBillSummaryVO vo = new AgentResidentBillSummaryVO();
        vo.setAccountId(loginUser.accountId());
        vo.setRealName(loginUser.realName());
        vo.setRoomCount(rooms.size());
        vo.setActiveRoomCount(activeRoomCount);
        vo.setIssuedBillCount(issuedBillCount);
        vo.setUnpaidBillCount(unpaidBillCount);
        vo.setUnpaidAmountTotal(unpaidAmountTotal);
        vo.setRooms(rooms);
        vo.setRecentBills(recentBills);
        return vo;
    }

    public AgentAdminBillStatsVO adminBillStats(LoginUser loginUser, Integer periodYear, Integer periodMonth) {
        accessGuard.requireRole(loginUser, "ADMIN");
        AdminDashboardSummaryVO summary = adminDashboardService.summary(loginUser, periodYear, periodMonth);
        AgentAdminBillStatsVO vo = new AgentAdminBillStatsVO();
        vo.setPeriodYear(summary.getPeriodYear());
        vo.setPeriodMonth(summary.getPeriodMonth());
        vo.setSummary(summary);
        vo.setPropertyYearly(adminMonthlyReportService.propertyYearly(loginUser, periodYear == null ? summary.getPeriodYear() : periodYear));
        vo.setWaterMonthly(adminMonthlyReportService.waterMonthly(loginUser, summary.getPeriodYear(), summary.getPeriodMonth()));
        return vo;
    }

    public AgentCommandPreviewVO preview(LoginUser loginUser, AgentCommandPreviewRequest request) {
        accessGuard.requireAnyRole(loginUser, "ADMIN", "RESIDENT");
        String prompt = request.getPrompt().trim();
        JsonNode parsed = oaiChatClient.completeJson(loadRuntimeSettings(), AGENT_PARSE_SYSTEM_PROMPT, prompt);

        String action = readText(parsed, "action");
        if (action == null || action.isBlank() || "UNKNOWN".equalsIgnoreCase(action)) {
            throw new BusinessException("AI_INTENT_UNSUPPORTED", "暂未识别该自然语言指令，请改成更明确的房间/账单/支付操作", HttpStatus.BAD_REQUEST);
        }
        Map<String, Object> arguments = toMap(parsed.path("arguments"));
        List<String> warnings = toList(parsed.path("warnings"));
        String riskLevel = normalizeRisk(readText(parsed, "riskLevel"), action);
        String summary = fallbackSummary(readText(parsed, "summary"), action);

        Map<String, Object> resolvedContext = resolveContext(action, arguments, prompt);
        List<String> missingArguments = collectMissingArguments(action, resolvedContext);
        boolean executable = missingArguments.isEmpty();
        boolean confirmationRequired = executable && "L4".equals(riskLevel);
        String confirmationToken = confirmationRequired ? UUID.randomUUID().toString() : null;
        String commandId = UUID.randomUUID().toString();

        AgentCommandSession session = new AgentCommandSession(
                commandId,
                loginUser.accountId(),
                prompt,
                prompt,
                action,
                summary,
                riskLevel,
                confirmationRequired,
                confirmationToken,
                arguments,
                resolvedContext,
                warnings,
                LocalDateTime.now(),
                executable ? (confirmationRequired ? "PENDING_CONFIRMATION" : "EXECUTED") : "NEEDS_INPUT"
        );

        if (!executable) {
            sessions.put(commandId, session);
        } else if (confirmationRequired) {
            sessions.put(commandId, session);
            agentConversationCacheService.saveCommandConfirmation(commandId, Map.of(
                    "operatorId", loginUser.accountId(),
                    "confirmationToken", confirmationToken,
                    "createdAt", session.getCreatedAt().toString()
            ));
        } else {
            Object result = execute(loginUser, session);
            session.setResult(result);
            sessions.put(commandId, session);
        }

        AgentCommandPreviewVO vo = new AgentCommandPreviewVO();
        vo.setCommandId(commandId);
        vo.setOriginalPrompt(prompt);
        vo.setNormalizedPrompt(prompt);
        vo.setAction(action);
        vo.setSummary(summary);
        vo.setRiskLevel(riskLevel);
        vo.setConfirmationRequired(confirmationRequired);
        vo.setConfirmationToken(confirmationToken);
        vo.setExecutable(executable);
        vo.setMessage(executable
                ? (confirmationRequired ? "该操作风险较高，请确认后执行" : "已完成指令预处理")
                : buildMissingArgumentsMessage(action, missingArguments));
        vo.setMissingArguments(missingArguments);
        vo.setParsedArguments(arguments);
        vo.setResolvedContext(resolvedContext);
        vo.setWarnings(warnings);
        vo.setResult(session.getResult());
        return vo;
    }

    public AgentCommandExecutionVO confirm(LoginUser loginUser, AgentCommandConfirmRequest request) {
        accessGuard.requireAnyRole(loginUser, "ADMIN", "RESIDENT");
        AgentCommandSession session = sessions.values().stream()
                .filter(item -> item.getOperatorId() != null && item.getOperatorId().equals(loginUser.accountId()))
                .filter(item -> request.getConfirmationToken().equals(item.getConfirmationToken()))
                .findFirst()
                .orElseGet(() -> sessions.values().stream()
                        .filter(item -> item.getOperatorId() != null && item.getOperatorId().equals(loginUser.accountId()))
                        .filter(item -> agentConversationCacheService.getCommandConfirmation(item.getCommandId())
                                .map(cached -> Objects.equals(request.getConfirmationToken(), cached.get("confirmationToken")))
                                .orElse(false))
                        .findFirst()
                        .orElseThrow(() -> new BusinessException("NOT_FOUND", "未找到待确认指令", HttpStatus.NOT_FOUND)));
        if (!"PENDING_CONFIRMATION".equals(session.getStatus())) {
            throw new BusinessException("CONFLICT", "当前指令无需再次确认", HttpStatus.CONFLICT);
        }
        Object result = execute(loginUser, session);
        session.setResult(result);
        session.setStatus("EXECUTED");
        agentConversationCacheService.deleteCommandConfirmation(session.getCommandId());
        return toExecutionVo(session);
    }

    public AgentCommandExecutionVO getCommand(LoginUser loginUser, String commandId) {
        accessGuard.requireAnyRole(loginUser, "ADMIN", "RESIDENT");
        AgentCommandSession session = sessions.get(commandId);
        if (session == null || !loginUser.accountId().equals(session.getOperatorId())) {
            throw new BusinessException("NOT_FOUND", "未找到指令结果", HttpStatus.NOT_FOUND);
        }
        return toExecutionVo(session);
    }

    public AgentConversationVO converse(LoginUser loginUser, AgentConversationRequest request) {
        accessGuard.requireAnyRole(loginUser, "ADMIN", "RESIDENT");
        AgentConversationEntity conversation = resolveConversation(loginUser, request.getSessionId());
        String message = request.getMessage().trim();

        AgentConversationMessageVO userMessage = buildUserConversationMessage(message);
        appendMessage(conversation, userMessage);

        AgentConversationMessageVO assistantMessage;
        Map<String, Object> context = readContext(conversation);
        AgentCommandSession pendingSession = findPendingCommandSession(loginUser, context);
        if (pendingSession != null) {
            AgentCommandPreviewVO preview = continuePendingCommand(loginUser, pendingSession, message);
            assistantMessage = buildCommandConversationMessage(preview);
            appendMessage(conversation, assistantMessage);
            updateCommandContext(context, preview);
            updateConversationState(conversation, context, assistantMessage.getContent(), firstUserPrompt(conversation, message));
        } else if (shouldHandleAsCommand(message)) {
            AgentCommandPreviewVO preview = preview(loginUser, toPreviewRequest(message));
            assistantMessage = buildCommandConversationMessage(preview);
            appendMessage(conversation, assistantMessage);
            updateCommandContext(context, preview);
            updateConversationState(conversation, context, assistantMessage.getContent(), firstUserPrompt(conversation, message));
        } else {
            assistantMessage = buildChatReply(loginUser, conversation);
            appendMessage(conversation, assistantMessage);
            context.remove("pendingCommandId");
            context.put("lastMode", "CHAT");
            updateConversationState(conversation, context, assistantMessage.getContent(), firstUserPrompt(conversation, message));
        }

        return getConversation(loginUser, conversation.getSessionId());
    }

    public PageResponse<AgentConversationListItemVO> listConversations(LoginUser loginUser, AgentConversationListQuery query) {
        accessGuard.requireAnyRole(loginUser, "ADMIN", "RESIDENT");
        int pageNo = query.getPageNo() == null || query.getPageNo() < 1 ? 1 : query.getPageNo();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : Math.min(query.getPageSize(), 50);
        return agentConversationCacheService.getConversationList(loginUser.accountId(), pageNo, pageSize)
                .orElseGet(() -> {
                    int offset = (pageNo - 1) * pageSize;
                    List<AgentConversationListItemVO> list = agentConversationMapper.listPage(loginUser.accountId(), offset, pageSize);
                    long total = agentConversationMapper.countByOperatorId(loginUser.accountId());
                    PageResponse<AgentConversationListItemVO> page = new PageResponse<>(list, pageNo, pageSize, total);
                    agentConversationCacheService.cacheConversationList(loginUser.accountId(), pageNo, pageSize, page);
                    return page;
                });
    }

    public AgentConversationVO getConversation(LoginUser loginUser, String sessionId) {
        accessGuard.requireAnyRole(loginUser, "ADMIN", "RESIDENT");
        AgentConversationVO cached = agentConversationCacheService.getConversationDetail(sessionId).orElse(null);
        if (cached != null) {
            AgentConversationEntity conversation = agentConversationMapper.findBySessionId(sessionId);
            if (conversation != null && loginUser.accountId().equals(conversation.getOperatorId())) {
                return sanitizeConversationForHistory(cached);
            }
        }
        AgentConversationEntity conversation = requireConversation(loginUser, sessionId);
        AgentConversationVO vo = toConversationVo(conversation);
        agentConversationCacheService.cacheConversationDetail(sessionId, vo);
        return sanitizeConversationForHistory(vo);
    }

    public SseEmitter streamConversation(LoginUser loginUser, AgentConversationRequest request) {
        accessGuard.requireAnyRole(loginUser, "ADMIN", "RESIDENT");
        AgentConversationEntity conversation = resolveConversation(loginUser, request.getSessionId());
        String message = request.getMessage().trim();

        AgentConversationMessageVO userMessage = buildUserConversationMessage(message);
        appendMessage(conversation, userMessage);

        SseEmitter emitter = new SseEmitter(0L);
        emitter.onTimeout(emitter::complete);
        emitter.onError((ex) -> emitter.complete());
        String authorization = RequestAuthHolder.getAuthorization();

        CompletableFuture.runAsync(() -> {
            try {
                RequestAuthHolder.setAuthorization(authorization);
                sendEvent(emitter, "session", Map.of("sessionId", conversation.getSessionId()));
                Map<String, Object> context = readContext(conversation);
                AgentCommandSession pendingSession = findPendingCommandSession(loginUser, context);
                if (pendingSession != null) {
                    AgentCommandPreviewVO preview = continuePendingCommand(loginUser, pendingSession, message);
                    AgentConversationMessageVO assistantMessage = buildCommandConversationMessage(preview);
                    appendMessage(conversation, assistantMessage);
                    updateCommandContext(context, preview);
                    updateConversationState(conversation, context, assistantMessage.getContent(), firstUserPrompt(conversation, message));
                    sendEvent(emitter, "command-preview", toStreamMessagePayload(conversation.getSessionId(), assistantMessage.getId(), assistantMessage));
                    sendEvent(emitter, "done", Map.of("sessionId", conversation.getSessionId()));
                    emitter.complete();
                    return;
                }
                if (shouldHandleAsCommand(message)) {
                    AgentCommandPreviewVO preview = preview(loginUser, toPreviewRequest(message));
                    AgentConversationMessageVO assistantMessage = buildCommandConversationMessage(preview);
                    appendMessage(conversation, assistantMessage);
                    updateCommandContext(context, preview);
                    updateConversationState(conversation, context, assistantMessage.getContent(), firstUserPrompt(conversation, message));
                    sendEvent(emitter, "command-preview", toStreamMessagePayload(conversation.getSessionId(), assistantMessage.getId(), assistantMessage));
                    sendEvent(emitter, "done", Map.of("sessionId", conversation.getSessionId()));
                    emitter.complete();
                    return;
                }

                String messageId = UUID.randomUUID().toString();
                sendEvent(emitter, "message-start", Map.of(
                        "sessionId", conversation.getSessionId(),
                        "messageId", messageId,
                        "role", "assistant",
                        "mode", "CHAT"
                ));

                AtomicReference<String> contentRef = new AtomicReference<>("");
                String fullReply = oaiChatClient.streamChat(loadRuntimeSettings(), buildChatMessages(loginUser, conversation.getSessionId()), delta -> {
                    String content = contentRef.updateAndGet(existing -> existing + delta);
                    sendEvent(emitter, "message-delta", Map.of(
                            "sessionId", conversation.getSessionId(),
                            "messageId", messageId,
                            "delta", delta,
                            "content", content
                    ));
                });

                AgentConversationMessageVO assistantMessage = new AgentConversationMessageVO();
                assistantMessage.setId(messageId);
                assistantMessage.setRole("assistant");
                assistantMessage.setMode("CHAT");
                assistantMessage.setContent(fullReply);
                assistantMessage.setConfirmationRequired(false);
                appendMessage(conversation, assistantMessage);
                context.remove("pendingCommandId");
                context.put("lastMode", "CHAT");
                updateConversationState(conversation, context, assistantMessage.getContent(), firstUserPrompt(conversation, message));

                sendEvent(emitter, "message-complete", toStreamMessagePayload(conversation.getSessionId(), messageId, assistantMessage));
                sendEvent(emitter, "done", Map.of("sessionId", conversation.getSessionId()));
                emitter.complete();
            } catch (BusinessException ex) {
                sendErrorEvent(emitter, ex.getCode(), ex.getMessage());
                emitter.complete();
            } catch (Exception ex) {
                sendErrorEvent(emitter, "AI_STREAM_FAILED", "流式对话失败");
                emitter.complete();
            } finally {
                RequestAuthHolder.clear();
            }
        });
        return emitter;
    }

    private Object execute(LoginUser loginUser, AgentCommandSession session) {
        return switch (session.getAction()) {
            case "ROOM_CREATE" -> executeRoomCreate(loginUser, session);
            case "ROOM_DISABLE" -> executeRoomDisable(loginUser, session);
            case "BILL_DETAIL" -> executeBillDetail(loginUser, session);
            case "BILL_LIST_BY_ROOM" -> executeBillListByRoom(loginUser, session);
            case "PAYMENT_CREATE" -> executePaymentCreate(loginUser, session);
            case "PAYMENT_QUERY" -> executePaymentQuery(loginUser, session);
            case "WATER_READING_CREATE" -> executeWaterReadingCreate(loginUser, session);
            default -> throw new BusinessException("AI_INTENT_UNSUPPORTED", "暂不支持该指令动作", HttpStatus.BAD_REQUEST);
        };
    }

    private AdminRoomVO executeRoomCreate(LoginUser loginUser, AgentCommandSession session) {
        accessGuard.requireRole(loginUser, "ADMIN");
        Map<String, Object> resolved = session.getResolvedContext();
        Long communityId = asLong(resolved.get("communityId"));
        Long roomTypeId = asLong(resolved.get("roomTypeId"));
        String buildingNo = asText(resolved.get("buildingNo"));
        String unitNo = asText(resolved.get("unitNo"));
        String roomNo = asText(resolved.get("roomNo"));
        BigDecimal areaM2 = asBigDecimal(resolved.get("areaM2"));
        if (communityId == null || buildingNo == null || unitNo == null || roomNo == null || areaM2 == null) {
            throw new BusinessException("AI_ARGUMENTS_INCOMPLETE", "房间创建参数不完整", HttpStatus.BAD_REQUEST);
        }
        AdminRoomCreateDTO dto = new AdminRoomCreateDTO();
        dto.setCommunityId(communityId);
        dto.setRoomTypeId(roomTypeId);
        dto.setBuildingNo(buildingNo);
        dto.setUnitNo(unitNo);
        dto.setRoomNo(roomNo);
        dto.setAreaM2(areaM2);
        AdminRoomVO created = apiV1AgentClient.createRoom(dto);
        auditLogService.record(loginUser, "AI_AGENT", session.getCommandId(), "ROOM_CREATE", Map.of(
                "prompt", session.getOriginalPrompt(),
                "action", session.getAction(),
                "roomId", created.getId()
        ));
        return created;
    }

    private Map<String, Object> executeRoomDisable(LoginUser loginUser, AgentCommandSession session) {
        accessGuard.requireRole(loginUser, "ADMIN");
        Long roomId = asLong(session.getResolvedContext().get("roomId"));
        if (roomId == null) {
            throw new BusinessException("AI_ARGUMENTS_INCOMPLETE", "缺少可停用的房间标识", HttpStatus.BAD_REQUEST);
        }
        apiV1AgentClient.disableRoom(roomId);
        auditLogService.record(loginUser, "AI_AGENT", session.getCommandId(), "ROOM_DISABLE", Map.of(
                "prompt", session.getOriginalPrompt(),
                "roomId", roomId
        ));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("roomId", roomId);
        result.put("mode", "DISABLE");
        return result;
    }

    private BillDetailVO executeBillDetail(LoginUser loginUser, AgentCommandSession session) {
        Long billId = asLong(session.getResolvedContext().get("billId"));
        if (billId == null) {
            throw new BusinessException("AI_ARGUMENTS_INCOMPLETE", "缺少账单 ID", HttpStatus.BAD_REQUEST);
        }
        return apiV1AgentClient.getBillDetail(billId, loginUser.hasRole("ADMIN"));
    }

    private PageResponse<BillListItemVO> executeBillListByRoom(LoginUser loginUser, AgentCommandSession session) {
        Long roomId = asLong(session.getResolvedContext().get("roomId"));
        if (roomId == null) {
            throw new BusinessException("AI_ARGUMENTS_INCOMPLETE", "未能根据小区、楼栋、单元和房号定位房间", HttpStatus.BAD_REQUEST);
        }
        return apiV1AgentClient.listRoomBills(roomId, asText(session.getResolvedContext().get("status")), loginUser.hasRole("ADMIN"));
    }

    private PaymentCreateVO executePaymentCreate(LoginUser loginUser, AgentCommandSession session) {
        Map<String, Object> resolved = session.getResolvedContext();
        Long billId = asLong(resolved.get("billId"));
        String channel = asText(resolved.get("channel"));
        if (billId == null || channel == null) {
            throw new BusinessException("AI_ARGUMENTS_INCOMPLETE", "缺少支付创建参数", HttpStatus.BAD_REQUEST);
        }
        PaymentCreateDTO dto = new PaymentCreateDTO();
        dto.setBillId(billId);
        dto.setChannel(channel);
        dto.setAnnualPayment(Boolean.TRUE.equals(resolved.get("annualPayment")));
        dto.setIdempotencyKey("agent-" + UUID.randomUUID());
        return apiV1AgentClient.createPayment(dto);
    }

    private PaymentStatusVO executePaymentQuery(LoginUser loginUser, AgentCommandSession session) {
        String payOrderNo = asText(session.getResolvedContext().get("payOrderNo"));
        if (payOrderNo == null) {
            throw new BusinessException("AI_ARGUMENTS_INCOMPLETE", "缺少支付单号", HttpStatus.BAD_REQUEST);
        }
        return apiV1AgentClient.queryPayment(payOrderNo);
    }

    private Map<String, Object> executeWaterReadingCreate(LoginUser loginUser, AgentCommandSession session) {
        Long roomId = asLong(session.getResolvedContext().get("roomId"));
        Integer year = asInteger(session.getResolvedContext().get("year"));
        Integer month = asInteger(session.getResolvedContext().get("month"));
        BigDecimal prevReading = asBigDecimal(session.getResolvedContext().get("prevReading"));
        BigDecimal currReading = asBigDecimal(session.getResolvedContext().get("currReading"));
        String readAt = asText(session.getResolvedContext().get("readAt"));
        if (roomId == null || year == null || month == null || prevReading == null || currReading == null || readAt == null) {
            throw new BusinessException("AI_ARGUMENTS_INCOMPLETE", "抄表参数不完整，请补充房间、年月、上期读数、本期读数和抄表时间", HttpStatus.BAD_REQUEST);
        }
        return apiV1AgentClient.createWaterReading(roomId, year, month, prevReading, currReading, readAt, asText(session.getResolvedContext().get("remark")));
    }

    private Map<String, Object> resolveContext(String action, Map<String, Object> arguments, String prompt) {
        Map<String, Object> resolved = new LinkedHashMap<>();
        resolved.putAll(arguments);
        if ("ROOM_CREATE".equals(action)) {
            String communityName = asText(arguments.get("communityName"));
            if (communityName != null) {
                Long communityId = apiV1AgentClient.listAdminCommunities().stream()
                        .filter(item -> communityName.equals(item.getName()))
                        .map(AdminCommunityVO::getId)
                        .findFirst()
                        .orElse(null);
                resolved.put("communityId", communityId);
            }
            Long communityId = asLong(resolved.get("communityId"));
            String roomTypeName = asText(arguments.get("roomTypeName"));
            if (communityId != null && roomTypeName != null) {
                Long roomTypeId = apiV1AgentClient.listRoomTypes(communityId).stream()
                        .filter(item -> roomTypeName.equals(item.getTypeName()))
                        .map(RoomTypeVO::getId)
                        .findFirst()
                        .orElse(null);
                resolved.put("roomTypeId", roomTypeId);
                if (resolved.get("areaM2") == null && roomTypeId != null) {
                    apiV1AgentClient.listRoomTypes(communityId).stream()
                            .filter(item -> roomTypeId.equals(item.getId()))
                            .findFirst()
                            .ifPresent(item -> resolved.put("areaM2", item.getAreaM2()));
                }
            }
        }
        if ("ROOM_DISABLE".equals(action)) {
            Long roomId = asLong(arguments.get("roomId"));
            if (roomId == null) {
                Long communityId = apiV1AgentClient.listAdminCommunities().stream()
                        .filter(item -> asText(arguments.get("communityName")) != null && asText(arguments.get("communityName")).equals(item.getName()))
                        .map(AdminCommunityVO::getId)
                        .findFirst()
                        .orElse(null);
                if (communityId != null) {
                    AdminRoomListQuery query = new AdminRoomListQuery();
                    query.setCommunityId(communityId);
                    query.setBuildingNo(normalizeLocationPart(asText(arguments.get("buildingNo"))));
                    query.setUnitNo(normalizeLocationPart(asText(arguments.get("unitNo"))));
                    query.setRoomNo(normalizeRoomNo(asText(arguments.get("roomNo"))));
                    List<AdminRoomVO> rooms = apiV1AgentClient.listAdminRooms(query);
                    roomId = rooms.isEmpty() ? null : rooms.get(0).getId();
                    resolved.put("communityId", communityId);
                }
                resolved.put("roomId", roomId);
            }
        }
        if ("BILL_DETAIL".equals(action)) {
            resolved.put("billId", asLong(arguments.get("billId")));
        }
        if ("BILL_LIST_BY_ROOM".equals(action)) {
            Long roomId = asLong(arguments.get("roomId"));
            String communityName = firstNonBlank(asText(arguments.get("communityName")), extractCommunityName(prompt));
            String buildingNo = firstNonBlank(normalizeLocationPart(asText(arguments.get("buildingNo"))), extractBuildingNo(prompt));
            String unitNo = firstNonBlank(normalizeLocationPart(asText(arguments.get("unitNo"))), extractUnitNo(prompt));
            String roomNo = firstNonBlank(normalizeRoomNo(asText(arguments.get("roomNo"))), extractRoomNo(prompt));
            if (roomId == null && communityName != null && buildingNo != null && unitNo != null && roomNo != null) {
                Long communityId = apiV1AgentClient.listAdminCommunities().stream()
                        .filter(item -> communityName.equals(item.getName()))
                        .map(AdminCommunityVO::getId)
                        .findFirst()
                        .orElse(null);
                resolved.put("communityId", communityId);
                if (communityId != null) {
                    AdminRoomListQuery query = new AdminRoomListQuery();
                    query.setCommunityId(communityId);
                    query.setBuildingNo(buildingNo);
                    query.setUnitNo(unitNo);
                    query.setRoomNo(roomNo);
                    List<AdminRoomVO> rooms = apiV1AgentClient.listAdminRooms(query);
                    roomId = rooms.isEmpty() ? null : rooms.get(0).getId();
                }
            }
            resolved.put("communityName", communityName);
            resolved.put("buildingNo", buildingNo);
            resolved.put("unitNo", unitNo);
            resolved.put("roomNo", roomNo);
            resolved.put("roomId", roomId);
            resolved.put("status", normalizeBillStatus(asText(arguments.get("status")), prompt, arguments));
        }
        if ("PAYMENT_CREATE".equals(action)) {
            resolved.put("billId", asLong(arguments.get("billId")));
            String channel = asText(arguments.get("channel"));
            resolved.put("channel", channel == null ? "WECHAT" : channel.toUpperCase());
            resolved.put("annualPayment", Boolean.TRUE.equals(arguments.get("annualPayment")));
        }
        if ("PAYMENT_QUERY".equals(action)) {
            resolved.put("payOrderNo", asText(arguments.get("payOrderNo")));
        }
        if ("WATER_READING_CREATE".equals(action)) {
            String communityName = firstNonBlank(asText(arguments.get("communityName")), extractCommunityName(prompt));
            String buildingNo = firstNonBlank(normalizeLocationPart(asText(arguments.get("buildingNo"))), extractBuildingNo(prompt));
            String unitNo = firstNonBlank(normalizeLocationPart(asText(arguments.get("unitNo"))), extractUnitNo(prompt));
            String roomNo = firstNonBlank(normalizeRoomNo(asText(arguments.get("roomNo"))), extractRoomNo(prompt));
            Long roomId = null;
            if (communityName != null && buildingNo != null && unitNo != null && roomNo != null) {
                Long communityId = apiV1AgentClient.listAdminCommunities().stream()
                        .filter(item -> communityName.equals(item.getName()))
                        .map(AdminCommunityVO::getId)
                        .findFirst()
                        .orElse(null);
                resolved.put("communityId", communityId);
                if (communityId != null) {
                    AdminRoomListQuery query = new AdminRoomListQuery();
                    query.setCommunityId(communityId);
                    query.setBuildingNo(buildingNo);
                    query.setUnitNo(unitNo);
                    query.setRoomNo(roomNo);
                    List<AdminRoomVO> rooms = apiV1AgentClient.listAdminRooms(query);
                    roomId = rooms.isEmpty() ? null : rooms.get(0).getId();
                }
            }
            resolved.put("communityName", communityName);
            resolved.put("buildingNo", buildingNo);
            resolved.put("unitNo", unitNo);
            resolved.put("roomNo", roomNo);
            resolved.put("roomId", roomId);
            resolved.put("year", asInteger(arguments.get("year")));
            resolved.put("month", asInteger(arguments.get("month")));
            resolved.put("prevReading", asBigDecimal(arguments.get("prevReading")));
            resolved.put("currReading", asBigDecimal(arguments.get("currReading")));
            resolved.put("readAt", asText(arguments.get("readAt")));
            resolved.put("remark", asText(arguments.get("remark")));
        }
        return resolved;
    }

    private AgentCommandSession findPendingCommandSession(LoginUser loginUser, Map<String, Object> context) {
        if (context == null) {
            return null;
        }
        String pendingCommandId = asText(context.get("pendingCommandId"));
        if (pendingCommandId == null) {
            return null;
        }
        AgentCommandSession session = sessions.get(pendingCommandId);
        if (session == null || !"NEEDS_INPUT".equals(session.getStatus())) {
            return null;
        }
        return Objects.equals(loginUser.accountId(), session.getOperatorId()) ? session : null;
    }

    private AgentCommandPreviewVO continuePendingCommand(LoginUser loginUser,
                                                         AgentCommandSession pendingSession,
                                                         String supplementPrompt) {
        String supplement = supplementPrompt == null ? "" : supplementPrompt.trim();
        JsonNode parsed = oaiChatClient.completeJson(loadRuntimeSettings(), AGENT_PARSE_SYSTEM_PROMPT, supplement);

        Map<String, Object> arguments = new LinkedHashMap<>(pendingSession.getParsedArguments());
        arguments.putAll(toMap(parsed.path("arguments")));

        List<String> warnings = new ArrayList<>();
        warnings.add("已结合上一轮上下文继续补全参数。");
        warnings.addAll(toList(parsed.path("warnings")));

        String action = pendingSession.getAction();
        String summary = fallbackSummary(readText(parsed, "summary"), action);
        String riskLevel = normalizeRisk(readText(parsed, "riskLevel"), action);
        String normalizedPrompt = pendingSession.getOriginalPrompt() + "\n补充参数：" + supplement;
        Map<String, Object> resolvedContext = new LinkedHashMap<>(pendingSession.getResolvedContext());
        resolvedContext.putAll(resolveContext(action, arguments, normalizedPrompt));
        List<String> missingArguments = collectMissingArguments(action, resolvedContext);
        boolean executable = missingArguments.isEmpty();
        boolean confirmationRequired = executable && "L4".equals(riskLevel);
        String confirmationToken = confirmationRequired ? UUID.randomUUID().toString() : null;
        String commandId = UUID.randomUUID().toString();

        AgentCommandSession session = new AgentCommandSession(
                commandId,
                loginUser.accountId(),
                normalizedPrompt,
                normalizedPrompt,
                action,
                summary,
                riskLevel,
                confirmationRequired,
                confirmationToken,
                arguments,
                resolvedContext,
                warnings,
                LocalDateTime.now(),
                executable ? (confirmationRequired ? "PENDING_CONFIRMATION" : "EXECUTED") : "NEEDS_INPUT"
        );

        if (!executable) {
            sessions.put(commandId, session);
        } else if (confirmationRequired) {
            sessions.put(commandId, session);
            agentConversationCacheService.saveCommandConfirmation(commandId, Map.of(
                    "operatorId", loginUser.accountId(),
                    "confirmationToken", confirmationToken,
                    "createdAt", session.getCreatedAt().toString()
            ));
        } else {
            Object result = execute(loginUser, session);
            session.setResult(result);
            sessions.put(commandId, session);
        }

        AgentCommandPreviewVO vo = new AgentCommandPreviewVO();
        vo.setCommandId(commandId);
        vo.setOriginalPrompt(supplement);
        vo.setNormalizedPrompt(normalizedPrompt);
        vo.setAction(action);
        vo.setSummary(summary);
        vo.setRiskLevel(riskLevel);
        vo.setConfirmationRequired(confirmationRequired);
        vo.setConfirmationToken(confirmationToken);
        vo.setExecutable(executable);
        vo.setMessage(executable ? "已完成指令预处理" : buildMissingArgumentsMessage(action, missingArguments));
        vo.setMissingArguments(missingArguments);
        vo.setParsedArguments(arguments);
        vo.setResolvedContext(resolvedContext);
        vo.setWarnings(warnings);
        vo.setResult(session.getResult());
        return vo;
    }

    private void updateCommandContext(Map<String, Object> context, AgentCommandPreviewVO preview) {
        context.putAll(preview.getResolvedContext());
        context.put("lastAction", preview.getAction());
        if (preview.isExecutable()) {
            context.remove("pendingCommandId");
        } else {
            context.put("pendingCommandId", preview.getCommandId());
        }
    }

    private List<String> collectMissingArguments(String action, Map<String, Object> resolvedContext) {
        List<String> missing = new ArrayList<>();
        switch (action) {
            case "ROOM_CREATE" -> {
                requireMissing(missing, resolvedContext, "communityId", "小区");
                requireMissing(missing, resolvedContext, "buildingNo", "楼栋");
                requireMissing(missing, resolvedContext, "unitNo", "单元");
                requireMissing(missing, resolvedContext, "roomNo", "房号");
                requireMissing(missing, resolvedContext, "areaM2", "面积");
            }
            case "ROOM_DISABLE" -> {
                if (asLong(resolvedContext.get("roomId")) == null) {
                    requireMissing(missing, resolvedContext, "communityName", "小区");
                    requireMissing(missing, resolvedContext, "buildingNo", "楼栋");
                    requireMissing(missing, resolvedContext, "unitNo", "单元");
                    requireMissing(missing, resolvedContext, "roomNo", "房号");
                }
            }
            case "BILL_DETAIL" -> requireMissing(missing, resolvedContext, "billId", "账单ID");
            case "BILL_LIST_BY_ROOM" -> {
                if (asLong(resolvedContext.get("roomId")) == null) {
                    requireMissing(missing, resolvedContext, "communityName", "小区");
                    requireMissing(missing, resolvedContext, "buildingNo", "楼栋");
                    requireMissing(missing, resolvedContext, "unitNo", "单元");
                    requireMissing(missing, resolvedContext, "roomNo", "房号");
                }
            }
            case "PAYMENT_CREATE" -> {
                requireMissing(missing, resolvedContext, "billId", "账单ID");
                requireMissing(missing, resolvedContext, "channel", "支付渠道");
            }
            case "PAYMENT_QUERY" -> requireMissing(missing, resolvedContext, "payOrderNo", "支付单号");
            case "WATER_READING_CREATE" -> {
                if (asLong(resolvedContext.get("roomId")) == null) {
                    requireMissing(missing, resolvedContext, "communityName", "小区");
                    requireMissing(missing, resolvedContext, "buildingNo", "楼栋");
                    requireMissing(missing, resolvedContext, "unitNo", "单元");
                    requireMissing(missing, resolvedContext, "roomNo", "房号");
                }
                requireMissing(missing, resolvedContext, "year", "年份");
                requireMissing(missing, resolvedContext, "month", "月份");
                requireMissing(missing, resolvedContext, "prevReading", "上期读数");
                requireMissing(missing, resolvedContext, "currReading", "本期读数");
                requireMissing(missing, resolvedContext, "readAt", "抄表时间");
            }
            default -> {
            }
        }
        return missing;
    }

    private void requireMissing(List<String> missing, Map<String, Object> context, String key, String label) {
        Object value = context.get(key);
        boolean absent = value == null;
        if (value instanceof String text) {
            absent = text.isBlank();
        }
        if (absent && !missing.contains(label)) {
            missing.add(label);
        }
    }

    private String buildMissingArgumentsMessage(String action, List<String> missingArguments) {
        String missing = String.join("、", missingArguments);
        return switch (action) {
            case "ROOM_CREATE" -> "创建房间前还缺少：" + missing + "。请继续补充。";
            case "ROOM_DISABLE" -> "停用房间前还缺少：" + missing + "。请继续补充。";
            case "BILL_DETAIL" -> "查询账单详情前还缺少：" + missing + "。请继续补充。";
            case "BILL_LIST_BY_ROOM" -> "按房间查询账单前还缺少：" + missing + "。请继续补充。";
            case "PAYMENT_CREATE" -> "创建支付单前还缺少：" + missing + "。请继续补充。";
            case "PAYMENT_QUERY" -> "查询支付单前还缺少：" + missing + "。请继续补充。";
            case "WATER_READING_CREATE" -> "录入水表抄表前还缺少：" + missing + "。请继续补充。";
            default -> "当前指令还缺少必要参数：" + missing + "。请继续补充。";
        };
    }

    private AgentCommandExecutionVO toExecutionVo(AgentCommandSession session) {
        AgentCommandExecutionVO vo = new AgentCommandExecutionVO();
        vo.setCommandId(session.getCommandId());
        vo.setStatus(session.getStatus());
        vo.setAction(session.getAction());
        vo.setRiskLevel(session.getRiskLevel());
        vo.setSummary(session.getSummary());
        vo.setResult(session.getResult());
        return vo;
    }

    private AgentConversationEntity resolveConversation(LoginUser loginUser, String sessionId) {
        if (sessionId != null && !sessionId.isBlank()) {
            return requireConversation(loginUser, sessionId);
        }
        AgentConversationEntity created = new AgentConversationEntity();
        created.setSessionId(UUID.randomUUID().toString());
        created.setOperatorId(loginUser.accountId());
        created.setTitle(null);
        created.setContextJson(writeJson(new LinkedHashMap<>()));
        created.setLastMessagePreview(null);
        created.setMessageCount(0);
        created.setStatus("ACTIVE");
        agentConversationMapper.insert(created);
        evictConversationList(loginUser.accountId());
        return created;
    }

    private AgentConversationEntity requireConversation(LoginUser loginUser, String sessionId) {
        AgentConversationEntity conversation = agentConversationMapper.findBySessionId(sessionId);
        if (conversation == null || !loginUser.accountId().equals(conversation.getOperatorId())) {
            throw new BusinessException("NOT_FOUND", "会话不存在", HttpStatus.NOT_FOUND);
        }
        return conversation;
    }

    private AgentConversationVO toConversationVo(AgentConversationEntity conversation) {
        AgentConversationVO vo = new AgentConversationVO();
        vo.setSessionId(conversation.getSessionId());
        vo.setTitle(conversation.getTitle());
        vo.setContext(readContext(conversation));
        vo.setMessages(agentConversationMessageMapper.listBySessionId(conversation.getSessionId()).stream().map(this::toMessageVo).toList());
        vo.setMessageCount(conversation.getMessageCount());
        vo.setCreatedAt(conversation.getCreatedAt());
        vo.setUpdatedAt(conversation.getUpdatedAt());
        return vo;
    }

    private AgentConversationMessageVO buildChatReply(LoginUser loginUser, AgentConversationEntity conversation) {
        AgentConversationMessageVO assistantMessage = new AgentConversationMessageVO();
        assistantMessage.setRole("assistant");
        assistantMessage.setMode("CHAT");
        assistantMessage.setConfirmationRequired(false);
        assistantMessage.setContent(oaiChatClient.completeChat(loadRuntimeSettings(), buildChatMessages(loginUser, conversation.getSessionId())));
        return assistantMessage;
    }

    private AgentConversationMessageVO buildCommandConversationMessage(AgentCommandPreviewVO preview) {
        AgentConversationMessageVO assistantMessage = new AgentConversationMessageVO();
        assistantMessage.setRole("assistant");
        assistantMessage.setMode(preview.isConfirmationRequired() ? "ACTION" : isQueryAction(preview.getAction()) ? "QUERY" : "ACTION");
        assistantMessage.setContent(buildAssistantReplyText(preview));
        assistantMessage.setAction(preview.getAction());
        assistantMessage.setCommandId(preview.getCommandId());
        assistantMessage.setRiskLevel(preview.getRiskLevel());
        assistantMessage.setConfirmationRequired(preview.isConfirmationRequired());
        assistantMessage.setConfirmationToken(preview.getConfirmationToken());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("summary", preview.getSummary());
        payload.put("parsedArguments", preview.getParsedArguments());
        payload.put("resolvedContext", preview.getResolvedContext());
        payload.put("missingArguments", preview.getMissingArguments());
        payload.put("warnings", preview.getWarnings());
        payload.put("confirmationToken", preview.getConfirmationToken());
        payload.put("executable", preview.isExecutable());
        payload.put("result", preview.getResult());
        assistantMessage.setPayload(payload);
        return assistantMessage;
    }

    private boolean shouldHandleAsCommand(String message) {
        return looksLikeCommand(message);
    }

    private boolean looksLikeCommand(String message) {
        return message.contains("查")
                || message.contains("查询")
                || message.contains("账单")
                || message.contains("支付")
                || message.contains("房间")
                || message.contains("创建")
                || message.contains("新增")
                || message.contains("停用")
                || message.contains("禁用")
                || message.contains("开通")
                || message.contains("bill")
                || message.contains("room")
                || message.contains("pay");
    }

    private List<Map<String, String>> buildChatMessages(LoginUser loginUser, String sessionId) {
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", buildChatSystemPrompt(loginUser)));

        List<AgentConversationMessageEntity> history = agentConversationMessageMapper.listBySessionId(sessionId);
        int start = Math.max(0, history.size() - 12);
        for (int index = start; index < history.size(); index++) {
            AgentConversationMessageEntity item = history.get(index);
            if (item.getContent() == null || item.getContent().isBlank()) {
                continue;
            }
            String role = "assistant".equals(item.getRole()) ? "assistant" : "user";
            messages.add(Map.of("role", role, "content", item.getContent()));
        }
        return messages;
    }

    private String buildChatSystemPrompt(LoginUser loginUser) {
        String roleScope = loginUser.hasRole("ADMIN")
                ? "当前用户角色是 ADMIN，可以讨论管理端视角的账单、房间、支付与操作流程。"
                : "当前用户角色是 RESIDENT，只能讨论自己绑定房间相关的账单、支付与说明。";
        return AGENT_CHAT_SYSTEM_PROMPT + "\n" + roleScope;
    }

    private boolean isQueryAction(String action) {
        return "BILL_DETAIL".equals(action) || "BILL_LIST_BY_ROOM".equals(action) || "PAYMENT_QUERY".equals(action);
    }

    private String buildAssistantReplyText(AgentCommandPreviewVO preview) {
        if (!preview.isExecutable()) {
            return preview.getMessage();
        }
        if (preview.isConfirmationRequired()) {
            return "我已经整理好这次受控操作的预览，请确认后再执行。";
        }
        return switch (preview.getAction()) {
            case "BILL_DETAIL", "BILL_LIST_BY_ROOM", "PAYMENT_QUERY" -> "我已根据你的问题查到结果，下面是整理后的摘要。";
            case "ROOM_CREATE", "ROOM_DISABLE", "PAYMENT_CREATE", "WATER_READING_CREATE" -> "我已经按你的要求完成本次操作。";
            default -> preview.getMessage();
        };
    }

    private AgentConversationMessageVO buildUserConversationMessage(String message) {
        AgentConversationMessageVO userMessage = new AgentConversationMessageVO();
        userMessage.setId(UUID.randomUUID().toString());
        userMessage.setRole("user");
        userMessage.setMode("CHAT");
        userMessage.setContent(message);
        userMessage.setConfirmationRequired(false);
        return userMessage;
    }

    private void appendMessage(AgentConversationEntity conversation, AgentConversationMessageVO message) {
        if (message.getId() == null || message.getId().isBlank()) {
            message.setId(UUID.randomUUID().toString());
        }
        AgentConversationMessageEntity entity = new AgentConversationMessageEntity();
        entity.setMessageId(message.getId());
        entity.setSessionId(conversation.getSessionId());
        entity.setSeqNo(nextSeqNo(conversation.getSessionId()));
        entity.setRole(message.getRole());
        entity.setMode(message.getMode());
        entity.setContent(message.getContent() == null ? "" : message.getContent());
        entity.setAction(message.getAction());
        entity.setCommandId(message.getCommandId());
        entity.setRiskLevel(message.getRiskLevel());
        entity.setConfirmationRequired(message.isConfirmationRequired());
        entity.setPayloadJson(writeJson(message.getPayload()));
        agentConversationMessageMapper.insert(entity);
        message.setCreatedAt(entity.getCreatedAt());
        agentConversationCacheService.evictConversationDetail(conversation.getSessionId());
        evictConversationList(conversation.getOperatorId());
    }

    private int nextSeqNo(String sessionId) {
        Integer maxSeqNo = agentConversationMessageMapper.maxSeqNoBySessionId(sessionId);
        return (maxSeqNo == null ? 0 : maxSeqNo) + 1;
    }

    private void updateConversationState(AgentConversationEntity conversation,
                                         Map<String, Object> context,
                                         String lastMessagePreview,
                                         String titleSeed) {
        conversation.setContextJson(writeJson(context));
        conversation.setLastMessagePreview(shortText(lastMessagePreview, 240));
        Integer latestCount = agentConversationMessageMapper.maxSeqNoBySessionId(conversation.getSessionId());
        conversation.setMessageCount(latestCount == null ? 0 : latestCount);
        if (conversation.getTitle() == null || conversation.getTitle().isBlank()) {
            conversation.setTitle(shortText(titleSeed, 64));
        }
        agentConversationMapper.updateConversation(conversation);
        agentConversationCacheService.evictConversationDetail(conversation.getSessionId());
        evictConversationList(conversation.getOperatorId());
    }

    private void evictConversationList(Long operatorId) {
        agentConversationCacheService.evictConversationList(operatorId, 1, 20);
    }

    private String firstUserPrompt(AgentConversationEntity conversation, String fallback) {
        return conversation.getTitle() != null && !conversation.getTitle().isBlank() ? conversation.getTitle() : fallback;
    }

    private Map<String, Object> readContext(AgentConversationEntity conversation) {
        if (conversation.getContextJson() == null || conversation.getContextJson().isBlank()) {
            return new LinkedHashMap<>();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(conversation.getContextJson(), MAP_TYPE);
            return map == null ? new LinkedHashMap<>() : new LinkedHashMap<>(map);
        } catch (JsonProcessingException ex) {
            return new LinkedHashMap<>();
        }
    }

    private AgentConversationMessageVO toMessageVo(AgentConversationMessageEntity entity) {
        AgentConversationMessageVO vo = new AgentConversationMessageVO();
        vo.setId(entity.getMessageId());
        vo.setRole(entity.getRole());
        vo.setMode(entity.getMode());
        vo.setContent(entity.getContent());
        vo.setAction(entity.getAction());
        vo.setCommandId(entity.getCommandId());
        vo.setRiskLevel(entity.getRiskLevel());
        vo.setConfirmationRequired(entity.isConfirmationRequired());
        vo.setPayload(readPayload(entity.getPayloadJson()));
        if (vo.isConfirmationRequired() && vo.getPayload() != null) {
            Object confirmationToken = vo.getPayload().get("confirmationToken");
            if (confirmationToken != null) {
                vo.setConfirmationToken(String.valueOf(confirmationToken));
            }
        }
        vo.setCreatedAt(entity.getCreatedAt());
        return vo;
    }

    private Map<String, Object> readPayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payloadJson, MAP_TYPE);
        } catch (JsonProcessingException ex) {
            return null;
        }
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("AI_CONVERSATION_SERIALIZE_FAILED", "会话数据序列化失败", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String shortText(String text, int maxLength) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String normalized = text.trim().replaceAll("\\s+", " ");
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private AgentConversationVO sanitizeConversationForHistory(AgentConversationVO conversation) {
        return conversation;
    }

    private Map<String, Object> toStreamMessagePayload(String sessionId, AgentConversationMessageVO message) {
        return toStreamMessagePayload(sessionId, UUID.randomUUID().toString(), message);
    }

    private Map<String, Object> toStreamMessagePayload(String sessionId, String messageId, AgentConversationMessageVO message) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sessionId", sessionId);
        payload.put("messageId", messageId);
        payload.put("role", message.getRole());
        payload.put("mode", message.getMode());
        payload.put("content", message.getContent());
        payload.put("action", message.getAction());
        payload.put("commandId", message.getCommandId());
        payload.put("riskLevel", message.getRiskLevel());
        payload.put("confirmationRequired", message.isConfirmationRequired());
        payload.put("confirmationToken", message.getConfirmationToken());
        payload.put("payload", message.getPayload());
        return payload;
    }

    private void sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (Exception ex) {
            throw new BusinessException("AI_STREAM_FAILED", "流式对话失败", HttpStatus.BAD_GATEWAY);
        }
    }

    private void sendErrorEvent(SseEmitter emitter, String code, String message) {
        try {
            emitter.send(SseEmitter.event().name("error").data(Map.of(
                    "code", code,
                    "message", message
            )));
        } catch (Exception ignored) {
            // ignore emitter close failure
        }
    }

    private AgentCommandPreviewRequest toPreviewRequest(String prompt) {
        AgentCommandPreviewRequest request = new AgentCommandPreviewRequest();
        request.setPrompt(prompt);
        return request;
    }

    private AiRuntimeSettings loadRuntimeSettings() {
        AiRuntimeConfig config = aiRuntimeConfigMapper.findSingleton();
        if (config == null) {
            AppAiProperties.Runtime runtime = appAiProperties.getRuntime();
            return new AiRuntimeSettings(
                    runtime.isEnabled(),
                    runtime.getApiBaseUrl(),
                    runtime.getProvider(),
                    runtime.getModel(),
                    runtime.getApiKey(),
                    runtime.getTimeoutMs(),
                    runtime.getMaxTokens(),
                    runtime.getTemperature()
            );
        }
        return new AiRuntimeSettings(
                config.getEnabled() != null && config.getEnabled() == 1,
                config.getApiBaseUrl(),
                config.getProvider(),
                config.getModel(),
                sensitiveConfigCipher.decrypt(config.getApiKeyCiphertext()),
                config.getTimeoutMs() == null ? 30000 : config.getTimeoutMs(),
                config.getMaxTokens() == null ? 4096 : config.getMaxTokens(),
                config.getTemperature() == null ? 0.2D : config.getTemperature()
        );
    }

    private String readText(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? null : value.asText(null);
    }

    private Map<String, Object> toMap(JsonNode node) {
        if (node == null || node.isMissingNode() || !node.isObject()) {
            return new LinkedHashMap<>();
        }
        Map<String, Object> map = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> {
            JsonNode value = entry.getValue();
            if (value.isTextual()) {
                map.put(entry.getKey(), value.asText());
            } else if (value.isIntegralNumber()) {
                map.put(entry.getKey(), value.asLong());
            } else if (value.isFloatingPointNumber()) {
                map.put(entry.getKey(), value.decimalValue());
            } else if (value.isBoolean()) {
                map.put(entry.getKey(), value.asBoolean());
            }
        });
        return map;
    }

    private List<String> toList(JsonNode node) {
        List<String> warnings = new ArrayList<>();
        if (node != null && node.isArray()) {
            node.forEach(item -> warnings.add(item.asText()));
        }
        return warnings;
    }

    private String normalizeRisk(String riskLevel, String action) {
        if (riskLevel != null && List.of("L1", "L2", "L3", "L4").contains(riskLevel)) {
            return riskLevel;
        }
        return switch (action) {
            case "ROOM_DISABLE" -> "L3";
            case "ROOM_CREATE", "PAYMENT_CREATE" -> "L2";
            default -> "L1";
        };
    }

    private String fallbackSummary(String summary, String action) {
        if (summary != null && !summary.isBlank()) {
            return summary;
        }
        return switch (action) {
            case "ROOM_CREATE" -> "创建房间";
            case "ROOM_DISABLE" -> "停用房间";
            case "BILL_DETAIL" -> "查询账单详情";
            case "BILL_LIST_BY_ROOM" -> "按房间查询账单";
            case "PAYMENT_CREATE" -> "创建支付单";
            case "PAYMENT_QUERY" -> "查询支付单";
            default -> "智能指令";
        };
    }

    private String normalizeLocationPart(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.replace(" ", "").replace("栋", "").replace("单元", "");
    }

    private String normalizeRoomNo(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.replace(" ", "").replace("室", "");
    }

    private String normalizeBillStatus(String explicitStatus, String sourceText, Map<String, Object> arguments) {
        String status = explicitStatus;
        if ((status == null || status.isBlank()) && sourceText != null && sourceText.contains("未缴")) {
            status = "ISSUED";
        }
        if ((status == null || status.isBlank()) && arguments.values().stream().anyMatch(value -> value != null && value.toString().contains("未缴"))) {
            status = "ISSUED";
        }
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status.toUpperCase()) {
            case "UNPAID", "ISSUED", "未缴" -> "ISSUED";
            case "PAID", "已缴" -> "PAID";
            default -> status.toUpperCase();
        };
    }

    private String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first : second;
    }

    private Integer asInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString().trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String extractCommunityName(String prompt) {
        return apiV1AgentClient.listAdminCommunities().stream()
                .map(AdminCommunityVO::getName)
                .filter(name -> prompt != null && prompt.contains(name))
                .findFirst()
                .orElse(null);
    }

    private String extractBuildingNo(String prompt) {
        if (prompt == null) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)\\s*栋").matcher(prompt);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractUnitNo(String prompt) {
        if (prompt == null) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(\\d+)\\s*单元").matcher(prompt);
        return matcher.find() ? matcher.group(1) : null;
    }

    private String extractRoomNo(String prompt) {
        if (prompt == null) {
            return null;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("(?:单元\\s*)?(\\d{3,4})(?:室)?").matcher(prompt);
        String roomNo = null;
        while (matcher.find()) {
            roomNo = matcher.group(1);
        }
        return roomNo;
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private BigDecimal asBigDecimal(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }
}
