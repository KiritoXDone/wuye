package com.wuye.ai.vo;

public class AiRuntimeConfigVO {

    private boolean enabled;
    private String apiBaseUrl;
    private String model;
    private String apiKeyMasked;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getApiKeyMasked() {
        return apiKeyMasked;
    }

    public void setApiKeyMasked(String apiKeyMasked) {
        this.apiKeyMasked = apiKeyMasked;
    }
}
