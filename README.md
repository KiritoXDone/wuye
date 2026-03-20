# 物业管理系统

当前仓库以 `CLAUDE.md` 与 `docs/物业管理系统_生产实施与上线交付文档_v1.1.docx` 为上线基线，正在从过渡态收口到真实生产口径。

## 当前冻结口径

- 房间是账单主体，不是账号
- 物业费按面积计费、按年开单、按年缴纳
- 水费按月抄表，录入即出账
- 账单按费用类型分单
- 报表默认按房间统计
- 管理端目标栈为 React + Tailwind CSS + TypeScript

## 当前后端已收口范围

- Spring Boot 3 + MyBatis + Flyway 后端骨架
- 统一响应体、统一异常处理、JWT 鉴权
- Resident 微信登录（开发桩）
- Admin 密码登录
- 房间绑定 / 我的房间
- 年度物业费规则与年度开单主链路
- 水表配置、抄表录入、录入即出账
- Resident 账单列表 / 账单详情
- 单渠道支付闭环（本地开发桩 + 幂等回调）
- 与年度物业费 / 月度水费口径一致的报表、导入导出与集成测试
- 微信小程序最小 Resident 页面：登录、我的房间、房间账单、账单详情、支付结果

> 说明：当前支付与微信登录均为本地开发桩实现，用于跑通业务闭环；`/auth/logout` 为客户端丢弃 token 语义，不维护服务端黑名单失效机制。

## 目录结构

```text
backend/                 后端工程
frontend/admin-web/      Web 管理端工程
frontend/miniapp-wechat/ 微信小程序工程
docs/                    原始需求与设计文档
ops/                     运维与本地环境说明
```

## 本地运行

### 1. 创建数据库

默认连接：

- host: `127.0.0.1`
- port: `3306`
- db: `wuye_system`
- user: `root`
- password: `password`

Flyway 会在应用启动时自动建表和写入基础演示数据。

### 2. 启动后端

```powershell
"D:\Application\IntelliJ IDEA 2025.3\plugins\maven\lib\maven3\bin\mvn.cmd" -f backend/pom.xml spring-boot:run
```

### 3. 运行测试

```powershell
"D:\Application\IntelliJ IDEA 2025.3\plugins\maven\lib\maven3\bin\mvn.cmd" -f backend/pom.xml test
```

## 演示账号

- Admin
  - username: `admin`
  - password: `123456`
- Resident 开发登录 code
  - `resident-zhangsan`
  - `resident-lisi`

## 文档入口

- `CLAUDE.md`：仓库级协作与实现约束
- `docs/00_上线基线说明.md`：可 diff 的上线基线摘要
- `docs/README.md`：docs 导航与历史文档迁移说明
- `docs/物业管理系统_生产实施与上线交付文档_v1.1.docx`：当前上线基线原文

> 说明：旧的拆分 docs 已退出默认事实来源，不再作为当前实现基线。

## 前端目录说明

- `frontend/admin-web`：正在收口为 React + Tailwind + TypeScript 管理端，当前核心页面已切到 React 入口 `src/main.tsx`。
- `frontend/miniapp-wechat`：原生微信小程序 + TypeScript，已接通 Resident 登录、我的房间、房间账单、账单详情与支付结果页面。

## 本地手工验收

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File "ops/manual-qa.ps1"
```

脚本会基于当前后端和本地 MySQL 环境自动创建规则、开单、发起支付并轮询支付结果，输出当前账期的支付与月报摘要。
