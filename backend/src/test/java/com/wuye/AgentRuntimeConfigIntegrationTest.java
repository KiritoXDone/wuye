package com.wuye;

import com.wuye.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AgentRuntimeConfigIntegrationTest extends AbstractIntegrationTest {

    @Test
    void agentRuntimeConfigCanBeQueriedAndUpdatedByAdmin() throws Exception {
        mockMvc.perform(get("/api/v1/admin/ai/runtime-config")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.provider").value("openai"))
                .andExpect(jsonPath("$.data.model").value("gpt-4o-mini"));

        mockMvc.perform(get("/api/v1/admin/agent/runtime-config")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.provider").value("openai"))
                .andExpect(jsonPath("$.data.model").value("gpt-4o-mini"));

        mockMvc.perform(put("/api/v1/admin/ai/runtime-config")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
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
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enabled").value(true))
                .andExpect(jsonPath("$.data.apiBaseUrl").value("https://api.example.com/v1"))
                .andExpect(jsonPath("$.data.provider").value("openai-compatible"))
                .andExpect(jsonPath("$.data.model").value("qwen-max"))
                .andExpect(jsonPath("$.data.apiKeyMasked").value("sk-t****1234"))
                .andExpect(jsonPath("$.data.timeoutMs").value(45000))
                .andExpect(jsonPath("$.data.maxTokens").value(8192))
                .andExpect(jsonPath("$.data.temperature").value(0.6));

        mockMvc.perform(put("/api/v1/admin/agent/runtime-config")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "enabled": true,
                                  "apiBaseUrl": "https://api.agent.example.com/v1",
                                  "provider": "agent-provider",
                                  "model": "agent-model-x",
                                  "timeoutMs": 30000,
                                  "maxTokens": 4096,
                                  "temperature": 0.3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.apiBaseUrl").value("https://api.agent.example.com/v1"))
                .andExpect(jsonPath("$.data.provider").value("agent-provider"))
                .andExpect(jsonPath("$.data.model").value("agent-model-x"));

        mockMvc.perform(get("/api/v1/admin/ai/runtime-config")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        mockMvc.perform(get("/api/v1/admin/agent/runtime-config")
                        .header("Authorization", "Bearer " + residentToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
