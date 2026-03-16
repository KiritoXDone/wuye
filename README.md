# 物业管理系统 MVP

当前仓库按 `docs/` 文档落地一个 **backend-first** 的 MVP 闭环，优先交付可启动、可建库、可验证的后端能力，并预留前端目录骨架。

## 已实现范围

- Spring Boot 3 + MyBatis + Flyway 后端骨架
- 统一响应体、统一异常处理、JWT 鉴权
- Resident 微信登录（开发桩）
- Admin 密码登录
- 房间绑定 / 我的房间
- 物业费规则、物业费开单
- 水表配置、抄表录入、水费开单
- Resident 账单列表 / 账单详情
- 单渠道支付闭环（本地开发桩 + 幂等回调）
- Admin 月报表

> 说明：当前支付与微信登录均为本地开发桩实现，用于跑通 MVP 业务闭环；`/auth/logout` 为客户端丢弃 token 语义，不维护服务端黑名单失效机制。

## 目录结构

```text
backend/                 后端工程
frontend/admin-web/      Web 管理端目录骨架
frontend/miniapp-wechat/ 微信小程序目录骨架
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

## 前端目录说明

当前仅创建目录骨架与说明文件，本次 MVP 不额外扩展完整前端页面实现，后续可在既有后端接口之上继续接 Web 管理端与微信小程序。
