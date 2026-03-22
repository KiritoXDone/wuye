SET @coupon_issue_rule_trigger_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'coupon_issue_rule'
      AND COLUMN_NAME = 'trigger_type'
);

SET @sql = IF(@coupon_issue_rule_trigger_exists = 0,
    'ALTER TABLE coupon_issue_rule ADD COLUMN trigger_type VARCHAR(32) NULL AFTER rule_name',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE coupon_issue_rule
SET trigger_type = CASE
    WHEN fee_type = 'PROPERTY' THEN 'PROPERTY_PAYMENT'
    WHEN fee_type = 'WATER' THEN 'WATER_PAYMENT'
    ELSE trigger_type
END
WHERE trigger_type IS NULL;

SET @sql = IF(@coupon_issue_rule_trigger_exists = 0,
    'ALTER TABLE coupon_issue_rule MODIFY COLUMN trigger_type VARCHAR(32) NOT NULL',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE INDEX idx_coupon_issue_rule_trigger_status ON coupon_issue_rule (trigger_type, status, template_id);
