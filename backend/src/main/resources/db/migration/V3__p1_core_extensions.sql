CREATE TABLE user_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    scope_type VARCHAR(16) NOT NULL,
    community_id BIGINT NOT NULL,
    status INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (group_code),
    CONSTRAINT fk_user_group_community FOREIGN KEY (community_id) REFERENCES community (id)
);

CREATE TABLE group_room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (group_id, room_id),
    CONSTRAINT fk_group_room_group FOREIGN KEY (group_id) REFERENCES user_group (id),
    CONSTRAINT fk_group_room_room FOREIGN KEY (room_id) REFERENCES room (id)
);

CREATE TABLE agent_profile (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    agent_code VARCHAR(64) NOT NULL,
    org_name VARCHAR(128),
    status INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (account_id),
    UNIQUE (agent_code),
    CONSTRAINT fk_agent_profile_account FOREIGN KEY (account_id) REFERENCES account (id)
);

CREATE TABLE agent_group (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    permission VARCHAR(16) NOT NULL,
    status INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (agent_id, group_id),
    CONSTRAINT fk_agent_group_agent FOREIGN KEY (agent_id) REFERENCES agent_profile (id),
    CONSTRAINT fk_agent_group_group FOREIGN KEY (group_id) REFERENCES user_group (id)
);

ALTER TABLE bill ADD COLUMN group_id BIGINT NULL;
ALTER TABLE bill ADD CONSTRAINT fk_bill_group FOREIGN KEY (group_id) REFERENCES user_group (id);

CREATE TABLE coupon_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_code VARCHAR(64) NOT NULL,
    type VARCHAR(16) NOT NULL,
    fee_type VARCHAR(16),
    name VARCHAR(128) NOT NULL,
    discount_mode VARCHAR(16) NOT NULL,
    value_amount DECIMAL(12, 2) NOT NULL,
    threshold_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    valid_from TIMESTAMP NOT NULL,
    valid_to TIMESTAMP NOT NULL,
    stackable INT NOT NULL DEFAULT 0,
    status INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (template_code)
);

CREATE TABLE coupon_issue_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    rule_name VARCHAR(128) NOT NULL,
    fee_type VARCHAR(16) NOT NULL,
    template_id BIGINT NOT NULL,
    min_pay_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    reward_count INT NOT NULL DEFAULT 1,
    status INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_coupon_issue_rule_template FOREIGN KEY (template_id) REFERENCES coupon_template (id)
);

CREATE TABLE coupon_instance (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL,
    owner_account_id BIGINT,
    owner_group_id BIGINT,
    source_type VARCHAR(16) NOT NULL,
    source_ref_no VARCHAR(64),
    status VARCHAR(16) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_coupon_instance_template FOREIGN KEY (template_id) REFERENCES coupon_template (id),
    CONSTRAINT fk_coupon_instance_account FOREIGN KEY (owner_account_id) REFERENCES account (id),
    CONSTRAINT fk_coupon_instance_group FOREIGN KEY (owner_group_id) REFERENCES user_group (id)
);

CREATE TABLE coupon_redemption (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_instance_id BIGINT NOT NULL,
    redeem_type VARCHAR(16) NOT NULL,
    pay_order_no VARCHAR(64),
    redeem_target VARCHAR(128),
    redeemed_at TIMESTAMP NOT NULL,
    operator_id BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (coupon_instance_id),
    CONSTRAINT fk_coupon_redemption_instance FOREIGN KEY (coupon_instance_id) REFERENCES coupon_instance (id),
    CONSTRAINT fk_coupon_redemption_pay_order FOREIGN KEY (pay_order_no) REFERENCES pay_order (pay_order_no),
    CONSTRAINT fk_coupon_redemption_operator FOREIGN KEY (operator_id) REFERENCES account (id)
);

ALTER TABLE pay_order ADD CONSTRAINT fk_pay_order_coupon_instance FOREIGN KEY (coupon_instance_id) REFERENCES coupon_instance (id);

CREATE TABLE import_batch (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_no VARCHAR(64) NOT NULL,
    import_type VARCHAR(16) NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    status VARCHAR(16) NOT NULL,
    total_count INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    fail_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (batch_no)
);

CREATE TABLE import_row_error (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    batch_id BIGINT NOT NULL,
    row_no INT NOT NULL,
    error_code VARCHAR(32) NOT NULL,
    error_message VARCHAR(255) NOT NULL,
    raw_data VARCHAR(4000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_import_row_error_batch FOREIGN KEY (batch_id) REFERENCES import_batch (id)
);

CREATE TABLE export_job (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    export_type VARCHAR(16) NOT NULL,
    request_json VARCHAR(4000),
    file_url VARCHAR(255),
    status VARCHAR(16) NOT NULL,
    expired_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bill_group_period_status ON bill (group_id, period_year, period_month, status);
CREATE INDEX idx_agent_group_account_status ON agent_group (agent_id, status, group_id);
CREATE INDEX idx_coupon_instance_owner_status ON coupon_instance (owner_account_id, status, template_id, expires_at);
CREATE INDEX idx_coupon_instance_source ON coupon_instance (source_type, source_ref_no);
CREATE INDEX idx_import_batch_status ON import_batch (status, created_at);
CREATE INDEX idx_export_job_status ON export_job (status, created_at);
