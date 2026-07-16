package com.wuye.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.common.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

@Component
public class OaiChatClient {

    private static final Logger log = LoggerFactory.getLogger(OaiChatClient.class);

    private static final long DEFAULT_TIMEOUT_MS = 60000L;

    private final ObjectMapper objectMapper;

    public OaiChatClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode completeJson(AiRuntimeSettings settings, String systemPrompt, String userPrompt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", settings.model());
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
        body.put("messages", messages);
        return sendCompletion(settings, body);
    }

    public String streamChat(AiRuntimeSettings settings, List<Map<String, String>> messages, Consumer<String> deltaConsumer) {
        validateSettings(settings);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", settings.model());
        body.put("stream", true);
        body.put("messages", messages);

        URI endpoint = completionEndpoint(settings);
        HttpClient client = createHttpClient(endpoint);
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(endpoint)
                    .timeout(Duration.ofMillis(DEFAULT_TIMEOUT_MS))
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
                String errorBody = "";
                try (InputStream errStream = response.body()) {
                    errorBody = new String(errStream.readAllBytes(), StandardCharsets.UTF_8);
                } catch (IOException ignored) {
                    // ignore body read failure during error reporting
                }
                log.error("[AI-DIAG] streamChat non-2xx status={} url={} model={} body={}",
                        response.statusCode(),
                        normalizeBaseUrl(settings.apiBaseUrl()) + "/chat/completions",
                        settings.model(),
                        errorBody);
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
            log.error("[AI-DIAG] streamChat transport failure type={} timeoutMs={} url={} msg={}",
                    ex.getClass().getName(),
                    DEFAULT_TIMEOUT_MS,
                    normalizeBaseUrl(settings.apiBaseUrl()) + "/chat/completions",
                    ex.getMessage(), ex);
            throw new BusinessException("AI_REQUEST_FAILED", "调用 OAI 格式 LLM 失败", HttpStatus.BAD_GATEWAY);
        }
    }

    private String sendCompletion(AiRuntimeSettings settings, Map<String, Object> body) {
        validateSettings(settings);
        URI endpoint = completionEndpoint(settings);
        HttpRequest request;
        try {
            request = HttpRequest.newBuilder()
                    .uri(endpoint)
                    .timeout(Duration.ofMillis(DEFAULT_TIMEOUT_MS))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + settings.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body), StandardCharsets.UTF_8))
                    .build();
        } catch (JsonProcessingException ex) {
            throw new BusinessException("AI_REQUEST_INVALID", "构造 LLM 请求失败", HttpStatus.BAD_GATEWAY);
        }

        try {
            HttpResponse<String> response = createHttpClient(endpoint).send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                log.error("[AI-DIAG] sendCompletion non-2xx status={} url={} model={} body={}",
                        response.statusCode(), endpoint, settings.model(), response.body());
                throw new BusinessException("AI_REQUEST_FAILED", "调用 OAI 格式 LLM 失败", HttpStatus.BAD_GATEWAY);
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
                throw new BusinessException("AI_RESPONSE_INVALID", "LLM 未返回可解析内容", HttpStatus.BAD_GATEWAY);
            }
            return contentNode.asText();
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.error("[AI-DIAG] sendCompletion failure type={} timeoutMs={} url={} model={} msg={} body={}",
                    ex.getClass().getName(),
                    DEFAULT_TIMEOUT_MS,
                    endpoint,
                    settings.model(),
                    ex.getMessage(),
                    "", ex);
            throw new BusinessException("AI_REQUEST_FAILED", "调用 OAI 格式 LLM 失败", HttpStatus.BAD_GATEWAY);
        }
    }

    private URI completionEndpoint(AiRuntimeSettings settings) {
        return URI.create(normalizeBaseUrl(settings.apiBaseUrl()) + "/chat/completions");
    }

    private HttpClient createHttpClient(URI target) {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(DEFAULT_TIMEOUT_MS));
        proxyAddress(target, System.getenv()).ifPresent(address -> builder.proxy(ProxySelector.of(address)));
        return builder.build();
    }

    static Optional<InetSocketAddress> proxyAddress(URI target, Map<String, String> environment) {
        if (isNoProxyHost(target.getHost(), firstPresent(environment, "NO_PROXY", "no_proxy"))) {
            return Optional.empty();
        }
        String rawProxy = "https".equalsIgnoreCase(target.getScheme())
                ? firstPresent(environment, "HTTPS_PROXY", "https_proxy", "HTTP_PROXY", "http_proxy")
                : firstPresent(environment, "HTTP_PROXY", "http_proxy");
        if (rawProxy == null || rawProxy.isBlank()) {
            return Optional.empty();
        }
        try {
            URI proxyUri = URI.create(rawProxy.contains("://") ? rawProxy : "http://" + rawProxy);
            if (proxyUri.getHost() == null) {
                return Optional.empty();
            }
            int port = proxyUri.getPort() > 0
                    ? proxyUri.getPort()
                    : "https".equalsIgnoreCase(proxyUri.getScheme()) ? 443 : 80;
            return Optional.of(InetSocketAddress.createUnresolved(proxyUri.getHost(), port));
        } catch (IllegalArgumentException ex) {
            log.warn("Ignoring invalid proxy URI from environment");
            return Optional.empty();
        }
    }

    private static boolean isNoProxyHost(String host, String noProxy) {
        if (host == null || noProxy == null || noProxy.isBlank()) {
            return false;
        }
        String normalizedHost = host.toLowerCase(Locale.ROOT);
        for (String rawEntry : noProxy.split(",")) {
            String entry = rawEntry.trim().toLowerCase(Locale.ROOT);
            int portSeparator = entry.lastIndexOf(':');
            if (portSeparator > 0 && entry.indexOf(':') == portSeparator) {
                entry = entry.substring(0, portSeparator);
            }
            if ("*".equals(entry)) {
                return true;
            }
            if (entry.startsWith("*.")) {
                entry = entry.substring(2);
            } else if (entry.startsWith(".")) {
                entry = entry.substring(1);
            }
            if (!entry.isEmpty() && (normalizedHost.equals(entry) || normalizedHost.endsWith("." + entry))) {
                return true;
            }
        }
        return false;
    }

    private static String firstPresent(Map<String, String> environment, String... keys) {
        for (String key : keys) {
            String value = environment.get(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
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
