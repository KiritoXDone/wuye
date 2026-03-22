SET @community_status_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'community'
      AND COLUMN_NAME = 'status'
);
SET @room_type_status_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'room_type'
      AND COLUMN_NAME = 'status'
);
SET @room_status_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'room'
      AND COLUMN_NAME = 'status'
);
SET @water_meter_status_exists = (
    SELECT COUNT(1)
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'water_meter'
      AND COLUMN_NAME = 'status'
);

SET @sql = IF(@community_status_exists = 0,
    'ALTER TABLE community ADD COLUMN status INT NOT NULL DEFAULT 1 AFTER name',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(@room_type_status_exists = 0,
    'ALTER TABLE room_type ADD COLUMN status INT NOT NULL DEFAULT 1 AFTER area_m2',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(@room_status_exists = 0,
    'ALTER TABLE room ADD COLUMN status INT NOT NULL DEFAULT 1 AFTER area_m2',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(@water_meter_status_exists = 0,
    'ALTER TABLE water_meter ADD COLUMN status INT NOT NULL DEFAULT 1 AFTER install_at',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE community SET status = 1 WHERE status IS NULL;
UPDATE room_type SET status = 1 WHERE status IS NULL;
UPDATE room SET status = 1 WHERE status IS NULL;
UPDATE water_meter SET status = 1 WHERE status IS NULL;
