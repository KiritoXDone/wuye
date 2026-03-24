package com.wuye.ai.vo;

public class AgentCommandExecutionVO {

    private String commandId;
    private String status;
    private String action;
    private String riskLevel;
    private String summary;
    private String resultSummary;
    private String resultMarkdown;
    private Object result;

    public String getCommandId() {
        return commandId;
    }

    public void setCommandId(String commandId) {
        this.commandId = commandId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getResultSummary() {
        return resultSummary;
    }

    public void setResultSummary(String resultSummary) {
        this.resultSummary = resultSummary;
    }

    public String getResultMarkdown() {
        return resultMarkdown;
    }

    public void setResultMarkdown(String resultMarkdown) {
        this.resultMarkdown = resultMarkdown;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }
}
