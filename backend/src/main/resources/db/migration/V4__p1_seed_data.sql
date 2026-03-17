INSERT INTO account (id, account_no, account_type, username, password_hash, nickname, mobile, real_name, avatar_url, status)
VALUES (30001, 'ACC-AGT-001', 'AGENT', NULL, NULL, '经办人A', '13900000002', '经办人A', NULL, 1);

INSERT INTO account_identity (id, account_id, platform, open_id, union_id, platform_user_id, status)
VALUES (30003, 30001, 'WECHAT', 'agent-a', NULL, 'agent-a', 1);

INSERT INTO user_group (id, group_code, name, scope_type, community_id, status)
VALUES (5001, 'G-COMM001-1-2', '一栋二单元', 'UNIT', 100, 1);

INSERT INTO group_room (id, group_id, room_id)
VALUES (60001, 5001, 1001),
       (60002, 5001, 1002);

INSERT INTO agent_profile (id, account_id, agent_code, org_name, status)
VALUES (70001, 30001, 'AGENT-A', '阳光花园片区', 1);

INSERT INTO agent_group (id, agent_id, group_id, permission, status)
VALUES (80001, 70001, 5001, 'VIEW', 1);

INSERT INTO coupon_template (id, template_code, type, fee_type, name, discount_mode, value_amount, threshold_amount, valid_from, valid_to, stackable, status)
VALUES (90001, 'PAY-OFF-10', 'PAYMENT', 'PROPERTY', '满100减10物业券', 'FIXED', 10.00, 100.00, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 0, 1),
       (90002, 'VCH-PARK-1H', 'VOUCHER', 'PROPERTY', '停车兑换券', 'FIXED', 0.00, 0.00, '2026-01-01 00:00:00', '2026-12-31 23:59:59', 0, 1);

INSERT INTO coupon_issue_rule (id, rule_name, fee_type, template_id, min_pay_amount, reward_count, status)
VALUES (91001, '物业费支付成功送停车券', 'PROPERTY', 90002, 100.00, 1, 1);

INSERT INTO coupon_instance (id, template_id, owner_account_id, owner_group_id, source_type, source_ref_no, status, issued_at, expires_at)
VALUES (92001, 90001, 10001, NULL, 'MANUAL', NULL, 'NEW', '2026-03-01 09:00:00', '2026-12-31 23:59:59');
