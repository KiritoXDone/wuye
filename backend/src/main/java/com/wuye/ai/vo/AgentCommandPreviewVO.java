package com.wuye.ai.vo;

import java.util.List;
import java.util.Map;

public class AgentCommandPreviewVO {

    private String commandId;
    private String originalPrompt;
    private String normalizedPrompt;
    private String action;
    private String summary;
    private String riskLevel;
    private boolean confirmationRequired;
    private String confirmationToken;
    private boolean executable;
    private String message;
    private Map<String, Object> parsedArguments;
    private Map<String, Object> resolvedContext;
    private List<String> warnings;

    public String getCommandId() {
        return commandId;
    }

    public void setCommandId(String commandId) {
        this.commandId = commandId;
    }

    public String getOriginalPrompt() {
        return originalPrompt;
    }

    public void setOriginalPrompt(String originalPrompt) {
        this.originalPrompt = originalPrompt;
    }

    public String getNormalizedPrompt() {
        return normalizedPrompt;
    }

    public void setNormalizedPrompt(String normalizedPrompt) {
        this.normalizedPrompt = normalizedPrompt;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public boolean isConfirmationRequired() {
        return confirmationRequired;
    }

    public void setConfirmationRequired(boolean confirmationRequired) {
        this.confirmationRequired = confirmationRequired;
    }

    public String getConfirmationToken() {
        return confirmationToken;
    }

    public void setConfirmationToken(String confirmationToken) {
        this.confirmationToken = confirmationToken;
    }

    public boolean isExecutable() {
        return executable;
    }

    public void setExecutable(boolean executable) {
        this.executable = executable;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Map<String, Object> getParsedArguments() {
        return parsedArguments;
    }

    public void setParsedArguments(Map<String, Object> parsedArguments) {
        this.parsedArguments = parsedArguments;
    }

    public Map<String, Object> getResolvedContext() {
        return resolvedContext;
    }

    public void setResolvedContext(Map<String, Object> resolvedContext) {
        this.resolvedContext = resolvedContext;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }
}
