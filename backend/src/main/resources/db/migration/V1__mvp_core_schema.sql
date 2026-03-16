CREATE TABLE IF NOT EXISTS community (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    community_code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    status INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (community_code)
);

CREATE TABLE IF NOT EXISTS account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_no VARCHAR(64) NOT NULL,
    account_type VARCHAR(16) NOT NULL,
    username VARCHAR(64),
    password_hash VARCHAR(255),
    nickname VARCHAR(64),
    mobile VARCHAR(20),
    real_name VARCHAR(64),
    avatar_url VARCHAR(255),
    status INT NOT NULL DEFAULT 1,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (account_no),
    UNIQUE (username),
    UNIQUE (mobile)
);

CREATE TABLE IF NOT EXISTS account_identity (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    platform VARCHAR(16) NOT NULL,
    open_id VARCHAR(128),
    union_id VARCHAR(128),
    platform_user_id VARCHAR(128),
    status INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (platform, open_id),
    UNIQUE (platform, platform_user_id),
    CONSTRAINT fk_identity_account FOREIGN KEY (account_id) REFERENCES account (id)
);

CREATE TABLE IF NOT EXISTS room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    community_id BIGINT NOT NULL,
    building_no VARCHAR(32) NOT NULL,
    unit_no VARCHAR(32) NOT NULL,
    room_no VARCHAR(32) NOT NULL,
    area_m2 DECIMAL(10, 2) NOT NULL,
    status INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (community_id, building_no, unit_no, room_no),
    CONSTRAINT fk_room_community FOREIGN KEY (community_id) REFERENCES community (id)
);

CREATE TABLE IF NOT EXISTS account_room (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    account_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    relation_type VARCHAR(16),
    status VARCHAR(16) NOT NULL,
    bind_source VARCHAR(16) NOT NULL,
    confirmed_at TIMESTAMP NULL,
    remark VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (account_id, room_id),
    CONSTRAINT fk_account_room_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT fk_account_room_room FOREIGN KEY (room_id) REFERENCES room (id)
);

CREATE TABLE IF NOT EXISTS fee_rule (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    community_id BIGINT NOT NULL,
    fee_type VARCHAR(16) NOT NULL,
    rule_name VARCHAR(128) NOT NULL,
    unit_price DECIMAL(10, 4) NOT NULL,
    cycle_type VARCHAR(16) NOT NULL DEFAULT 'MONTH',
    effective_from DATE NOT NULL,
    effective_to DATE NULL,
    status INT NOT NULL DEFAULT 1,
    remark VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fee_rule_community FOREIGN KEY (community_id) REFERENCES community (id)
);

CREATE TABLE IF NOT EXISTS water_meter (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    meter_no VARCHAR(64),
    install_at DATE NULL,
    status INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (room_id),
    CONSTRAINT fk_water_meter_room FOREIGN KEY (room_id) REFERENCES room (id)
);

CREATE TABLE IF NOT EXISTS water_meter_reading (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    meter_id BIGINT NOT NULL,
    period_year SMALLINT NOT NULL,
    period_month TINYINT NOT NULL,
    prev_reading DECIMAL(12, 3) NOT NULL,
    curr_reading DECIMAL(12, 3) NOT NULL,
    usage_amount DECIMAL(12, 3) NOT NULL,
    read_by_admin_id BIGINT NOT NULL,
    read_at TIMESTAMP NOT NULL,
    photo_url VARCHAR(255),
    remark VARCHAR(255),
    status VARCHAR(16) NOT NULL DEFAULT 'NORMAL',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (room_id, period_year, period_month),
    CONSTRAINT fk_water_reading_room FOREIGN KEY (room_id) REFERENCES room (id),
    CONSTRAINT fk_water_reading_meter FOREIGN KEY (meter_id) REFERENCES water_meter (id),
    CONSTRAINT fk_water_reading_reader FOREIGN KEY (read_by_admin_id) REFERENCES account (id)
);

CREATE TABLE IF NOT EXISTS bill (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_no VARCHAR(64) NOT NULL,
    room_id BIGINT NOT NULL,
    fee_type VARCHAR(16) NOT NULL,
    period_year SMALLINT NOT NULL,
    period_month TINYINT NOT NULL,
    amount_due DECIMAL(12, 2) NOT NULL,
    discount_amount_total DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    amount_paid DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    due_date DATE NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'ISSUED',
    paid_at TIMESTAMP NULL,
    cancelled_at TIMESTAMP NULL,
    source_type VARCHAR(16) NOT NULL DEFAULT 'GENERATED',
    remark VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (bill_no),
    UNIQUE (room_id, fee_type, period_year, period_month),
    CONSTRAINT fk_bill_room FOREIGN KEY (room_id) REFERENCES room (id)
);

CREATE TABLE IF NOT EXISTS bill_line (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    bill_id BIGINT NOT NULL,
    line_no INT NOT NULL,
    line_type VARCHAR(16) NOT NULL,
    item_name VARCHAR(64) NOT NULL,
    unit_price DECIMAL(10, 4) NOT NULL,
    quantity DECIMAL(12, 3) NOT NULL,
    line_amount DECIMAL(12, 2) NOT NULL,
    ext_json VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (bill_id, line_no),
    CONSTRAINT fk_bill_line_bill FOREIGN KEY (bill_id) REFERENCES bill (id)
);

CREATE TABLE IF NOT EXISTS pay_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pay_order_no VARCHAR(64) NOT NULL,
    bill_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    channel VARCHAR(16) NOT NULL,
    origin_amount DECIMAL(12, 2) NOT NULL,
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    pay_amount DECIMAL(12, 2) NOT NULL,
    coupon_instance_id BIGINT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'CREATED',
    channel_trade_no VARCHAR(128),
    paid_at TIMESTAMP NULL,
    expired_at TIMESTAMP NULL,
    close_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (pay_order_no),
    UNIQUE (idempotency_key),
    CONSTRAINT fk_pay_order_bill FOREIGN KEY (bill_id) REFERENCES bill (id),
    CONSTRAINT fk_pay_order_account FOREIGN KEY (account_id) REFERENCES account (id)
);

CREATE TABLE IF NOT EXISTS pay_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pay_order_no VARCHAR(64) NOT NULL,
    trade_type VARCHAR(32) NOT NULL,
    request_json VARCHAR(4000),
    response_json VARCHAR(4000),
    transaction_status VARCHAR(32) NOT NULL,
    error_code VARCHAR(64),
    error_message VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pay_txn_pay_order FOREIGN KEY (pay_order_no) REFERENCES pay_order (pay_order_no)
);

CREATE INDEX idx_bill_room_status ON bill (room_id, status);
CREATE INDEX idx_bill_period_status ON bill (period_year, period_month, status);
CREATE INDEX idx_fee_rule_lookup ON fee_rule (community_id, fee_type, status, effective_from, effective_to);
CREATE INDEX idx_water_reading_meter_period ON water_meter_reading (meter_id, period_year, period_month);
CREATE INDEX idx_pay_order_bill_status ON pay_order (bill_id, status);
CREATE INDEX idx_pay_order_account_created ON pay_order (account_id, created_at);
CREATE INDEX idx_pay_txn_order_type ON pay_transaction (pay_order_no, trade_type, created_at);
