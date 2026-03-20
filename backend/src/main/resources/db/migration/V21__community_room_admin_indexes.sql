CREATE INDEX idx_community_status_id ON community (status, id);
CREATE INDEX idx_room_admin_filter ON room (community_id, status, building_no, unit_no, room_no);
CREATE INDEX idx_room_admin_type_filter ON room (community_id, room_type_id, status);
