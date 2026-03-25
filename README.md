# 物业管理系统

面向真实上线场景的物业管理系统，覆盖房间、账单、抄表、支付、报表、审计以及多端协同闭环。项目当前以生产实施口径推进，重点不是“从零画原型”，而是围绕可运行、可核对、可回滚的业务链路持续完善。

## 项目重点

- 账单主体固定为 `room`，不是 `account`
- 物业费按面积计费，按自然年开单、按年缴纳
- 水费按月抄表，录入后立即生成当月账单
- 账单按费用类型拆分，物业费和水费分单管理
- 报表默认按房间统计，避免一个房间绑定多个账号时重复计户
- Web 管理端基线为 `React + Tailwind CSS + TypeScript`
- 支付、账单、审计、导入导出等关键环节按生产交付思路设计

## 项目优势

- 业务口径稳定：围绕房间、年度物业费、月度水费三条主规则冻结核心语义，避免需求扩展时口径漂移
- 闭环优先：不是孤立页面或单点接口，而是从规则、出账、查询、支付、统计到审计形成纵向链路
- 上线导向：兼顾发布、回滚、对账、审计与历史兼容性，减少“开发可用、生产不可落地”的问题
- 多端协同：后端、Web 管理端、微信小程序端均按真实业务角色拆分，便于后续继续扩展
- 可维护性强：后端采用 Spring Boot + MyBatis 的清晰分层，前端采用 React + Tailwind 的轻量栈，便于持续迭代

## 核心业务规则

- 房间是账单、支付、统计的核心主体
- 多个账号可以绑定同一个房间，共享该房间账单与支付结果
- 物业费唯一约束：同一房间同一年只能有一张有效物业费账单
- 水费唯一约束：同一房间同一月只能有一条有效抄表记录和一张有效水费账单
- 已支付账单不能直接覆盖，支付回调必须支持重复回调幂等
- 报表、缴费率、欠费户数等统计口径默认按房间，不按账号

## 技术栈

### 后端

- Java 17
- Spring Boot 3.x
- MyBatis
- MySQL 8
- Redis
- RabbitMQ
- Maven
- Flyway

### Web 管理端

- React 18
- TypeScript
- Tailwind CSS
- Vite
- React Router
- Axios
- Zustand

### 微信小程序端

- 原生微信小程序
- TypeScript

### 发布与运维

- Nginx 托管 Web 管理端静态构建产物
- 支持静态包快速回滚
- 预留支付对账、审计、导入导出、对象存储等生产能力扩展空间

## 当前已实现能力

### 后端闭环

- JWT 鉴权、统一响应、统一异常处理
- Resident 微信登录开发桩
- Admin 密码登录
- 房间绑定与“我的房间”查询
- 年度物业费规则管理与年度开单
- 水表配置、月度抄表、录入即出账
- 账单列表、账单详情、支付创建、支付结果查询
- 管理端缴费统计、手动标记已缴、费用规则、水费录入
- 报表、导入导出、催缴、发票申请、审计日志基础能力

### Web 管理端页面

- `/dashboard`
- `/household-payments`
- `/billing-generate`
- `/water-readings`
- `/bills`
- `/dunning`
- `/fee-rules`
- `/invoice-applications`

### Resident 端链路

- 微信登录
- 我的房间
- 账单列表
- 账单详情
- 创建支付单
- 支付结果轮询

## 工程结构

```text
backend/                 Spring Boot 后端工程
frontend/admin-web/      React + Tailwind Web 管理端
frontend/miniapp-wechat/ 微信小程序 Resident 端
docs/                    上线基线与专项文档
ops/                     运维、脚本与部署相关内容
```

## 快速开始

### 1. 启动后端

```bash
mvn -f backend/pom.xml spring-boot:run
```

本地 `dev` 默认配置：

- 服务端口：`8081`
- MySQL：`127.0.0.1:3306/wuye_system`
- 用户名：`root`
- 密码：`password`
- 管理员账号：`admin / 123456`
- 微信登录模式：`mock`

本地开发桩支持以下 Resident 演示 code：

- `resident-zhangsan`
- `resident-lisi`

如需切回真实微信登录，可显式设置 `APP_AUTH_WECHAT_MODE=real` 并补齐微信配置。

### 2. 运行后端测试

```bash
mvn -f backend/pom.xml test
```

### 3. 启动 Web 管理端

```bash
cd frontend/admin-web
npm install
npm run dev
```

默认开发地址：`http://127.0.0.1:5173`

### 4. 构建 Web 管理端

```bash
cd frontend/admin-web
npm run build
```

### 5. 启动微信小程序端

```bash
cd frontend/miniapp-wechat
npm install
```

然后使用微信开发者工具打开 `frontend/miniapp-wechat`。

## 关键接口

### Admin

- `POST /api/v1/admin/auth/login/password`
- `GET /api/v1/admin/dashboard/summary`
- `GET /api/v1/admin/bills`
- `GET /api/v1/admin/bills/{billId}`
- `GET /api/v1/admin/billing/households`
- `POST /api/v1/admin/bills/{billId}/mark-paid`
- `POST /api/v1/admin/bills/generate/property-yearly`
- `POST /api/v1/admin/water-readings`

### Resident

- `GET /api/v1/me/rooms`
- `GET /api/v1/me/bills`
- `GET /api/v1/bills/{billId}`
- `POST /api/v1/payments`
- `GET /api/v1/payments/{payOrderNo}`

## 最低冒烟关注点

- 同一房间同一年只能生成一张有效物业费账单
- 同一房间同一月只能录入一条有效抄表并生成一张有效水费账单
- 抄表录入后可立即在账单列表查到对应水费账单
- 已支付账单不会被直接覆盖
- 支付回调重复调用不会重复记账
- Resident / Agent / Admin 数据范围隔离正确

## 文档入口

- [docs/00_上线基线说明.md](/D:/Code/JAVA/wuye/docs/00_上线基线说明.md)
- [docs/README.md](/D:/Code/JAVA/wuye/docs/README.md)
- [frontend/admin-web/README.md](/D:/Code/JAVA/wuye/frontend/admin-web/README.md)
- [frontend/miniapp-wechat/README.md](/D:/Code/JAVA/wuye/frontend/miniapp-wechat/README.md)
- `docs/物业管理系统_生产实施与上线交付文档_v1.1.docx`
