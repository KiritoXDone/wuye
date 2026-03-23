package com.wuye;

import com.wuye.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AgentRuntimeConfigIntegrationTest extends AbstractIntegrationTest {

    @Test
    void seededRuntimeConfigCanBeQueriedFromAiAndAgentRoutes() throws Exception {
        assertRuntimeConfigGet("/api/v1/admin/ai/runtime-config", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.provider").value("openai"))
                .andExpect(jsonPath("$.data.model").value("gpt-4o-mini"));

        assertRuntimeConfigGet("/api/v1/admin/agent/runtime-config", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.provider").value("openai"))
                .andExpect(jsonPath("$.data.model").value("gpt-4o-mini"));
    }

    @Test
    void runtimeConfigFallsBackToPropertyDefaultsWhenTableRowIsMissing() throws Exception {
        jdbcTemplate.update("DELETE FROM ai_runtime_config");

        assertRuntimeConfigGet("/api/v1/admin/ai/runtime-config", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.apiBaseUrl").value("https://api.openai.com/v1"))
                .andExpect(jsonPath("$.data.provider").value("openai"))
                .andExpect(jsonPath("$.data.model").value("gpt-4o-mini"))
                .andExpect(jsonPath("$.data.apiKeyMasked").value(""))
                .andExpect(jsonPath("$.data.timeoutMs").value(30000))
                .andExpect(jsonPath("$.data.maxTokens").value(4096))
                .andExpect(jsonPath("$.data.temperature").value(0.2));
    }

    @Test
    void runtimeConfigUpdateIsSharedAcrossAiAndAgentRoutes() throws Exception {
        assertRuntimeConfigPut("/api/v1/admin/ai/runtime-config", adminToken, """
                {
                  "enabled": true,
                  "apiBaseUrl": "https://api.example.com/v1",
                  "provider": "openai-compatible",
                  "model": "qwen-max",
                  "apiKey": "sk-test-secret-1234",
                  "timeoutMs": 45000,
                  "maxTokens": 8192,
                  "temperature": 0.6
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.apiBaseUrl").value("https://api.example.com/v1"))
                .andExpect(jsonPath("$.data.provider").value("openai-compatible"))
                .andExpect(jsonPath("$.data.model").value("qwen-max"))
                .andExpect(jsonPath("$.data.apiKeyMasked").value("sk-t****1234"))
                .andExpect(jsonPath("$.data.timeoutMs").value(45000))
                .andExpect(jsonPath("$.data.maxTokens").value(8192))
                .andExpect(jsonPath("$.data.temperature").value(0.6));

        assertRuntimeConfigGet("/api/v1/admin/agent/runtime-config", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.apiBaseUrl").value("https://api.example.com/v1"))
                .andExpect(jsonPath("$.data.provider").value("openai-compatible"))
                .andExpect(jsonPath("$.data.model").value("qwen-max"))
                .andExpect(jsonPath("$.data.apiKeyMasked").value("sk-t****1234"));

        String storedCiphertext = jdbcTemplate.queryForObject(
                "SELECT api_key_ciphertext FROM ai_runtime_config WHERE id = 1",
                String.class);
        assertThat(storedCiphertext)
                .isNotBlank()
                .startsWith("enc:v1:")
                .doesNotContain("sk-test-secret-1234");

        assertRuntimeConfigPut("/api/v1/admin/agent/runtime-config", adminToken, """
                {
                  "enabled": true,
                  "apiBaseUrl": "https://api.agent.example.com/v1",
                  "provider": "agent-provider",
                  "model": "agent-model-x",
                  "timeoutMs": 30000,
                  "maxTokens": 4096,
                  "temperature": 0.3
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.apiBaseUrl").value("https://api.agent.example.com/v1"))
                .andExpect(jsonPath("$.data.provider").value("agent-provider"))
                .andExpect(jsonPath("$.data.model").value("agent-model-x"))
                .andExpect(jsonPath("$.data.apiKeyMasked").value("sk-t****1234"));

        assertRuntimeConfigGet("/api/v1/admin/ai/runtime-config", adminToken)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.apiBaseUrl").value("https://api.agent.example.com/v1"))
                .andExpect(jsonPath("$.data.provider").value("agent-provider"))
                .andExpect(jsonPath("$.data.model").value("agent-model-x"))
                .andExpect(jsonPath("$.data.apiKeyMasked").value("sk-t****1234"));
    }

    @Test
    void runtimeConfigUpdateKeepsStoredApiKeyWhenBlankApiKeyIsSubmitted() throws Exception {
        assertRuntimeConfigPut("/api/v1/admin/ai/runtime-config", adminToken, """
                {
                  "enabled": true,
                  "apiBaseUrl": "https://api.example.com/v1",
                  "provider": "openai-compatible",
                  "model": "qwen-max",
                  "apiKey": "sk-test-secret-1234",
                  "timeoutMs": 45000,
                  "maxTokens": 8192,
                  "temperature": 0.6
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.apiKeyMasked").value("sk-t****1234"));

        assertRuntimeConfigPut("/api/v1/admin/agent/runtime-config", adminToken, """
                {
                  "enabled": false,
                  "apiBaseUrl": "https://api.agent.example.com/v1",
                  "provider": "agent-provider",
                  "model": "agent-model-x",
                  "apiKey": "   ",
                  "timeoutMs": 30000,
                  "maxTokens": 4096,
                  "temperature": 0.3
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.apiKeyMasked").value("sk-t****1234"));
    }

    @Test
    void residentCannotReadOrUpdateRuntimeConfig() throws Exception {
        assertRuntimeConfigGet("/api/v1/admin/ai/runtime-config", residentToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertRuntimeConfigGet("/api/v1/admin/agent/runtime-config", residentToken)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertRuntimeConfigPut("/api/v1/admin/ai/runtime-config", residentToken, """
                {
                  "enabled": true,
                  "apiBaseUrl": "https://api.example.com/v1",
                  "provider": "openai-compatible",
                  "model": "qwen-max",
                  "apiKey": "sk-test-secret-1234",
                  "timeoutMs": 45000,
                  "maxTokens": 8192,
                  "temperature": 0.6
                }
                """)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        assertRuntimeConfigPut("/api/v1/admin/agent/runtime-config", residentToken, """
                {
                  "enabled": true,
                  "apiBaseUrl": "https://api.agent.example.com/v1",
                  "provider": "agent-provider",
                  "model": "agent-model-x",
                  "timeoutMs": 30000,
                  "maxTokens": 4096,
                  "temperature": 0.3
                }
                """)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void invalidRuntimeConfigPayloadIsRejected() throws Exception {
        assertRuntimeConfigPut("/api/v1/admin/ai/runtime-config", adminToken, """
                {
                  "enabled": true,
                  "apiBaseUrl": " ",
                  "provider": "openai-compatible",
                  "model": "qwen-max",
                  "timeoutMs": 999999,
                  "maxTokens": 0,
                  "temperature": 3
                }
                """)
                .andExpect(status().isUnprocessableEntity());
    }

    private ResultActions assertRuntimeConfigGet(String path, String token) throws Exception {
        return mockMvc.perform(get(path)
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions assertRuntimeConfigPut(String path, String token, String body) throws Exception {
        return mockMvc.perform(put(path)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }
}
