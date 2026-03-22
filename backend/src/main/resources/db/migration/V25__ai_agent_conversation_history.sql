CREATE TABLE ai_agent_conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) NOT NULL,
    operator_id BIGINT NOT NULL,
    title VARCHAR(128) NULL,
    context_json LONGTEXT NULL,
    last_message_preview VARCHAR(255) NULL,
    message_count INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_agent_conversation_session UNIQUE (session_id),
    INDEX idx_ai_agent_conversation_operator_updated (operator_id, updated_at)
);

CREATE TABLE ai_agent_conversation_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    seq_no INT NOT NULL,
    role VARCHAR(16) NOT NULL,
    mode VARCHAR(32) NULL,
    content LONGTEXT NOT NULL,
    action VARCHAR(64) NULL,
    command_id VARCHAR(64) NULL,
    risk_level VARCHAR(16) NULL,
    confirmation_required TINYINT(1) NOT NULL DEFAULT 0,
    payload_json LONGTEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_ai_agent_conversation_message_message UNIQUE (message_id),
    INDEX idx_ai_agent_conversation_message_session_seq (session_id, seq_no),
    INDEX idx_ai_agent_conversation_message_command (command_id)
);
