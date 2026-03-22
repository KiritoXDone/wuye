DROP INDEX idx_room_admin_type_filter ON room;
DROP INDEX idx_room_type_community_status ON room_type;

ALTER TABLE community DROP COLUMN status;
ALTER TABLE room_type DROP COLUMN status;
ALTER TABLE room DROP COLUMN status;
ALTER TABLE water_meter DROP COLUMN status;

CREATE INDEX idx_room_admin_type_filter ON room (community_id, room_type_id);
CREATE INDEX idx_room_type_community ON room_type (community_id);
