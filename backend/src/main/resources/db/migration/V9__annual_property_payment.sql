ALTER TABLE pay_order ADD COLUMN is_annual_payment TINYINT NOT NULL DEFAULT 0;
ALTER TABLE pay_order ADD COLUMN covered_bill_count INT NOT NULL DEFAULT 1;

CREATE TABLE pay_order_bill_cover (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    pay_order_no VARCHAR(64) NOT NULL,
    bill_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    period_year SMALLINT NOT NULL,
    period_month TINYINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (pay_order_no, bill_id),
    CONSTRAINT fk_pay_order_bill_cover_pay_order FOREIGN KEY (pay_order_no) REFERENCES pay_order (pay_order_no),
    CONSTRAINT fk_pay_order_bill_cover_bill FOREIGN KEY (bill_id) REFERENCES bill (id),
    CONSTRAINT fk_pay_order_bill_cover_room FOREIGN KEY (room_id) REFERENCES room (id)
);

CREATE INDEX idx_pay_order_bill_cover_order ON pay_order_bill_cover (pay_order_no, period_year, period_month);
CREATE INDEX idx_pay_order_bill_cover_bill ON pay_order_bill_cover (bill_id);
