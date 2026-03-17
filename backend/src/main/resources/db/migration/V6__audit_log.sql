CREATE TABLE audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    biz_type VARCHAR(32) NOT NULL,
    biz_id VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,
    operator_id BIGINT NULL,
    detail_json VARCHAR(4000) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_log_operator FOREIGN KEY (operator_id) REFERENCES account (id)
);

CREATE INDEX idx_audit_log_biz ON audit_log (biz_type, biz_id, created_at);
CREATE INDEX idx_audit_log_operator_created ON audit_log (operator_id, created_at);
