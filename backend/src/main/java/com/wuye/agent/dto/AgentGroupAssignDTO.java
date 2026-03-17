package com.wuye.agent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class AgentGroupAssignDTO {

    @NotBlank(message = "agentCode 不能为空")
    private String agentCode;
    @NotBlank(message = "groupCode 不能为空")
    private String groupCode;
    @NotBlank(message = "permission 不能为空")
    private String permission;
    @NotNull(message = "status 不能为空")
    private Integer status;

    public String getAgentCode() {
        return agentCode;
    }

    public void setAgentCode(String agentCode) {
        this.agentCode = agentCode;
    }

    public String getGroupCode() {
        return groupCode;
    }

    public void setGroupCode(String groupCode) {
        this.groupCode = groupCode;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
