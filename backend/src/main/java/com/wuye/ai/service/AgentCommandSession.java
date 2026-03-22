package com.wuye.ai.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class AgentCommandSession {

    private final String commandId;
    private final Long operatorId;
    private final String originalPrompt;
    private final String normalizedPrompt;
    private final String action;
    private final String summary;
    private final String riskLevel;
    private final boolean confirmationRequired;
    private final String confirmationToken;
    private final Map<String, Object> parsedArguments;
    private final Map<String, Object> resolvedContext;
    private final List<String> warnings;
    private final LocalDateTime createdAt;
    private Object result;
    private String status;

    public AgentCommandSession(String commandId,
                               Long operatorId,
                               String originalPrompt,
                               String normalizedPrompt,
                               String action,
                               String summary,
                               String riskLevel,
                               boolean confirmationRequired,
                               String confirmationToken,
                               Map<String, Object> parsedArguments,
                               Map<String, Object> resolvedContext,
                               List<String> warnings,
                               LocalDateTime createdAt,
                               String status) {
        this.commandId = commandId;
        this.operatorId = operatorId;
        this.originalPrompt = originalPrompt;
        this.normalizedPrompt = normalizedPrompt;
        this.action = action;
        this.summary = summary;
        this.riskLevel = riskLevel;
        this.confirmationRequired = confirmationRequired;
        this.confirmationToken = confirmationToken;
        this.parsedArguments = parsedArguments;
        this.resolvedContext = resolvedContext;
        this.warnings = warnings;
        this.createdAt = createdAt;
        this.status = status;
    }

    public String getCommandId() {
        return commandId;
    }

    public Long getOperatorId() {
        return operatorId;
    }

    public String getOriginalPrompt() {
        return originalPrompt;
    }

    public String getNormalizedPrompt() {
        return normalizedPrompt;
    }

    public String getAction() {
        return action;
    }

    public String getSummary() {
        return summary;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public String getConfirmationToken() {
        return confirmationToken;
    }

    public Map<String, Object> getParsedArguments() {
        return parsedArguments;
    }

    public Map<String, Object> getResolvedContext() {
        return resolvedContext;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
