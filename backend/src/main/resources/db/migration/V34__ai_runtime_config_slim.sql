ALTER TABLE ai_runtime_config
    DROP COLUMN provider,
    DROP COLUMN timeout_ms,
    DROP COLUMN max_tokens,
    DROP COLUMN temperature;
