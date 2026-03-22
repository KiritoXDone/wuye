CREATE TABLE coupon_exchange_record (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_instance_id BIGINT NOT NULL,
    template_id BIGINT NOT NULL,
    owner_account_id BIGINT NOT NULL,
    goods_name VARCHAR(128) NOT NULL,
    goods_spec VARCHAR(128),
    exchange_status VARCHAR(16) NOT NULL,
    pickup_site VARCHAR(255),
    remark VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (coupon_instance_id),
    CONSTRAINT fk_coupon_exchange_instance FOREIGN KEY (coupon_instance_id) REFERENCES coupon_instance (id),
    CONSTRAINT fk_coupon_exchange_template FOREIGN KEY (template_id) REFERENCES coupon_template (id),
    CONSTRAINT fk_coupon_exchange_owner FOREIGN KEY (owner_account_id) REFERENCES account (id)
);

ALTER TABLE coupon_template
    ADD COLUMN goods_name VARCHAR(128) NULL AFTER name,
    ADD COLUMN goods_spec VARCHAR(128) NULL AFTER goods_name,
    ADD COLUMN fulfillment_type VARCHAR(16) NULL AFTER goods_spec,
    ADD COLUMN redeem_instruction VARCHAR(255) NULL AFTER fulfillment_type;

CREATE INDEX idx_coupon_exchange_owner_status ON coupon_exchange_record (owner_account_id, exchange_status, created_at);
CREATE INDEX idx_coupon_exchange_template_status ON coupon_exchange_record (template_id, exchange_status, created_at);
