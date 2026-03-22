package com.wuye.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.common.exception.BusinessException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class OaiChatClient {

    private final RestTemplateBuilder restTemplateBuilder;
    private final ObjectMapper objectMapper;

    public OaiChatClient(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplateBuilder = restTemplateBuilder;
        this.objectMapper = objectMapper;
    }

    public JsonNode completeJson(AiRuntimeSettings settings, String systemPrompt, String userPrompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", settings.model());
        body.put("temperature", settings.temperature());
        body.put("max_tokens", settings.maxTokens());
        body.put("response_format", Map.of("type", "json_object"));
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        try {
            return objectMapper.readTree(sendCompletion(settings, body));
        } catch (JsonProcessingException ex) {
            throw new BusinessException("AI_RESPONSE_INVALID", "LLM 返回结果不是合法 JSON", HttpStatus.BAD_GATEWAY);
        }
    }

    public String completeChat(AiRuntimeSettings settings, List<Map<String, String>> messages) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", settings.model());
        body.put("temperature", settings.temperature());
        body.put("max_tokens", settings.maxTokens());
        body.put("messages", messages);
        return sendCompletion(settings, body);
    }

    public String streamChat(AiRuntimeSettings settings, List<Map<String, String>> messages, Consumer<String> deltaConsumer) {
        validateSettings(settings);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", settings.model());
        body.put("temperature", settings.temperature());
        body.put("max_tokens", settings.maxTokens());
        body.put("stream", true);
        body.put("messages", messages);

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(settings.timeoutMs()))
                .build();
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(settings.apiBaseUrl()) + "/chat/completions"))
                    .timeout(Duration.ofMillis(settings.timeoutMs()))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + settings.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
        } catch (JsonProcessingException ex) {
            throw new BusinessException("AI_REQUEST_INVALID", "构造流式请求失败", HttpStatus.BAD_GATEWAY);
        }

        StringBuilder content = new StringBuilder();
        try {
            HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException("AI_REQUEST_FAILED", "调用 OAI 格式 LLM 失败", HttpStatus.BAD_GATEWAY);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.isBlank() || !line.startsWith("data:")) {
                        continue;
                    }
                    String data = line.substring(5).trim();
                    if ("[DONE]".equals(data)) {
                        break;
                    }
                    JsonNode root = objectMapper.readTree(data);
                    JsonNode deltaNode = root.path("choices").path(0).path("delta").path("content");
                    if (deltaNode.isMissingNode() || deltaNode.isNull()) {
                        continue;
                    }
                    String delta = deltaNode.asText();
                    if (delta.isEmpty()) {
                        continue;
                    }
                    content.append(delta);
                    deltaConsumer.accept(delta);
                }
            }
            if (content.isEmpty()) {
                throw new BusinessException("AI_RESPONSE_INVALID", "LLM 未返回可解析内容", HttpStatus.BAD_GATEWAY);
            }
            return content.toString();
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new BusinessException("AI_REQUEST_FAILED", "调用 OAI 格式 LLM 失败", HttpStatus.BAD_GATEWAY);
        }
    }

    private String sendCompletion(AiRuntimeSettings settings, Map<String, Object> body) {
        validateSettings(settings);
        RestTemplate restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(settings.timeoutMs()))
                .setReadTimeout(Duration.ofMillis(settings.timeoutMs()))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(settings.apiKey());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    normalizeBaseUrl(settings.apiBaseUrl()) + "/chat/completions",
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                throw new BusinessException("AI_RESPONSE_INVALID", "LLM 未返回可解析内容", HttpStatus.BAD_GATEWAY);
            }
            return contentNode.asText();
        } catch (RestClientException ex) {
            throw new BusinessException("AI_REQUEST_FAILED", "调用 OAI 格式 LLM 失败", HttpStatus.BAD_GATEWAY);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("AI_RESPONSE_INVALID", "LLM 返回结果不可解析", HttpStatus.BAD_GATEWAY);
        }
    }

    private void validateSettings(AiRuntimeSettings settings) {
        if (!settings.enabled()) {
            throw new BusinessException("AI_DISABLED", "AI 运行配置未启用", HttpStatus.BAD_REQUEST);
        }
        if (settings.apiBaseUrl() == null || settings.apiBaseUrl().isBlank()) {
            throw new BusinessException("AI_CONFIG_INVALID", "AI API 源未配置", HttpStatus.BAD_REQUEST);
        }
        if (settings.apiKey() == null || settings.apiKey().isBlank()) {
            throw new BusinessException("AI_CONFIG_INVALID", "AI API Key 未配置", HttpStatus.BAD_REQUEST);
        }
        if (settings.model() == null || settings.model().isBlank()) {
            throw new BusinessException("AI_CONFIG_INVALID", "AI 模型未配置", HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizeBaseUrl(String apiBaseUrl) {
        return apiBaseUrl.endsWith("/")
                ? apiBaseUrl.substring(0, apiBaseUrl.length() - 1)
                : apiBaseUrl;
    }
}
