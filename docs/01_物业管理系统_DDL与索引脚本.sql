-- 物业管理系统 DDL 与索引脚本
-- 适用：MySQL 8.x / InnoDB / utf8mb4
-- 说明：
-- 1) 账单主体统一为 room（房间）
-- 2) 物业费按面积计费，水费按抄表计费
-- 3) 支持 Web 管理端 + 微信小程序住户端 + 微信小程序管理轻端
-- 4) 本脚本可直接初始化空库；生产环境建议通过 Flyway / Liquibase 管理版本

CREATE DATABASE IF NOT EXISTS `wuye_system`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;
USE `wuye_system`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE IF NOT EXISTS `community` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '小区ID',
  `community_code` VARCHAR(64) NOT NULL COMMENT '小区编码',
  `name` VARCHAR(128) NOT NULL COMMENT '小区名称',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_community_code` (`community_code`),
  KEY `idx_community_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='小区表';

CREATE TABLE IF NOT EXISTS `account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '账号ID',
  `account_no` VARCHAR(64) NOT NULL COMMENT '账号编号',
  `account_type` VARCHAR(16) NOT NULL COMMENT 'RESIDENT/AGENT/ADMIN/FINANCE',
  `username` VARCHAR(64) NULL COMMENT 'Web管理端用户名',
  `password_hash` VARCHAR(255) NULL COMMENT 'Web管理端密码Hash',
  `nickname` VARCHAR(64) NULL COMMENT '昵称',
  `mobile` VARCHAR(20) NULL COMMENT '手机号',
  `real_name` VARCHAR(64) NULL COMMENT '真实姓名',
  `avatar_url` VARCHAR(255) NULL COMMENT '头像',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `last_login_at` DATETIME NULL COMMENT '最近登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_no` (`account_no`),
  UNIQUE KEY `uk_account_username` (`username`),
  UNIQUE KEY `uk_account_mobile` (`mobile`),
  KEY `idx_account_type_status` (`account_type`, `status`),
  CONSTRAINT `chk_account_type` CHECK (`account_type` IN ('RESIDENT', 'AGENT', 'ADMIN', 'FINANCE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一账号表';

CREATE TABLE IF NOT EXISTS `account_identity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `account_id` BIGINT NOT NULL COMMENT '账号ID',
  `platform` VARCHAR(16) NOT NULL COMMENT 'WECHAT/ALIPAY',
  `open_id` VARCHAR(128) NULL COMMENT '微信open_id',
  `union_id` VARCHAR(128) NULL COMMENT '微信union_id',
  `platform_user_id` VARCHAR(128) NULL COMMENT '平台user_id等',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_identity_platform_openid` (`platform`, `open_id`),
  UNIQUE KEY `uk_identity_platform_userid` (`platform`, `platform_user_id`),
  KEY `idx_identity_account` (`account_id`, `status`),
  CONSTRAINT `fk_identity_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账号三方身份表';

CREATE TABLE IF NOT EXISTS `user_group` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户组ID',
  `group_code` VARCHAR(64) NOT NULL COMMENT '用户组编码',
  `name` VARCHAR(128) NOT NULL COMMENT '用户组名称',
  `scope_type` VARCHAR(16) NOT NULL COMMENT 'BUILDING/UNIT/REGION/CUSTOM',
  `community_id` BIGINT NOT NULL COMMENT '小区ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_group_code` (`group_code`),
  KEY `idx_user_group_community_status` (`community_id`, `status`),
  CONSTRAINT `fk_user_group_community` FOREIGN KEY (`community_id`) REFERENCES `community` (`id`),
  CONSTRAINT `chk_user_group_scope_type` CHECK (`scope_type` IN ('BUILDING', 'UNIT', 'REGION', 'CUSTOM'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户组表';

CREATE TABLE IF NOT EXISTS `house_type` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '户型ID',
  `community_id` BIGINT NOT NULL COMMENT '小区ID',
  `type_code` VARCHAR(64) NOT NULL COMMENT '户型编码',
  `name` VARCHAR(128) NOT NULL COMMENT '户型名称',
  `area_m2_default` DECIMAL(10,2) NOT NULL COMMENT '默认面积(㎡)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_house_type_code` (`community_id`, `type_code`),
  KEY `idx_house_type_status` (`community_id`, `status`),
  CONSTRAINT `fk_house_type_community` FOREIGN KEY (`community_id`) REFERENCES `community` (`id`),
  CONSTRAINT `chk_house_type_area` CHECK (`area_m2_default` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='户型表';

CREATE TABLE IF NOT EXISTS `room` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '房间ID',
  `community_id` BIGINT NOT NULL COMMENT '小区ID',
  `building_no` VARCHAR(32) NOT NULL COMMENT '楼栋',
  `unit_no` VARCHAR(32) NOT NULL COMMENT '单元',
  `room_no` VARCHAR(32) NOT NULL COMMENT '房号',
  `house_type_id` BIGINT NULL COMMENT '户型ID，可空',
  `area_m2` DECIMAL(10,2) NOT NULL COMMENT '面积(㎡)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_location` (`community_id`, `building_no`, `unit_no`, `room_no`),
  KEY `idx_room_community_status` (`community_id`, `status`),
  KEY `idx_room_house_type` (`house_type_id`),
  CONSTRAINT `fk_room_community` FOREIGN KEY (`community_id`) REFERENCES `community` (`id`),
  CONSTRAINT `fk_room_house_type` FOREIGN KEY (`house_type_id`) REFERENCES `house_type` (`id`),
  CONSTRAINT `chk_room_area` CHECK (`area_m2` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='房间表，账单主体';

CREATE TABLE IF NOT EXISTS `account_room` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `account_id` BIGINT NOT NULL COMMENT '账号ID',
  `room_id` BIGINT NOT NULL COMMENT '房间ID',
  `relation_type` VARCHAR(16) NULL COMMENT 'OWNER/FAMILY/TENANT/OTHER',
  `status` VARCHAR(16) NOT NULL COMMENT 'PENDING/ACTIVE/INACTIVE',
  `bind_source` VARCHAR(16) NOT NULL COMMENT 'SELF/ADMIN/IMPORT',
  `confirmed_at` DATETIME NULL COMMENT '确认时间',
  `remark` VARCHAR(255) NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_room` (`account_id`, `room_id`),
  KEY `idx_account_room_account_status` (`account_id`, `status`),
  KEY `idx_account_room_room_status` (`room_id`, `status`),
  CONSTRAINT `fk_account_room_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`),
  CONSTRAINT `fk_account_room_room` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`),
  CONSTRAINT `chk_account_room_status` CHECK (`status` IN ('PENDING', 'ACTIVE', 'INACTIVE')),
  CONSTRAINT `chk_account_room_bind_source` CHECK (`bind_source` IN ('SELF', 'ADMIN', 'IMPORT'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账号与房间绑定关系';

CREATE TABLE IF NOT EXISTS `group_room` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `group_id` BIGINT NOT NULL COMMENT '用户组ID',
  `room_id` BIGINT NOT NULL COMMENT '房间ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_group_room` (`group_id`, `room_id`),
  KEY `idx_group_room_room` (`room_id`),
  CONSTRAINT `fk_group_room_group` FOREIGN KEY (`group_id`) REFERENCES `user_group` (`id`),
  CONSTRAINT `fk_group_room_room` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户组与房间关系';

CREATE TABLE IF NOT EXISTS `agent_profile` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `account_id` BIGINT NOT NULL COMMENT '账号ID',
  `agent_code` VARCHAR(64) NOT NULL COMMENT 'Agent编码',
  `org_name` VARCHAR(128) NULL COMMENT '所属组织',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_profile_account` (`account_id`),
  UNIQUE KEY `uk_agent_profile_code` (`agent_code`),
  KEY `idx_agent_profile_status` (`status`),
  CONSTRAINT `fk_agent_profile_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent档案';

CREATE TABLE IF NOT EXISTS `agent_group` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `agent_id` BIGINT NOT NULL COMMENT 'AgentID',
  `group_id` BIGINT NOT NULL COMMENT '用户组ID',
  `permission` VARCHAR(16) NOT NULL COMMENT 'VIEW/MANAGE',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_agent_group` (`agent_id`, `group_id`),
  KEY `idx_agent_group_group_status` (`group_id`, `status`),
  CONSTRAINT `fk_agent_group_agent` FOREIGN KEY (`agent_id`) REFERENCES `agent_profile` (`id`),
  CONSTRAINT `fk_agent_group_group` FOREIGN KEY (`group_id`) REFERENCES `user_group` (`id`),
  CONSTRAINT `chk_agent_group_permission` CHECK (`permission` IN ('VIEW', 'MANAGE'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Agent授权用户组';

CREATE TABLE IF NOT EXISTS `org_unit` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '组织ID',
  `tenant_code` VARCHAR(64) NOT NULL COMMENT '租户编码',
  `org_code` VARCHAR(64) NOT NULL COMMENT '组织编码',
  `name` VARCHAR(128) NOT NULL COMMENT '组织名称',
  `parent_id` BIGINT NULL COMMENT '上级组织ID',
  `community_id` BIGINT NULL COMMENT '关联小区ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_org_unit_tenant_code` (`tenant_code`, `org_code`),
  KEY `idx_org_unit_tenant_parent` (`tenant_code`, `parent_id`, `status`),
  CONSTRAINT `fk_org_unit_parent` FOREIGN KEY (`parent_id`) REFERENCES `org_unit` (`id`),
  CONSTRAINT `fk_org_unit_community` FOREIGN KEY (`community_id`) REFERENCES `community` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='轻量组织架构与租户节点';

ALTER TABLE `user_group`
  ADD COLUMN `org_unit_id` BIGINT NULL COMMENT '组织单元ID',
  ADD CONSTRAINT `fk_user_group_org_unit` FOREIGN KEY (`org_unit_id`) REFERENCES `org_unit` (`id`);

ALTER TABLE `agent_profile`
  ADD COLUMN `org_unit_id` BIGINT NULL COMMENT '组织单元ID',
  ADD CONSTRAINT `fk_agent_profile_org_unit` FOREIGN KEY (`org_unit_id`) REFERENCES `org_unit` (`id`);

CREATE TABLE IF NOT EXISTS `fee_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '规则ID',
  `community_id` BIGINT NOT NULL COMMENT '小区ID',
  `fee_type` VARCHAR(16) NOT NULL COMMENT 'PROPERTY/WATER',
  `rule_name` VARCHAR(128) NOT NULL COMMENT '规则名称',
  `unit_price` DECIMAL(10,4) NOT NULL COMMENT '单价',
  `cycle_type` VARCHAR(16) NOT NULL DEFAULT 'MONTH' COMMENT 'MONTH/QUARTER/YEAR',
  `pricing_mode` VARCHAR(16) NOT NULL DEFAULT 'FLAT' COMMENT 'FLAT/TIERED',
  `effective_from` DATE NOT NULL COMMENT '生效开始',
  `effective_to` DATE NULL COMMENT '生效结束',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `remark` VARCHAR(255) NULL COMMENT '备注',
  `abnormal_abs_threshold` DECIMAL(12,3) NULL COMMENT '异常绝对阈值',
  `abnormal_multiplier_threshold` DECIMAL(10,2) NULL COMMENT '异常倍数阈值',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_fee_rule_lookup` (`community_id`, `fee_type`, `status`, `effective_from`, `effective_to`),
  KEY `idx_fee_rule_pricing_mode` (`community_id`, `fee_type`, `pricing_mode`, `status`),
  CONSTRAINT `fk_fee_rule_community` FOREIGN KEY (`community_id`) REFERENCES `community` (`id`),
  CONSTRAINT `chk_fee_rule_type` CHECK (`fee_type` IN ('PROPERTY', 'WATER')),
  CONSTRAINT `chk_fee_rule_pricing_mode` CHECK (`pricing_mode` IN ('FLAT', 'TIERED')),
  CONSTRAINT `chk_fee_rule_cycle_type` CHECK (`cycle_type` IN ('MONTH', 'QUARTER', 'YEAR')),
  CONSTRAINT `chk_fee_rule_price` CHECK (`unit_price` >= 0),
  CONSTRAINT `chk_fee_rule_effective_range` CHECK (`effective_to` IS NULL OR `effective_to` >= `effective_from`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='费用规则表';

CREATE TABLE IF NOT EXISTS `fee_rule_water_tier` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '阶梯ID',
  `fee_rule_id` BIGINT NOT NULL COMMENT '费用规则ID',
  `tier_order` INT NOT NULL COMMENT '阶梯顺序',
  `start_usage` DECIMAL(12,3) NOT NULL COMMENT '起始用量',
  `end_usage` DECIMAL(12,3) NULL COMMENT '结束用量，可空表示以上',
  `unit_price` DECIMAL(10,4) NOT NULL COMMENT '本阶梯单价',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_fee_rule_water_tier_order` (`fee_rule_id`, `tier_order`),
  KEY `idx_fee_rule_water_tier_rule` (`fee_rule_id`, `tier_order`),
  CONSTRAINT `fk_fee_rule_water_tier_rule` FOREIGN KEY (`fee_rule_id`) REFERENCES `fee_rule` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='水费阶梯价明细';

CREATE TABLE IF NOT EXISTS `water_meter` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '水表ID',
  `room_id` BIGINT NOT NULL COMMENT '房间ID',
  `meter_no` VARCHAR(64) NULL COMMENT '水表编号',
  `install_at` DATE NULL COMMENT '安装日期',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_water_meter_room` (`room_id`),
  KEY `idx_water_meter_status` (`status`),
  CONSTRAINT `fk_water_meter_room` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='水表档案';

CREATE TABLE IF NOT EXISTS `water_meter_reading` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '抄表记录ID',
  `room_id` BIGINT NOT NULL COMMENT '房间ID',
  `meter_id` BIGINT NOT NULL COMMENT '水表ID',
  `period_year` SMALLINT NOT NULL COMMENT '账期年',
  `period_month` TINYINT NOT NULL COMMENT '账期月',
  `prev_reading` DECIMAL(12,3) NOT NULL COMMENT '期初读数',
  `curr_reading` DECIMAL(12,3) NOT NULL COMMENT '期末读数',
  `usage_amount` DECIMAL(12,3) NOT NULL COMMENT '用量=期末-期初',
  `read_by_admin_id` BIGINT NOT NULL COMMENT '抄表人账号ID',
  `read_at` DATETIME NOT NULL COMMENT '抄表时间',
  `photo_url` VARCHAR(255) NULL COMMENT '抄表照片',
  `remark` VARCHAR(255) NULL COMMENT '备注',
  `status` VARCHAR(16) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL/ABNORMAL/VOID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_water_reading_room_period` (`room_id`, `period_year`, `period_month`),
  KEY `idx_water_reading_meter_period` (`meter_id`, `period_year`, `period_month`),
  KEY `idx_water_reading_reader` (`read_by_admin_id`, `read_at`),
  CONSTRAINT `fk_water_reading_room` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`),
  CONSTRAINT `fk_water_reading_meter` FOREIGN KEY (`meter_id`) REFERENCES `water_meter` (`id`),
  CONSTRAINT `fk_water_reading_reader` FOREIGN KEY (`read_by_admin_id`) REFERENCES `account` (`id`),
  CONSTRAINT `chk_water_reading_month` CHECK (`period_month` BETWEEN 1 AND 12),
  CONSTRAINT `chk_water_reading_curr_prev` CHECK (`curr_reading` >= `prev_reading`),
  CONSTRAINT `chk_water_reading_usage` CHECK (`usage_amount` >= 0),
  CONSTRAINT `chk_water_reading_status` CHECK (`status` IN ('NORMAL', 'ABNORMAL', 'VOID'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='水表抄表记录';

CREATE TABLE IF NOT EXISTS `water_usage_alert` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '预警ID',
  `reading_id` BIGINT NOT NULL COMMENT '抄表记录ID',
  `room_id` BIGINT NOT NULL COMMENT '房间ID',
  `alert_code` VARCHAR(32) NOT NULL COMMENT 'ABS_THRESHOLD/MULTIPLIER_THRESHOLD',
  `alert_message` VARCHAR(255) NOT NULL COMMENT '预警描述',
  `threshold_value` DECIMAL(12,3) NULL COMMENT '阈值',
  `actual_value` DECIMAL(12,3) NOT NULL COMMENT '实际值',
  `status` VARCHAR(16) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN/CLOSED',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_water_usage_alert_reading_code` (`reading_id`, `alert_code`),
  KEY `idx_water_usage_alert_room_status` (`room_id`, `status`, `created_at`),
  CONSTRAINT `fk_water_usage_alert_reading` FOREIGN KEY (`reading_id`) REFERENCES `water_meter_reading` (`id`),
  CONSTRAINT `fk_water_usage_alert_room` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='异常用量预警';

CREATE TABLE IF NOT EXISTS `bill` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '账单ID',
  `bill_no` VARCHAR(64) NOT NULL COMMENT '账单编号',
  `room_id` BIGINT NOT NULL COMMENT '房间ID',
  `group_id` BIGINT NULL COMMENT '所属用户组',
  `fee_type` VARCHAR(16) NOT NULL COMMENT 'PROPERTY/WATER',
  `period_year` SMALLINT NOT NULL COMMENT '账期年',
  `period_month` TINYINT NOT NULL COMMENT '账期月',
  `amount_due` DECIMAL(12,2) NOT NULL COMMENT '应收金额',
  `discount_amount_total` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '累计抵扣金额',
  `amount_paid` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '实收金额',
  `due_date` DATE NOT NULL COMMENT '到期日',
  `status` VARCHAR(16) NOT NULL DEFAULT 'ISSUED' COMMENT 'ISSUED/PAID/CANCELLED',
  `paid_at` DATETIME NULL COMMENT '支付完成时间',
  `cancelled_at` DATETIME NULL COMMENT '作废时间',
  `source_type` VARCHAR(16) NOT NULL DEFAULT 'GENERATED' COMMENT 'GENERATED/IMPORTED',
  `remark` VARCHAR(255) NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bill_no` (`bill_no`),
  UNIQUE KEY `uk_bill_room_fee_period` (`room_id`, `fee_type`, `period_year`, `period_month`),
  KEY `idx_bill_room_status` (`room_id`, `status`),
  KEY `idx_bill_group_period` (`group_id`, `period_year`, `period_month`),
  KEY `idx_bill_period_status` (`period_year`, `period_month`, `status`),
  CONSTRAINT `fk_bill_room` FOREIGN KEY (`room_id`) REFERENCES `room` (`id`),
  CONSTRAINT `fk_bill_group` FOREIGN KEY (`group_id`) REFERENCES `user_group` (`id`),
  CONSTRAINT `chk_bill_fee_type` CHECK (`fee_type` IN ('PROPERTY', 'WATER')),
  CONSTRAINT `chk_bill_month` CHECK (`period_month` BETWEEN 1 AND 12),
  CONSTRAINT `chk_bill_status` CHECK (`status` IN ('ISSUED', 'PAID', 'CANCELLED')),
  CONSTRAINT `chk_bill_source_type` CHECK (`source_type` IN ('GENERATED', 'IMPORTED')),
  CONSTRAINT `chk_bill_amount_due` CHECK (`amount_due` >= 0),
  CONSTRAINT `chk_bill_discount_amount_total` CHECK (`discount_amount_total` >= 0),
  CONSTRAINT `chk_bill_amount_paid` CHECK (`amount_paid` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单主表';

CREATE TABLE IF NOT EXISTS `bill_line` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `bill_id` BIGINT NOT NULL COMMENT '账单ID',
  `line_no` INT NOT NULL COMMENT '行号，从1开始',
  `line_type` VARCHAR(16) NOT NULL COMMENT 'PROPERTY/WATER',
  `item_name` VARCHAR(64) NOT NULL COMMENT '明细项名称',
  `unit_price` DECIMAL(10,4) NOT NULL COMMENT '单价',
  `quantity` DECIMAL(12,3) NOT NULL COMMENT '数量',
  `line_amount` DECIMAL(12,2) NOT NULL COMMENT '行金额',
  `ext_json` JSON NULL COMMENT '扩展字段：面积/期初期末/用量等',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bill_line_no` (`bill_id`, `line_no`),
  KEY `idx_bill_line_type` (`bill_id`, `line_type`),
  CONSTRAINT `fk_bill_line_bill` FOREIGN KEY (`bill_id`) REFERENCES `bill` (`id`),
  CONSTRAINT `chk_bill_line_type` CHECK (`line_type` IN ('PROPERTY', 'WATER')),
  CONSTRAINT `chk_bill_line_unit_price` CHECK (`unit_price` >= 0),
  CONSTRAINT `chk_bill_line_quantity` CHECK (`quantity` >= 0),
  CONSTRAINT `chk_bill_line_amount` CHECK (`line_amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='账单明细';

CREATE TABLE IF NOT EXISTS `coupon_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '模板ID',
  `template_code` VARCHAR(64) NOT NULL COMMENT '模板编码',
  `type` VARCHAR(16) NOT NULL COMMENT 'PAYMENT/VOUCHER',
  `name` VARCHAR(128) NOT NULL COMMENT '模板名称',
  `description` VARCHAR(255) NULL COMMENT '描述',
  `discount_mode` VARCHAR(16) NOT NULL COMMENT 'FIXED/PERCENT',
  `discount_value` DECIMAL(12,2) NOT NULL COMMENT '面额或折扣值',
  `threshold_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '最低门槛金额',
  `scope_type` VARCHAR(16) NOT NULL DEFAULT 'ALL' COMMENT 'ALL/FEE_TYPE/GROUP',
  `scope_value` VARCHAR(255) NULL COMMENT '适用范围值',
  `valid_days` INT NULL COMMENT '发放后有效天数',
  `valid_from` DATETIME NULL COMMENT '固定生效时间',
  `valid_to` DATETIME NULL COMMENT '固定截止时间',
  `stackable` TINYINT NOT NULL DEFAULT 0 COMMENT '是否可叠加',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_template_code` (`template_code`),
  KEY `idx_coupon_template_type_status` (`type`, `status`),
  CONSTRAINT `chk_coupon_template_type` CHECK (`type` IN ('PAYMENT', 'VOUCHER')),
  CONSTRAINT `chk_coupon_template_discount_mode` CHECK (`discount_mode` IN ('FIXED', 'PERCENT')),
  CONSTRAINT `chk_coupon_template_scope_type` CHECK (`scope_type` IN ('ALL', 'FEE_TYPE', 'GROUP')),
  CONSTRAINT `chk_coupon_template_discount_value` CHECK (`discount_value` >= 0),
  CONSTRAINT `chk_coupon_template_threshold` CHECK (`threshold_amount` >= 0),
  CONSTRAINT `chk_coupon_template_stackable` CHECK (`stackable` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='券模板';

CREATE TABLE IF NOT EXISTS `coupon_issue_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '发券规则ID',
  `rule_code` VARCHAR(64) NOT NULL COMMENT '规则编码',
  `name` VARCHAR(128) NOT NULL COMMENT '规则名称',
  `trigger_type` VARCHAR(32) NOT NULL COMMENT 'PAYMENT_SUCCESS',
  `fee_type` VARCHAR(16) NULL COMMENT 'PROPERTY/WATER/ALL',
  `min_pay_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '最低支付金额门槛',
  `reward_template_id` BIGINT NOT NULL COMMENT '奖励模板ID',
  `reward_count` INT NOT NULL DEFAULT 1 COMMENT '发放数量',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '1启用 0停用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_issue_rule_code` (`rule_code`),
  KEY `idx_coupon_issue_rule_status` (`status`, `trigger_type`),
  CONSTRAINT `fk_coupon_issue_rule_template` FOREIGN KEY (`reward_template_id`) REFERENCES `coupon_template` (`id`),
  CONSTRAINT `chk_coupon_issue_rule_trigger` CHECK (`trigger_type` IN ('PAYMENT_SUCCESS')),
  CONSTRAINT `chk_coupon_issue_rule_fee_type` CHECK (`fee_type` IS NULL OR `fee_type` IN ('PROPERTY', 'WATER', 'ALL')),
  CONSTRAINT `chk_coupon_issue_rule_min_pay_amount` CHECK (`min_pay_amount` >= 0),
  CONSTRAINT `chk_coupon_issue_rule_reward_count` CHECK (`reward_count` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付后发券规则';

CREATE TABLE IF NOT EXISTS `coupon_instance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '券实例ID',
  `coupon_no` VARCHAR(64) NOT NULL COMMENT '券编号',
  `template_id` BIGINT NOT NULL COMMENT '模板ID',
  `owner_account_id` BIGINT NULL COMMENT '归属账号ID',
  `owner_group_id` BIGINT NULL COMMENT '归属用户组ID',
  `source_type` VARCHAR(16) NOT NULL COMMENT 'MANUAL/PAYMENT_REWARD/IMPORT',
  `source_ref` VARCHAR(64) NULL COMMENT '来源引用',
  `status` VARCHAR(16) NOT NULL DEFAULT 'NEW' COMMENT 'NEW/LOCKED/USED/EXPIRED/CANCELLED',
  `lock_token` VARCHAR(64) NULL COMMENT '锁令牌',
  `locked_at` DATETIME NULL COMMENT '加锁时间',
  `issued_at` DATETIME NOT NULL COMMENT '发放时间',
  `expires_at` DATETIME NULL COMMENT '过期时间',
  `used_at` DATETIME NULL COMMENT '使用时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_instance_no` (`coupon_no`),
  KEY `idx_coupon_instance_account_status` (`owner_account_id`, `status`, `expires_at`),
  KEY `idx_coupon_instance_group_status` (`owner_group_id`, `status`, `expires_at`),
  KEY `idx_coupon_instance_template_status` (`template_id`, `status`),
  CONSTRAINT `fk_coupon_instance_template` FOREIGN KEY (`template_id`) REFERENCES `coupon_template` (`id`),
  CONSTRAINT `fk_coupon_instance_owner_account` FOREIGN KEY (`owner_account_id`) REFERENCES `account` (`id`),
  CONSTRAINT `fk_coupon_instance_owner_group` FOREIGN KEY (`owner_group_id`) REFERENCES `user_group` (`id`),
  CONSTRAINT `chk_coupon_instance_source_type` CHECK (`source_type` IN ('MANUAL', 'PAYMENT_REWARD', 'IMPORT')),
  CONSTRAINT `chk_coupon_instance_status` CHECK (`status` IN ('NEW', 'LOCKED', 'USED', 'EXPIRED', 'CANCELLED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='券实例';

CREATE TABLE IF NOT EXISTS `pay_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pay_order_no` VARCHAR(64) NOT NULL COMMENT '支付单号',
  `bill_id` BIGINT NOT NULL COMMENT '账单ID',
  `account_id` BIGINT NOT NULL COMMENT '发起支付账号ID',
  `channel` VARCHAR(16) NOT NULL COMMENT 'WECHAT/ALIPAY',
  `origin_amount` DECIMAL(12,2) NOT NULL COMMENT '原始金额',
  `discount_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00 COMMENT '抵扣金额',
  `pay_amount` DECIMAL(12,2) NOT NULL COMMENT '实付金额',
  `coupon_instance_id` BIGINT NULL COMMENT '使用券实例ID',
  `idempotency_key` VARCHAR(128) NOT NULL COMMENT '幂等键',
  `status` VARCHAR(16) NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/PAYING/SUCCESS/FAILED/CLOSED',
  `channel_trade_no` VARCHAR(128) NULL COMMENT '渠道交易号',
  `paid_at` DATETIME NULL COMMENT '支付完成时间',
  `expired_at` DATETIME NULL COMMENT '支付过期时间',
  `close_reason` VARCHAR(255) NULL COMMENT '关闭原因',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_pay_order_no` (`pay_order_no`),
  UNIQUE KEY `uk_pay_order_idempotency_key` (`idempotency_key`),
  KEY `idx_pay_order_bill_status` (`bill_id`, `status`),
  KEY `idx_pay_order_account_created` (`account_id`, `created_at`),
  KEY `idx_pay_order_channel_trade` (`channel`, `channel_trade_no`),
  CONSTRAINT `fk_pay_order_bill` FOREIGN KEY (`bill_id`) REFERENCES `bill` (`id`),
  CONSTRAINT `fk_pay_order_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`),
  CONSTRAINT `fk_pay_order_coupon_instance` FOREIGN KEY (`coupon_instance_id`) REFERENCES `coupon_instance` (`id`),
  CONSTRAINT `chk_pay_order_channel` CHECK (`channel` IN ('WECHAT', 'ALIPAY')),
  CONSTRAINT `chk_pay_order_status` CHECK (`status` IN ('CREATED', 'PAYING', 'SUCCESS', 'FAILED', 'CLOSED')),
  CONSTRAINT `chk_pay_order_origin_amount` CHECK (`origin_amount` >= 0),
  CONSTRAINT `chk_pay_order_discount_amount` CHECK (`discount_amount` >= 0),
  CONSTRAINT `chk_pay_order_pay_amount` CHECK (`pay_amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付单';

CREATE TABLE IF NOT EXISTS `pay_transaction` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pay_order_no` VARCHAR(64) NOT NULL COMMENT '支付单号',
  `trade_type` VARCHAR(32) NOT NULL COMMENT 'UNIFIED_ORDER/CALLBACK/QUERY/CLOSE',
  `request_json` LONGTEXT NULL COMMENT '请求报文',
  `response_json` LONGTEXT NULL COMMENT '响应报文',
  `transaction_status` VARCHAR(32) NOT NULL COMMENT 'SUCCESS/FAIL',
  `error_code` VARCHAR(64) NULL COMMENT '错误码',
  `error_message` VARCHAR(255) NULL COMMENT '错误信息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_pay_txn_order_type` (`pay_order_no`, `trade_type`, `created_at`),
  CONSTRAINT `fk_pay_txn_pay_order` FOREIGN KEY (`pay_order_no`) REFERENCES `pay_order` (`pay_order_no`),
  CONSTRAINT `chk_pay_txn_status` CHECK (`transaction_status` IN ('SUCCESS', 'FAIL'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付渠道交互流水';

CREATE TABLE IF NOT EXISTS `payment_voucher` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '电子凭证ID',
  `pay_order_no` VARCHAR(64) NOT NULL COMMENT '支付单号',
  `bill_id` BIGINT NOT NULL COMMENT '账单ID',
  `account_id` BIGINT NOT NULL COMMENT '账号ID',
  `voucher_no` VARCHAR(64) NOT NULL COMMENT '凭证编号',
  `amount` DECIMAL(12,2) NOT NULL COMMENT '凭证金额',
  `status` VARCHAR(16) NOT NULL COMMENT 'ISSUED',
  `issued_at` DATETIME NOT NULL COMMENT '签发时间',
  `content_json` LONGTEXT NULL COMMENT '凭证内容JSON',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_voucher_pay_order` (`pay_order_no`),
  UNIQUE KEY `uk_payment_voucher_no` (`voucher_no`),
  KEY `idx_payment_voucher_account_created` (`account_id`, `created_at`),
  CONSTRAINT `fk_payment_voucher_pay_order` FOREIGN KEY (`pay_order_no`) REFERENCES `pay_order` (`pay_order_no`),
  CONSTRAINT `fk_payment_voucher_bill` FOREIGN KEY (`bill_id`) REFERENCES `bill` (`id`),
  CONSTRAINT `fk_payment_voucher_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='电子凭证';

CREATE TABLE IF NOT EXISTS `invoice_application` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '发票申请ID',
  `application_no` VARCHAR(64) NOT NULL COMMENT '申请单号',
  `bill_id` BIGINT NOT NULL COMMENT '账单ID',
  `pay_order_no` VARCHAR(64) NOT NULL COMMENT '支付单号',
  `account_id` BIGINT NOT NULL COMMENT '申请账号ID',
  `invoice_title` VARCHAR(128) NOT NULL COMMENT '发票抬头',
  `tax_no` VARCHAR(64) NULL COMMENT '税号',
  `status` VARCHAR(16) NOT NULL COMMENT 'APPLIED/APPROVED/REJECTED',
  `remark` VARCHAR(255) NULL COMMENT '备注',
  `applied_at` DATETIME NOT NULL COMMENT '申请时间',
  `processed_at` DATETIME NULL COMMENT '处理时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_invoice_application_no` (`application_no`),
  UNIQUE KEY `uk_invoice_application_pay_order` (`pay_order_no`),
  KEY `idx_invoice_application_account_status` (`account_id`, `status`, `applied_at`),
  CONSTRAINT `fk_invoice_application_bill` FOREIGN KEY (`bill_id`) REFERENCES `bill` (`id`),
  CONSTRAINT `fk_invoice_application_pay_order` FOREIGN KEY (`pay_order_no`) REFERENCES `pay_order` (`pay_order_no`),
  CONSTRAINT `fk_invoice_application_account` FOREIGN KEY (`account_id`) REFERENCES `account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='发票申请';

CREATE TABLE IF NOT EXISTS `dunning_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '催缴任务ID',
  `task_no` VARCHAR(64) NOT NULL COMMENT '催缴任务号',
  `bill_id` BIGINT NOT NULL COMMENT '账单ID',
  `group_id` BIGINT NULL COMMENT '用户组ID',
  `org_unit_id` BIGINT NULL COMMENT '组织ID',
  `tenant_code` VARCHAR(64) NULL COMMENT '租户编码',
  `trigger_type` VARCHAR(16) NOT NULL COMMENT 'MANUAL/AUTO',
  `trigger_date` DATE NOT NULL COMMENT '触发日期',
  `status` VARCHAR(16) NOT NULL COMMENT 'SENT',
  `remark` VARCHAR(255) NULL COMMENT '备注',
  `executed_at` DATETIME NOT NULL COMMENT '执行时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dunning_task_bill_trigger` (`bill_id`, `trigger_type`, `trigger_date`),
  UNIQUE KEY `uk_dunning_task_no` (`task_no`),
  KEY `idx_dunning_task_group_status` (`group_id`, `status`, `trigger_date`),
  KEY `idx_dunning_task_tenant_status` (`tenant_code`, `status`, `trigger_date`),
  CONSTRAINT `fk_dunning_task_bill` FOREIGN KEY (`bill_id`) REFERENCES `bill` (`id`),
  CONSTRAINT `fk_dunning_task_group` FOREIGN KEY (`group_id`) REFERENCES `user_group` (`id`),
  CONSTRAINT `fk_dunning_task_org_unit` FOREIGN KEY (`org_unit_id`) REFERENCES `org_unit` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='催缴任务';

CREATE TABLE IF NOT EXISTS `dunning_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '催缴日志ID',
  `task_id` BIGINT NOT NULL COMMENT '催缴任务ID',
  `bill_id` BIGINT NOT NULL COMMENT '账单ID',
  `send_channel` VARCHAR(16) NOT NULL COMMENT 'SYSTEM',
  `status` VARCHAR(16) NOT NULL COMMENT 'SENT',
  `content` VARCHAR(500) NOT NULL COMMENT '发送内容',
  `sent_at` DATETIME NOT NULL COMMENT '发送时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_dunning_log_bill_sent` (`bill_id`, `sent_at`),
  CONSTRAINT `fk_dunning_log_task` FOREIGN KEY (`task_id`) REFERENCES `dunning_task` (`id`),
  CONSTRAINT `fk_dunning_log_bill` FOREIGN KEY (`bill_id`) REFERENCES `bill` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='催缴日志';

CREATE TABLE IF NOT EXISTS `coupon_redemption` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `coupon_instance_id` BIGINT NOT NULL COMMENT '券实例ID',
  `redeem_type` VARCHAR(16) NOT NULL COMMENT 'PAYMENT/VOUCHER',
  `pay_order_no` VARCHAR(64) NULL COMMENT '支付单号',
  `redeem_target` VARCHAR(128) NULL COMMENT '兑换对象或目标',
  `operator_id` BIGINT NULL COMMENT '操作人',
  `remark` VARCHAR(255) NULL COMMENT '备注',
  `redeemed_at` DATETIME NOT NULL COMMENT '核销时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_redemption_coupon` (`coupon_instance_id`),
  KEY `idx_coupon_redemption_pay_order` (`pay_order_no`),
  CONSTRAINT `fk_coupon_redemption_coupon` FOREIGN KEY (`coupon_instance_id`) REFERENCES `coupon_instance` (`id`),
  CONSTRAINT `fk_coupon_redemption_pay_order` FOREIGN KEY (`pay_order_no`) REFERENCES `pay_order` (`pay_order_no`),
  CONSTRAINT `fk_coupon_redemption_operator` FOREIGN KEY (`operator_id`) REFERENCES `account` (`id`),
  CONSTRAINT `chk_coupon_redemption_type` CHECK (`redeem_type` IN ('PAYMENT', 'VOUCHER'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='券核销记录';

CREATE TABLE IF NOT EXISTS `import_batch` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '批次ID',
  `batch_no` VARCHAR(64) NOT NULL COMMENT '批次号',
  `import_type` VARCHAR(16) NOT NULL COMMENT 'BILL/WATER_READING',
  `file_url` VARCHAR(255) NOT NULL COMMENT '源文件地址',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PROCESSING' COMMENT 'PROCESSING/SUCCESS/FAILED',
  `total_count` INT NOT NULL DEFAULT 0 COMMENT '总行数',
  `success_count` INT NOT NULL DEFAULT 0 COMMENT '成功数',
  `fail_count` INT NOT NULL DEFAULT 0 COMMENT '失败数',
  `operator_id` BIGINT NULL COMMENT '操作人账号ID',
  `finished_at` DATETIME NULL COMMENT '完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_import_batch_no` (`batch_no`),
  KEY `idx_import_batch_status_created` (`status`, `created_at`),
  CONSTRAINT `fk_import_batch_operator` FOREIGN KEY (`operator_id`) REFERENCES `account` (`id`),
  CONSTRAINT `chk_import_batch_type` CHECK (`import_type` IN ('BILL', 'WATER_READING')),
  CONSTRAINT `chk_import_batch_status` CHECK (`status` IN ('PROCESSING', 'SUCCESS', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导入批次';

CREATE TABLE IF NOT EXISTS `import_row_error` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `batch_id` BIGINT NOT NULL COMMENT '批次ID',
  `row_no` INT NOT NULL COMMENT '文件行号',
  `error_code` VARCHAR(32) NOT NULL COMMENT '错误码',
  `error_message` VARCHAR(255) NOT NULL COMMENT '错误信息',
  `raw_data` LONGTEXT NULL COMMENT '原始行数据',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_import_row_error_batch_row` (`batch_id`, `row_no`),
  CONSTRAINT `fk_import_row_error_batch` FOREIGN KEY (`batch_id`) REFERENCES `import_batch` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导入行级错误';

CREATE TABLE IF NOT EXISTS `export_job` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '导出任务ID',
  `job_no` VARCHAR(64) NOT NULL COMMENT '导出任务号',
  `export_type` VARCHAR(16) NOT NULL COMMENT 'BILL/REPORT',
  `request_json` LONGTEXT NULL COMMENT '导出参数',
  `file_url` VARCHAR(255) NULL COMMENT '导出文件地址',
  `status` VARCHAR(16) NOT NULL DEFAULT 'PROCESSING' COMMENT 'PROCESSING/SUCCESS/FAILED',
  `operator_id` BIGINT NULL COMMENT '操作人账号ID',
  `expired_at` DATETIME NULL COMMENT '下载过期时间',
  `finished_at` DATETIME NULL COMMENT '完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_by` BIGINT NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `updated_by` BIGINT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_export_job_no` (`job_no`),
  KEY `idx_export_job_status_created` (`status`, `created_at`),
  CONSTRAINT `fk_export_job_operator` FOREIGN KEY (`operator_id`) REFERENCES `account` (`id`),
  CONSTRAINT `chk_export_job_type` CHECK (`export_type` IN ('BILL', 'REPORT')),
  CONSTRAINT `chk_export_job_status` CHECK (`status` IN ('PROCESSING', 'SUCCESS', 'FAILED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='导出任务';

CREATE TABLE IF NOT EXISTS `audit_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `biz_type` VARCHAR(32) NOT NULL COMMENT 'BILL/PAYMENT/COUPON/AUTH/IMPORT/EXPORT',
  `biz_id` VARCHAR(64) NOT NULL COMMENT '业务主键',
  `action` VARCHAR(32) NOT NULL COMMENT 'CREATE/UPDATE/CANCEL/IMPORT/EXPORT/CALLBACK',
  `operator_id` BIGINT NULL COMMENT '操作人',
  `ip` VARCHAR(64) NULL COMMENT '来源IP',
  `user_agent` VARCHAR(255) NULL COMMENT 'UA',
  `detail_json` LONGTEXT NULL COMMENT '明细JSON',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_audit_log_biz` (`biz_type`, `biz_id`, `created_at`),
  KEY `idx_audit_log_operator_created` (`operator_id`, `created_at`),
  CONSTRAINT `fk_audit_log_operator` FOREIGN KEY (`operator_id`) REFERENCES `account` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审计日志';

CREATE INDEX `idx_bill_room_fee_status_period` ON `bill` (`room_id`, `fee_type`, `status`, `period_year`, `period_month`);
CREATE INDEX `idx_bill_fee_period_group_status` ON `bill` (`fee_type`, `period_year`, `period_month`, `group_id`, `status`);
CREATE INDEX `idx_pay_order_status_created` ON `pay_order` (`status`, `created_at`);
CREATE INDEX `idx_coupon_instance_account_status_template` ON `coupon_instance` (`owner_account_id`, `status`, `template_id`, `expires_at`);

SET FOREIGN_KEY_CHECKS = 1;
