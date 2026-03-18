package com.wuye.ai.mapper;

import com.wuye.ai.entity.AiRuntimeConfig;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AiRuntimeConfigMapper {

    @Select("""
            SELECT id, enabled, api_base_url, provider, model, api_key_ciphertext, timeout_ms, max_tokens, temperature, created_at, updated_at
            FROM ai_runtime_config
            WHERE id = 1
            """)
    AiRuntimeConfig findSingleton();

    @Insert("""
            INSERT INTO ai_runtime_config(id, enabled, api_base_url, provider, model, api_key_ciphertext, timeout_ms, max_tokens, temperature)
            VALUES(#{id}, #{enabled}, #{apiBaseUrl}, #{provider}, #{model}, #{apiKeyCiphertext}, #{timeoutMs}, #{maxTokens}, #{temperature})
            """)
    @Options(useGeneratedKeys = false)
    int insert(AiRuntimeConfig config);

    @Update("""
            UPDATE ai_runtime_config
            SET enabled = #{enabled},
                api_base_url = #{apiBaseUrl},
                provider = #{provider},
                model = #{model},
                api_key_ciphertext = #{apiKeyCiphertext},
                timeout_ms = #{timeoutMs},
                max_tokens = #{maxTokens},
                temperature = #{temperature},
                updated_at = CURRENT_TIMESTAMP
            WHERE id = #{id}
            """)
    int update(AiRuntimeConfig config);
}
