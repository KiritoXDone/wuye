package com.wuye.ai.service;

public record AiRuntimeSettings(boolean enabled,
                                String apiBaseUrl,
                                String model,
                                String apiKey) {
}
