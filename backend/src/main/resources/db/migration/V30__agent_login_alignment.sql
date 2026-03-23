UPDATE account
SET status = 1,
    updated_at = CURRENT_TIMESTAMP
WHERE account_type = 'AGENT'
  AND status = 0;
