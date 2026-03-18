package com.wuye.ai.service;

import com.wuye.ai.dto.AiRuntimeConfigUpdateDTO;
import com.wuye.ai.entity.AiRuntimeConfig;
import com.wuye.ai.mapper.AiRuntimeConfigMapper;
import com.wuye.ai.vo.AiRuntimeConfigVO;
import com.wuye.common.config.AppAiProperties;
import com.wuye.common.security.AccessGuard;
import com.wuye.common.security.LoginUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiRuntimeConfigService {

    private static final long SINGLETON_ID = 1L;

    private final AiRuntimeConfigMapper aiRuntimeConfigMapper;
    private final AccessGuard accessGuard;
    private final AppAiProperties appAiProperties;

    public AiRuntimeConfigService(AiRuntimeConfigMapper aiRuntimeConfigMapper,
                                  AccessGuard accessGuard,
                                  AppAiProperties appAiProperties) {
        this.aiRuntimeConfigMapper = aiRuntimeConfigMapper;
        this.accessGuard = accessGuard;
        this.appAiProperties = appAiProperties;
    }

    public AiRuntimeConfigVO get(LoginUser loginUser) {
        accessGuard.requireRole(loginUser, "ADMIN");
        AiRuntimeConfig config = aiRuntimeConfigMapper.findSingleton();
        if (config == null) {
            return fromProperties();
        }
        return toVo(config);
    }

    @Transactional
    public AiRuntimeConfigVO save(LoginUser loginUser, AiRuntimeConfigUpdateDTO dto) {
        accessGuard.requireRole(loginUser, "ADMIN");
        AiRuntimeConfig config = aiRuntimeConfigMapper.findSingleton();
        if (config == null) {
            config = new AiRuntimeConfig();
            config.setId(SINGLETON_ID);
        }
        config.setEnabled(Boolean.TRUE.equals(dto.getEnabled()) ? 1 : 0);
        config.setApiBaseUrl(dto.getApiBaseUrl().trim());
        config.setProvider(dto.getProvider().trim());
        config.setModel(dto.getModel().trim());
        if (dto.getApiKey() != null && !dto.getApiKey().isBlank()) {
            config.setApiKeyCiphertext(dto.getApiKey().trim());
        } else if (config.getApiKeyCiphertext() == null) {
            config.setApiKeyCiphertext(appAiProperties.getRuntime().getApiKey());
        }
        config.setTimeoutMs(dto.getTimeoutMs());
        config.setMaxTokens(dto.getMaxTokens());
        config.setTemperature(dto.getTemperature());
        if (aiRuntimeConfigMapper.findSingleton() == null) {
            aiRuntimeConfigMapper.insert(config);
        } else {
            aiRuntimeConfigMapper.update(config);
        }
        return toVo(aiRuntimeConfigMapper.findSingleton());
    }

    private AiRuntimeConfigVO fromProperties() {
        AppAiProperties.Runtime runtime = appAiProperties.getRuntime();
        AiRuntimeConfigVO vo = new AiRuntimeConfigVO();
        vo.setEnabled(runtime.isEnabled());
        vo.setApiBaseUrl(runtime.getApiBaseUrl());
        vo.setProvider(runtime.getProvider());
        vo.setModel(runtime.getModel());
        vo.setApiKeyMasked(mask(runtime.getApiKey()));
        vo.setTimeoutMs(runtime.getTimeoutMs());
        vo.setMaxTokens(runtime.getMaxTokens());
        vo.setTemperature(runtime.getTemperature());
        return vo;
    }

    private AiRuntimeConfigVO toVo(AiRuntimeConfig config) {
        AiRuntimeConfigVO vo = new AiRuntimeConfigVO();
        vo.setEnabled(config.getEnabled() != null && config.getEnabled() == 1);
        vo.setApiBaseUrl(config.getApiBaseUrl());
        vo.setProvider(config.getProvider());
        vo.setModel(config.getModel());
        vo.setApiKeyMasked(mask(config.getApiKeyCiphertext()));
        vo.setTimeoutMs(config.getTimeoutMs() == null ? 30000 : config.getTimeoutMs());
        vo.setMaxTokens(config.getMaxTokens() == null ? 4096 : config.getMaxTokens());
        vo.setTemperature(config.getTemperature() == null ? 0.2D : config.getTemperature());
        return vo;
    }

    private String mask(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        if (raw.length() <= 8) {
            return "****";
        }
        return raw.substring(0, 4) + "****" + raw.substring(raw.length() - 4);
    }
}
