package com.wuye.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.ai.vo.AgentConversationListItemVO;
import com.wuye.ai.vo.AgentConversationVO;
import com.wuye.common.api.PageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Service
@ConditionalOnProperty(prefix = "app.infra.redis", name = "enabled", havingValue = "true")
public class AgentConversationCacheService {

    private static final Logger log = LoggerFactory.getLogger(AgentConversationCacheService.class);
    private static final Duration DETAIL_TTL = Duration.ofMinutes(15);
    private static final Duration LIST_TTL = Duration.ofMinutes(3);
    private static final Duration COMMAND_TTL = Duration.ofMinutes(10);

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public AgentConversationCacheService(StringRedisTemplate stringRedisTemplate, ObjectMapper objectMapper) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<AgentConversationVO> getConversationDetail(String sessionId) {
        return readJson(detailKey(sessionId), new TypeReference<>() {});
    }

    public void cacheConversationDetail(String sessionId, AgentConversationVO conversation) {
        writeJson(detailKey(sessionId), conversation, DETAIL_TTL);
    }

    public void evictConversationDetail(String sessionId) {
        delete(detailKey(sessionId));
    }

    public Optional<PageResponse<AgentConversationListItemVO>> getConversationList(Long operatorId, int pageNo, int pageSize) {
        return readJson(listKey(operatorId, pageNo, pageSize), new TypeReference<>() {});
    }

    public void cacheConversationList(Long operatorId, int pageNo, int pageSize, PageResponse<AgentConversationListItemVO> page) {
        writeJson(listKey(operatorId, pageNo, pageSize), page, LIST_TTL);
    }

    public void evictConversationList(Long operatorId, int pageNo, int pageSize) {
        delete(listKey(operatorId, pageNo, pageSize));
    }

    public void saveCommandConfirmation(String commandId, Map<String, Object> payload) {
        writeJson(commandKey(commandId), payload, COMMAND_TTL);
    }

    public Optional<Map<String, Object>> getCommandConfirmation(String commandId) {
        return readJson(commandKey(commandId), new TypeReference<>() {});
    }

    public void deleteCommandConfirmation(String commandId) {
        delete(commandKey(commandId));
    }

    private String detailKey(String sessionId) {
        return "ai:conv:detail:" + sessionId;
    }

    private String listKey(Long operatorId, int pageNo, int pageSize) {
        return "ai:conv:list:" + operatorId + ":" + pageNo + ":" + pageSize;
    }

    private String commandKey(String commandId) {
        return "ai:cmd:confirm:" + commandId;
    }

    private void delete(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (RuntimeException ex) {
            log.warn("agent redis delete degraded for key {}", key, ex);
        }
    }

    private void writeJson(String key, Object value, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(value), ttl);
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("agent redis write degraded for key {}", key, ex);
        }
    }

    private <T> Optional<T> readJson(String key, TypeReference<T> typeReference) {
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) {
                return Optional.empty();
            }
            return Optional.ofNullable(objectMapper.readValue(value, typeReference));
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("agent redis read degraded for key {}", key, ex);
            return Optional.empty();
        }
    }
}
