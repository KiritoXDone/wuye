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
        private String provider = "openai";
        private String model = "gpt-4o-mini";
        private String apiKey = "";
        private int timeoutMs = 30000;
        private int maxTokens = 4096;
        private double temperature = 0.2D;

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

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
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

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }

        public double getTemperature() {
            return temperature;
        }

        public void setTemperature(double temperature) {
            this.temperature = temperature;
        }
    }
}
