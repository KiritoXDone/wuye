INSERT INTO community (id, community_code, name, status)
VALUES (100, 'COMM-001', '阳光花园', 1);

INSERT INTO account (id, account_no, account_type, username, password_hash, nickname, mobile, real_name, avatar_url, status)
VALUES (10001, 'ACC-RES-001', 'RESIDENT', NULL, NULL, '张三', '13800000001', '张三', 'https://example.com/avatar/zhangsan.png', 1),
       (10002, 'ACC-RES-002', 'RESIDENT', NULL, NULL, '李四', '13800000002', '李四', 'https://example.com/avatar/lisi.png', 1),
       (20001, 'ACC-ADM-001', 'ADMIN', 'admin', '{noop}123456', '管理员', '13900000001', '系统管理员', NULL, 1);

INSERT INTO account_identity (id, account_id, platform, open_id, union_id, platform_user_id, status)
VALUES (30001, 10001, 'WECHAT', 'resident-zhangsan', NULL, 'resident-zhangsan', 1),
       (30002, 10002, 'WECHAT', 'resident-lisi', NULL, 'resident-lisi', 1);

INSERT INTO room (id, community_id, building_no, unit_no, room_no, area_m2, status)
VALUES (1001, 100, '1', '2', '301', 98.50, 1),
       (1002, 100, '1', '2', '302', 88.00, 1);

INSERT INTO account_room (id, account_id, room_id, relation_type, status, bind_source, confirmed_at, remark)
VALUES (40001, 10001, 1001, 'OWNER', 'ACTIVE', 'IMPORT', CURRENT_TIMESTAMP, '初始化绑定'),
       (40002, 10002, 1001, 'FAMILY', 'ACTIVE', 'IMPORT', CURRENT_TIMESTAMP, '初始化共享房间');
