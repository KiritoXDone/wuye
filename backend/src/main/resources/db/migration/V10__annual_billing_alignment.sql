DELETE FROM invoice_application;
DELETE FROM payment_voucher;
DELETE FROM dunning_log;
DELETE FROM dunning_task;
DELETE FROM coupon_redemption WHERE pay_order_no IS NOT NULL;
DELETE FROM pay_order_bill_cover;
DELETE FROM pay_transaction;
DELETE FROM pay_order;
DELETE FROM bill_line;
DELETE FROM bill;

ALTER TABLE bill ADD COLUMN cycle_type VARCHAR(16) NOT NULL DEFAULT 'MONTH';
ALTER TABLE bill ADD COLUMN service_period_start DATE NULL;
ALTER TABLE bill ADD COLUMN service_period_end DATE NULL;

UPDATE fee_rule
SET cycle_type = 'YEAR'
WHERE fee_type = 'PROPERTY';

CREATE INDEX idx_bill_cycle_type_period ON bill (fee_type, cycle_type, period_year, period_month);
