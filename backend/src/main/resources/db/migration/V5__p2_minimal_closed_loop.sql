ALTER TABLE fee_rule ADD COLUMN pricing_mode VARCHAR(16) NOT NULL DEFAULT 'FLAT';
ALTER TABLE fee_rule ADD COLUMN abnormal_abs_threshold DECIMAL(12, 3) NULL;
ALTER TABLE fee_rule ADD COLUMN abnormal_multiplier_threshold DECIMAL(10, 2) NULL;

CREATE TABLE fee_rule_water_tier (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    fee_rule_id BIGINT NOT NULL,
    tier_order INT NOT NULL,
    start_usage DECIMAL(12, 3) NOT NULL,
    end_usage DECIMAL(12, 3) NULL,
    unit_price DECIMAL(10, 4) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (fee_rule_id, tier_order),
    CONSTRAINT fk_fee_rule_water_tier_rule FOREIGN KEY (fee_rule_id) REFERENCES fee_rule (id)
);

CREATE TABLE org_unit (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    tenant_code VARCHAR(64) NOT NULL,
    org_code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    parent_id BIGINT NULL,
    community_id BIGINT NULL,
    status INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (tenant_code, org_code),
    CONSTRAINT fk_org_unit_parent FOREIGN KEY (parent_id) REFERENCES org_unit (id),
    CONSTRAINT fk_org_unit_community FOREIGN KEY (community_id) REFERENCES community (id)
);

ALTER TABLE user_group ADD COLUMN org_unit_id BIGINT NULL;
ALTER TABLE user_group ADD CONSTRAINT fk_user_group_org_unit FOREIGN KEY (org_unit_id) REFERENCES org_unit (id);

ALTER TABLE agent_profile ADD COLUMN org_unit_id BIGINT NULL;
ALTER TABLE agent_profile ADD CONSTRAINT fk_agent_profile_org_unit FOREIGN KEY (org_unit_id) REFERENCES org_unit (id);

CREATE TABLE water_usage_alert (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    reading_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    alert_code VARCHAR(32) NOT NULL,
    alert_message VARCHAR(255) NOT NULL,
    threshold_value DECIMAL(12, 3) NULL,
    actual_value DECIMAL(12, 3) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_water_usage_alert_reading FOREIGN KEY (reading_id) REFERENCES water_meter_reading (id),
    CONSTRAINT fk_water_usage_alert_room FOREIGN KEY (room_id) REFERENCES room (id)
);

CREATE TABLE dunning_task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_no VARCHAR(64) NOT NULL,
    bill_id BIGINT NOT NULL,
    group_id BIGINT NULL,
    org_unit_id BIGINT NULL,
    tenant_code VARCHAR(64) NULL,
    trigger_type VARCHAR(16) NOT NULL,
    trigger_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL,
    remark VARCHAR(255),
    executed_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (bill_id, trigger_type, trigger_date),
    UNIQUE (task_no),
    CONSTRAINT fk_dunning_task_bill FOREIGN KEY (bill_id) REFERENCES bill (id),
    CONSTRAINT fk_dunning_task_group FOREIGN KEY (group_id) REFERENCES user_group (id),
    CONSTRAINT fk_dunning_task_org_unit FOREIGN KEY (org_unit_id) REFERENCES org_unit (id)
);

CREATE TABLE dunning_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    bill_id BIGINT NOT NULL,
    send_channel VARCHAR(16) NOT NULL,
    status VARCHAR(16) NOT NULL,
    content VARCHAR(500) NOT NULL,
    sent_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dunning_log_task FOREIGN KEY (task_id) REFERENCES dunning_task (id),
    CONSTRAINT fk_dunning_log_bill FOREIGN KEY (bill_id) REFERENCES bill (id)
);

CREATE TABLE payment_voucher (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pay_order_no VARCHAR(64) NOT NULL,
    bill_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    voucher_no VARCHAR(64) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    status VARCHAR(16) NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    content_json VARCHAR(4000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (pay_order_no),
    UNIQUE (voucher_no),
    CONSTRAINT fk_payment_voucher_pay_order FOREIGN KEY (pay_order_no) REFERENCES pay_order (pay_order_no),
    CONSTRAINT fk_payment_voucher_bill FOREIGN KEY (bill_id) REFERENCES bill (id),
    CONSTRAINT fk_payment_voucher_account FOREIGN KEY (account_id) REFERENCES account (id)
);

CREATE TABLE invoice_application (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_no VARCHAR(64) NOT NULL,
    bill_id BIGINT NOT NULL,
    pay_order_no VARCHAR(64) NOT NULL,
    account_id BIGINT NOT NULL,
    invoice_title VARCHAR(128) NOT NULL,
    tax_no VARCHAR(64),
    status VARCHAR(16) NOT NULL,
    remark VARCHAR(255),
    applied_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (application_no),
    CONSTRAINT fk_invoice_application_bill FOREIGN KEY (bill_id) REFERENCES bill (id),
    CONSTRAINT fk_invoice_application_pay_order FOREIGN KEY (pay_order_no) REFERENCES pay_order (pay_order_no),
    CONSTRAINT fk_invoice_application_account FOREIGN KEY (account_id) REFERENCES account (id)
);

CREATE INDEX idx_fee_rule_pricing_mode ON fee_rule (community_id, fee_type, pricing_mode, status);
CREATE INDEX idx_fee_rule_water_tier_rule ON fee_rule_water_tier (fee_rule_id, tier_order);
CREATE INDEX idx_org_unit_tenant_parent ON org_unit (tenant_code, parent_id, status);
CREATE INDEX idx_water_usage_alert_room_status ON water_usage_alert (room_id, status, created_at);
CREATE INDEX idx_dunning_task_group_status ON dunning_task (group_id, status, trigger_date);
CREATE INDEX idx_dunning_task_tenant_status ON dunning_task (tenant_code, status, trigger_date);
CREATE INDEX idx_dunning_log_bill_sent ON dunning_log (bill_id, sent_at);
CREATE INDEX idx_payment_voucher_account_created ON payment_voucher (account_id, created_at);
CREATE INDEX idx_invoice_application_account_status ON invoice_application (account_id, status, applied_at);

INSERT INTO org_unit (id, tenant_code, org_code, name, parent_id, community_id, status)
VALUES (10001, 'TENANT-DEMO', 'ROOT', '演示租户', NULL, NULL, 1),
       (10002, 'TENANT-DEMO', 'COMM-001-SERVICE', '阳光花园服务中心', 10001, 100, 1);

UPDATE user_group
SET org_unit_id = 10002
WHERE id = 5001;

UPDATE agent_profile
SET org_unit_id = 10002
WHERE id = 70001;

UPDATE bill b
SET group_id = (
    SELECT MIN(gr.group_id)
    FROM group_room gr
    WHERE gr.room_id = b.room_id
)
WHERE b.group_id IS NULL;
