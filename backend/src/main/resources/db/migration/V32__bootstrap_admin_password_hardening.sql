UPDATE account
SET password_hash = '{noop}BOOTSTRAP_REQUIRED',
    status = 0,
    token_invalid_before = COALESCE(token_invalid_before, CURRENT_TIMESTAMP),
    updated_at = CURRENT_TIMESTAMP
WHERE username = 'admin'
  AND account_type = 'ADMIN'
  AND password_hash = '{noop}123456';
