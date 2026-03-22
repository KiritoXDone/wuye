package com.wuye.ai.dto;

import jakarta.validation.constraints.NotBlank;

public class AgentConversationRequest {

    private String sessionId;

    @NotBlank
    private String message;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
