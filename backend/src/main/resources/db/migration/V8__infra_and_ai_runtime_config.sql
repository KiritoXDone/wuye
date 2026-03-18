CREATE TABLE ai_runtime_config (
    id bigint PRIMARY KEY,
    enabled tinyint NOT NULL DEFAULT 0,
    api_base_url varchar(255) NOT NULL,
    provider varchar(64) NOT NULL,
    model varchar(128) NOT NULL,
    api_key_ciphertext varchar(512) NULL,
    timeout_ms int NOT NULL,
    max_tokens int NOT NULL,
    temperature decimal(4,2) NOT NULL,
    created_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at datetime NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO ai_runtime_config(id, enabled, api_base_url, provider, model, api_key_ciphertext, timeout_ms, max_tokens, temperature)
VALUES (1, 0, 'https://api.openai.com/v1', 'openai', 'gpt-4o-mini', NULL, 30000, 4096, 0.20);
