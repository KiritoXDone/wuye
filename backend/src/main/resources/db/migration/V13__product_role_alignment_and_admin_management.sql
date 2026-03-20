UPDATE account
SET account_type = 'ADMIN', updated_at = CURRENT_TIMESTAMP
WHERE account_type = 'FINANCE';

UPDATE account
SET status = 0, updated_at = CURRENT_TIMESTAMP
WHERE account_type = 'AGENT';

CREATE INDEX idx_account_type_status ON account (account_type, status);
