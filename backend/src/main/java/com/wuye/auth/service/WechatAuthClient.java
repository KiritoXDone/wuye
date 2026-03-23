package com.wuye.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wuye.common.config.AppAuthProperties;
import com.wuye.common.exception.BusinessException;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Duration;

@Component
public class WechatAuthClient {

    private final RestTemplateBuilder restTemplateBuilder;
    private final ObjectMapper objectMapper;
    private final AppAuthProperties appAuthProperties;

    public WechatAuthClient(RestTemplateBuilder restTemplateBuilder,
                            ObjectMapper objectMapper,
                            AppAuthProperties appAuthProperties) {
        this.restTemplateBuilder = restTemplateBuilder;
        this.objectMapper = objectMapper;
        this.appAuthProperties = appAuthProperties;
    }

    public WechatSession exchangeCode(String code) {
        String normalizedCode = code == null ? "" : code.trim();
        if (normalizedCode.isEmpty()) {
            throw new BusinessException("INVALID_ARGUMENT", "微信登录 code 不能为空", HttpStatus.BAD_REQUEST);
        }
        if ("mock".equalsIgnoreCase(appAuthProperties.getWechat().getMode())) {
            return new WechatSession(normalizedCode, null, null);
        }
        return exchangeCodeWithWechat(normalizedCode);
    }

    private WechatSession exchangeCodeWithWechat(String code) {
        String appId = trim(appAuthProperties.getWechat().getAppId());
        String appSecret = trim(appAuthProperties.getWechat().getAppSecret());
        if (appId == null || appSecret == null) {
            throw new BusinessException("INTEGRATION_ERROR", "微信登录配置缺失", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String url = UriComponentsBuilder.fromHttpUrl(appAuthProperties.getWechat().getJscode2sessionUrl())
                .queryParam("appid", appId)
                .queryParam("secret", appSecret)
                .queryParam("js_code", code)
                .queryParam("grant_type", "authorization_code")
                .build(true)
                .toUriString();

        RestTemplate restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode root = objectMapper.readTree(response == null ? "{}" : response);
            if (root.path("errcode").asInt(0) != 0) {
                throw new BusinessException("UNAUTHORIZED", "微信登录失败", HttpStatus.UNAUTHORIZED);
            }
            String openId = trim(root.path("openid").asText(null));
            if (openId == null) {
                throw new BusinessException("INTEGRATION_ERROR", "微信登录返回缺少 openid", HttpStatus.BAD_GATEWAY);
            }
            return new WechatSession(
                    openId,
                    trim(root.path("unionid").asText(null)),
                    trim(root.path("session_key").asText(null))
            );
        } catch (JsonProcessingException ex) {
            throw new BusinessException("INTEGRATION_ERROR", "微信登录响应解析失败", HttpStatus.BAD_GATEWAY);
        } catch (RestClientException ex) {
            throw new BusinessException("INTEGRATION_ERROR", "微信登录服务不可用", HttpStatus.BAD_GATEWAY);
        }
    }

    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public record WechatSession(String openId, String unionId, String sessionKey) {
    }
}
