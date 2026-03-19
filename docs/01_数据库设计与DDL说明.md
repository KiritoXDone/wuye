# 物业管理系统数据库设计与 DDL 说明

> 版本：V1.0  
> 适用：MySQL 8 + Spring Boot + MyBatis  
> 目标：给后端开发、DBA、测试一份可以直接建库、建表、对字段和索引口径达成一致的实施文档

---

## 1. 文档目标

这份文档用于把主开发文档里的数据库部分单独拆出来，变成一份可以直接执行和落地的建库文档。

适用的核心口径：
- 账单主体永远是 `room`
- 同一房间同一费用类型同一账期只能有一张账单
- 物业费按面积计费，水费按抄表计费
- Resident / Agent / Admin 通过账号类型 + 数据范围控制权限
- 支付、券、导入导出、审计都独立建模，避免后续模块互相污染
- 为支持 **Web 管理端**，账号表预留 `username/password_hash`

---

## 2. 建库原则

### 2.1 基础约定

- 数据库：`MySQL 8`
- 字符集：`utf8mb4`
- 存储引擎：`InnoDB`
- 主键：统一 `bigint`
- 时间字段：统一 `created_at / created_by / updated_at / updated_by`
- 金额字段：统一 `decimal(12,2)`
- 水表读数 / 用量：统一 `decimal(12,3)`
- 业务状态：统一使用 `varchar` 保存枚举值，方便扩展

### 2.2 关于外键

本脚本使用了**适度外键**，目的是：
- 在从零开发初期尽量保证主从关系不被写坏
- 让测试阶段更快暴露脏数据问题
- 降低多人并行开发时的误写概率

如果后续你要做大批量导入、历史迁移、分库分表，届时可以再评估是否弱化部分外键约束。

---

## 3. 建表顺序

建议执行顺序：

### 第一批：账号、房间、分组
1. `community`
2. `account`
3. `account_identity`
4. `user_group`
5. `house_type`
6. `room`
7. `account_room`
8. `group_room`
9. `agent_profile`
10. `agent_group`

### 第二批：计费、抄表、账单
11. `fee_rule`
12. `water_meter`
13. `water_meter_reading`
14. `bill`
15. `bill_line`

### 第三批：券与支付
16. `coupon_template`
17. `coupon_issue_rule`
18. `coupon_instance`
19. `pay_order`
20. `pay_transaction`
21. `coupon_redemption`

### 第四批：导入导出与审计
22. `import_batch`
23. `import_row_error`
24. `export_job`
25. `audit_log`

---

## 4. 关键唯一约束

以下是最关键的几条唯一约束：

### 4.1 房间唯一定位
```sql
UNIQUE KEY uk_room_location (community_id, building_no, unit_no, room_no)
```

### 4.2 一个账号对同一房间只有一条绑定关系
```sql
UNIQUE KEY uk_account_room (account_id, room_id)
```

### 4.3 同一户同一账期只允许一条抄表记录
```sql
UNIQUE KEY uk_water_reading_room_period (room_id, period_year, period_month)
```

### 4.4 同一房间同一费用类型同一账期只允许一张账单
```sql
UNIQUE KEY uk_bill_room_fee_period (room_id, fee_type, period_year, period_month)
```

### 4.5 支付幂等键唯一
```sql
UNIQUE KEY uk_pay_order_idempotency_key (idempotency_key)
```

### 4.6 券实例默认只能核销一次
```sql
UNIQUE KEY uk_coupon_redemption_coupon (coupon_instance_id)
```

---

## 5. 表分层说明

## 5.1 主数据层

### community
物业项目 / 小区主表。

### account
统一账号表，服务于：
- 住户
- Agent
- 管理员
- 财务

为了支持你新增的 **Web 管理端**，在账号表里加了：
- `username`
- `password_hash`

### account_identity
保存微信 / 支付宝等第三方身份映射。小程序登录优先命中这张表。

### house_type / room
- `house_type`：户型默认面积来源
- `room`：全系统的账单主体、统计主体、权限绑定主体

---

## 5.2 权限与数据范围层

### account_room
解决“一个房间可绑定多个账号，共享账单与支付结果”的核心表。

### user_group / group_room / agent_group
这三张表一起完成 Agent 的分组授权模型：
- `user_group`：定义一个组
- `group_room`：定义哪些房间属于该组
- `agent_group`：定义哪个 Agent 能看哪个组，以及可查看 / 可管理

---

## 5.3 计费与账单层

### fee_rule
统一管理物业费 / 水费规则。

建议：
- `PROPERTY` 和 `WATER` 都走这张表
- 通过 `effective_from / effective_to` 控制调价生效区间

补充口径：
- `PROPERTY` 物业费支持 `cycle_type=MONTH`（月）与 `cycle_type=YEAR`（年）。
- `WATER` 水费当前仅支持 `cycle_type=MONTH`（月）。
- 当物业费规则为 `YEAR` 时，`unit_price` 语义为“元/㎡/年”；当为 `MONTH` 时，`unit_price` 语义为“元/㎡/月”。
- YEAR 费率不会生成独立的年度账单，系统仍按 `bill.period_year + bill.period_month` 维护月账单，只是在开单时把年单价自动折算到当月账单金额。

P2 增强口径：
- 水费规则继续保留 `unit_price` 作为兼容基础单价
- 新增 `pricing_mode` 区分 `FLAT / TIERED`
- 新增 `abnormal_abs_threshold`、`abnormal_multiplier_threshold`，用于异常用量预警判定
- 阶梯水价明细拆到 `fee_rule_water_tier`，不要把分段 JSON 直接塞进主表

### fee_rule_water_tier
水费阶梯价明细表，用于表达按用量区间分段计费：
- `start_usage`：起始用量
- `end_usage`：结束用量，可空表示“以上”
- `unit_price`：该段单价
- `tier_order`：分段顺序，保证前端和开单计算口径一致

### water_meter / water_meter_reading
水费计算事实来源。

推荐口径：
- `prev_reading` 默认取上期 `curr_reading`
- `curr_reading >= prev_reading`
- 抄表要记录 `read_by_admin_id`、`read_at`、`photo_url`
- P2 可把异常抄表状态从 `NORMAL` 扩展到 `ABNORMAL`，但不改变“同一房间同账期只能有一条抄表记录”的唯一口径

### water_usage_alert
异常用量预警事件表。

最小设计建议：
- 一条抄表记录可命中多个预警码，例如绝对阈值、环比倍数阈值
- 用 `UNIQUE(reading_id, alert_code)` 保证同一预警类型不会重复落表
- 预警事件只做提示与追踪，不应阻断正常开单主链路

### bill / bill_line
- `bill`：存汇总金额与状态
- `bill_line`：存计算依据

建议：
- 物业费 `ext_json` 记录面积、单价、公式、面积来源
- 水费 `ext_json` 记录期初、期末、用量、水表 ID

补充口径：
- 物业费使用 YEAR 费率时，`bill` 仍然是一张月账单。
- 对应 `bill_line.ext_json` 应保留 `cycleType`、`annualUnitPrice`、`monthlyUnitPrice` 和折算公式，便于财务解释和审计追踪。

---

## 5.4 支付与券层

### pay_order
支付单主表，负责：
- 绑定账单
- 绑定下单账号
- 绑定支付渠道
- 绑定券抵扣
- 记录幂等键

P2 补充口径：
- 为支持物业费按年缴纳，`pay_order` 可增加是否年度支付、覆盖账单数量等附加字段。
- 年度支付仍锚定一张主账单，但真实覆盖范围应通过独立关系表记录，而不是只塞进 `remark`。

### pay_order_bill_cover
支付单覆盖账单关系表。

最小设计建议：
- 一笔年度物业费支付可覆盖同一房间同一自然年的 12 张月度物业费账单。
- 用 `(pay_order_no, bill_id)` 唯一约束保证覆盖关系不重复。
- 查询、排障、审计时以这张表判断“哪一笔支付覆盖了哪些月账单”。

### pay_transaction
保存渠道请求 / 回调 / 查单 / 关单痕迹，便于排障和审计。

### payment_voucher
支付成功后的电子凭证表。

建议：
- 一张支付单只生成一张电子凭证，`pay_order_no` 唯一
- 内容以已支付账单与支付单事实为准，生成后只读

### invoice_application
住户发票申请记录表。

最小设计建议：
- 只允许针对已支付账单申请
- `pay_order_no` 唯一，避免同一支付单重复申请
- 后台处理仅推进 `APPLIED / APPROVED / REJECTED` 三态，不引入税控系统复杂状态机

### coupon_template / coupon_issue_rule / coupon_instance / coupon_redemption
建议券系统拆四层：
- 模板
- 发券规则
- 实例
- 核销记录

其中 `coupon_issue_rule` 是对原文档“支付后发券规则”的结构化落地，避免把规则硬编码在代码里。

---

## 5.5 任务与审计层

### org_unit
轻量组织架构与租户节点表。

设计原则：
- 只表达“租户编码 + 组织树 + 小区映射”
- `user_group`、`agent_profile` 通过 `org_unit_id` 关联组织
- 不做 SaaS 级强隔离，不改变现有 `Agent -> user_group -> group_room` 的数据权限主语义

### dunning_task / dunning_log
自动催缴任务与发送日志。

最小设计建议：
- `dunning_task` 只面向逾期未支付账单生成
- `dunning_log` 记录本次催缴的渠道、状态和文案
- 同一账单在同一触发日期、同一触发类型下只保留一条任务，避免重复催缴

### import_batch / import_row_error
支持账单导入和抄表导入，必须保留行级错误。

### export_job
导出任务建议异步化，不要前端长连接等待大文件生成。

### audit_log
最少记录：
- 账单新增 / 修改 / 作废
- 支付单创建 / 回调入账
- 发券 / 核销
- 导入 / 导出
- 登录 / 授权操作

---

## 6. 高频索引建议

### 6.1 住户查账单
```sql
CREATE INDEX idx_bill_room_fee_status_period
ON bill (room_id, fee_type, status, period_year, period_month);
```

### 6.2 Agent / Admin 查月报
```sql
CREATE INDEX idx_bill_fee_period_group_status
ON bill (fee_type, period_year, period_month, group_id, status);
```

### 6.3 支付回调 / 渠道查单
```sql
CREATE INDEX idx_pay_order_status_created
ON pay_order (status, created_at);
```

### 6.4 券列表
```sql
CREATE INDEX idx_coupon_instance_account_status_template
ON coupon_instance (owner_account_id, status, template_id, expires_at);
```

---

## 7. 必须在应用层同步校验的规则

虽然脚本已经加了不少约束，但这些规则仍然必须在应用层再校验一次：

- 账单已支付时禁止重复下单
- 券已使用 / 已过期 / 已锁定时禁止抵扣
- 抄表 `curr_reading < prev_reading` 时直接拒绝
- 已支付账单禁止直接覆盖，应走作废 / 补差 / 冲正流程
- 同一 `payOrderNo` 回调重复到达时，只能推进一次状态
- 阶梯水价必须保证阶梯区间有序且不倒挂
- 异常预警命中后允许继续开单，但必须保留预警事件与抄表状态
- 催缴仅针对逾期且未支付账单生成，已支付账单禁止再进催缴任务
- 发票申请仅允许基于成功支付单创建，且一张支付单只允许一条申请记录

---

## 8. 与接口文档的对应关系

请与以下文档配套阅读：

- `02_后端接口联调文档.md`
- `03_前端实施补充_页面与交互规范.md`

主要对应关系：

| 接口模块 | 主要表 |
|---|---|
| 登录 | `account`、`account_identity` |
| 房间绑定 | `room`、`account_room` |
| Agent 授权 | `user_group`、`group_room`、`agent_profile`、`agent_group` |
| 计费与账单 | `fee_rule`、`water_meter`、`water_meter_reading`、`bill`、`bill_line` |
| 支付 | `pay_order`、`pay_transaction` |
| P2 计费增强 | `fee_rule_water_tier`、`water_usage_alert` |
| P2 组织与催缴 | `org_unit`、`dunning_task`、`dunning_log` |
| P2 凭证与发票 | `payment_voucher`、`invoice_application` |
| 券 | `coupon_template`、`coupon_issue_rule`、`coupon_instance`、`coupon_redemption` |
| 导入导出 | `import_batch`、`import_row_error`、`export_job` |
| 审计 | `audit_log` |

---

## 9. 建议的落地顺序

1. 先执行 `01_物业管理系统_DDL与索引脚本.sql` 建库  
2. 先把 `account / room / account_room / bill / pay_order` 这几张 P0 表跑通  
3. 再补 `coupon_*`、`import_*`、`export_job`、`audit_log`  
4. 最后再做报表优化索引和性能调优

---

## 10. 我建议你第一次联调时只先验证这些点

- 同一房间可绑定多个账号
- 同一房间同一账期只有一张账单
- 住户查询账单时只按房间口径取数
- 重复回调不重复入账
- 券并发核销不会重复使用
- Agent 只能看到授权用户组
