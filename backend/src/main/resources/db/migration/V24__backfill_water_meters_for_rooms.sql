INSERT INTO water_meter (room_id, meter_no, install_at, status)
SELECT r.id,
       CONCAT('WM-ROOM-', r.id),
       NULL,
       1
FROM room r
LEFT JOIN water_meter wm ON wm.room_id = r.id
WHERE wm.id IS NULL;
