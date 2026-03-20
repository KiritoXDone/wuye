ALTER TABLE bill ADD COLUMN period_month_key TINYINT NOT NULL DEFAULT 0;

UPDATE bill
SET period_month_key = COALESCE(period_month, 0);

ALTER TABLE bill MODIFY COLUMN period_month TINYINT NULL;

UPDATE bill
SET period_month = NULL,
    period_month_key = 0
WHERE cycle_type = 'YEAR'
  AND period_month = 0;

CREATE UNIQUE INDEX uk_bill_room_fee_period_key ON bill (room_id, fee_type, period_year, period_month_key);

ALTER TABLE pay_order_bill_cover MODIFY COLUMN period_month TINYINT NULL;

UPDATE pay_order_bill_cover
SET period_month = NULL
WHERE period_month = 0
  AND bill_id IN (
      SELECT id
      FROM bill
      WHERE cycle_type = 'YEAR'
  );
