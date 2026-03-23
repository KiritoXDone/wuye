ALTER TABLE account
    ADD COLUMN token_invalid_before DATETIME NULL AFTER last_login_at;

CREATE UNIQUE INDEX uk_coupon_instance_reward_source
    ON coupon_instance (source_type, source_ref_no, owner_account_id, template_id);
