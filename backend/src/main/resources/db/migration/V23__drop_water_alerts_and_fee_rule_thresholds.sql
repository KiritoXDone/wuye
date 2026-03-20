ALTER TABLE fee_rule
    DROP COLUMN abnormal_abs_threshold,
    DROP COLUMN abnormal_multiplier_threshold;

DROP TABLE IF EXISTS water_usage_alert;
