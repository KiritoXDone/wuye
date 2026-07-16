package com.wuye.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai")
public class AppAiProperties {

    private final Runtime runtime = new Runtime();

    public Runtime getRuntime() {
        return runtime;
    }

    public static class Runtime {
        private boolean enabled;
        private String apiBaseUrl = "https://api.openai.com/v1";
        private String backendBaseUrl = "http://127.0.0.1:8081";
        private String model = "gpt-4o-mini";
        private String apiKey = "";

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

        public String getBackendBaseUrl() {
            return backendBaseUrl;
        }

        public void setBackendBaseUrl(String backendBaseUrl) {
            this.backendBaseUrl = backendBaseUrl;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}
