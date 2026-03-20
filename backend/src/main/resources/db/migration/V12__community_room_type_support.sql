CREATE TABLE IF NOT EXISTS room_type (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    community_id BIGINT NOT NULL,
    type_code VARCHAR(64) NOT NULL,
    type_name VARCHAR(128) NOT NULL,
    area_m2 DECIMAL(10, 2) NOT NULL,
    status INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (community_id, type_code),
    UNIQUE (community_id, type_name),
    CONSTRAINT fk_room_type_community FOREIGN KEY (community_id) REFERENCES community (id)
);

ALTER TABLE room
    ADD COLUMN room_type_id BIGINT NULL AFTER room_no;

ALTER TABLE room
    ADD CONSTRAINT fk_room_room_type FOREIGN KEY (room_type_id) REFERENCES room_type (id);

CREATE INDEX idx_room_type_community_status ON room_type (community_id, status);
CREATE INDEX idx_room_room_type ON room (room_type_id);
