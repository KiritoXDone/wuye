package com.wuye.ai.dto;

import jakarta.validation.constraints.NotBlank;

public class AgentCommandConfirmRequest {

    @NotBlank
    private String confirmationToken;

    public String getConfirmationToken() {
        return confirmationToken;
    }

    public void setConfirmationToken(String confirmationToken) {
        this.confirmationToken = confirmationToken;
    }
}
