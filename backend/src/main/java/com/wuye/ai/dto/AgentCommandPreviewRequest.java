package com.wuye.ai.dto;

import jakarta.validation.constraints.NotBlank;

public class AgentCommandPreviewRequest {

    @NotBlank
    private String prompt;

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
