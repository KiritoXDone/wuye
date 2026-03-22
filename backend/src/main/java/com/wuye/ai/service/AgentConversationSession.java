package com.wuye.ai.service;

import com.wuye.ai.vo.AgentConversationMessageVO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AgentConversationSession {

    private final String sessionId;
    private final Long operatorId;
    private final List<AgentConversationMessageVO> messages = new ArrayList<>();
    private final Map<String, Object> context = new LinkedHashMap<>();
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AgentConversationSession(String sessionId, Long operatorId, LocalDateTime createdAt) {
        this.sessionId = sessionId;
        this.operatorId = operatorId;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public List<AgentConversationMessageVO> getMessages() {
        return messages;
    }

    public Map<String, Object> getContext() {
        return context;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
