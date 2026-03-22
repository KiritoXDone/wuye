package com.wuye.ai.service;

public record AiRuntimeSettings(boolean enabled,
                                String apiBaseUrl,
                                String provider,
                                String model,
                                String apiKey,
                                int timeoutMs,
                                int maxTokens,
                                double temperature) {
}
