CREATE TABLE coupon_seckill_campaign (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    campaign_code VARCHAR(64) NOT NULL,
    template_id BIGINT NOT NULL,
    title VARCHAR(128) NOT NULL,
    total_stock INT NOT NULL,
    available_stock INT NOT NULL,
    per_user_limit INT NOT NULL DEFAULT 1,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    status INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (campaign_code),
    CONSTRAINT fk_coupon_seckill_campaign_template FOREIGN KEY (template_id) REFERENCES coupon_template (id)
);

CREATE TABLE coupon_seckill_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no VARCHAR(64) NOT NULL,
    campaign_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    coupon_instance_id BIGINT NULL,
    status VARCHAR(16) NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    fail_reason VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (order_no),
    UNIQUE (campaign_id, account_id, request_id),
    CONSTRAINT fk_coupon_seckill_order_campaign FOREIGN KEY (campaign_id) REFERENCES coupon_seckill_campaign (id),
    CONSTRAINT fk_coupon_seckill_order_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT fk_coupon_seckill_order_instance FOREIGN KEY (coupon_instance_id) REFERENCES coupon_instance (id)
);

CREATE INDEX idx_coupon_seckill_campaign_status_time ON coupon_seckill_campaign (status, start_at, end_at);
CREATE INDEX idx_coupon_seckill_order_account_status ON coupon_seckill_order (account_id, status, created_at);
