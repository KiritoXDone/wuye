# 物业管理系统

面向真实上线场景的物业管理系统，覆盖组织/房间、年度物业费、月度水费、居民缴费、支付回调、优惠券、报表审计、AI 助手、Web 管理端和微信小程序端。当前工程按“可运行、可核对、可回滚、可继续交付”的生产实施口径维护。

完整项目说明见 [docs/01_完整项目文档.md](docs/01_完整项目文档.md)。

## 核心口径

- 房间是账单、支付和统计的核心主体，不以账号作为账单主体。
- 一个房间可绑定多个居民账号，居民共享该房间的账单和支付结果。
- 物业费按面积计费，按自然年开单，按年缴纳。
- 水费按月抄表，录入后生成当月水费账单。
- 账单按费用类型拆分，物业费和水费分单管理。
- 已支付账单不能直接覆盖，支付回调和优惠券发放必须幂等。
- 报表默认按房间统计，避免多账号绑定同一房间时重复计户。

## 已实现能力

### 后端业务闭环

- JWT 鉴权、统一响应、统一异常处理、角色和数据范围拦截。
- Admin 密码登录、Resident 微信登录，支持本地 mock 与真实微信 `jscode2session` 模式。
- 小区、房型、房间、账号、房间绑定和授权组管理。
- 年度物业费规则、年度物业费批量开单、月度水表读数、月度水费批量开单。
- 账单列表、账单详情、居民支付创建、支付状态查询、支付凭证。
- 支付回调幂等、数据库行锁/CAS 防重、Redis 回调锁降级、RabbitMQ 支付成功事件边界。
- 管理端缴费统计、线下手动标记已缴、催缴、发票申请、审计日志。
- 优惠券模板、发放规则、手工发券、可用券校验、兑换券、优惠券秒杀活动。
- 导入导出基础能力，限制导入文件目录，拒绝远程 URL 和路径逃逸。

### AI 助手

- AI Runtime 管理，支持 provider、model、apiBaseUrl、timeout、maxTokens、temperature。
- API Key 加密落库、脱敏展示，兼容历史明文读取。
- 内置 Agent 支持会话、会话列表、会话详情、SSE 流式输出和上下文延续。
- Agent 会缓存会话列表/详情；Redis 未启用时自动使用空实现降级。
- 高风险动作采用“预览参数 -> 确认执行 -> 结果回写”机制。
- 已接通动作包括：
  - 房间创建、房间停用
  - 账单详情、按房间查账单、居民账单摘要、近期活动
  - 创建支付单、查询支付单
  - 录入水表抄表
  - 生成年度物业费账单
  - 生成月度水费账单

### 多端

- Web 管理端：`React 18 + TypeScript + Tailwind CSS + Vite`。
- 微信小程序端：原生微信小程序 + TypeScript，覆盖 Resident 登录、房间、账单、支付和智能助手。
- 后端：`Java 17 + Spring Boot 3.3 + MyBatis + MySQL 8 + Flyway`，Redis/RabbitMQ 可按环境开启。

## 工程结构

```text
backend/                 Spring Boot 后端工程
frontend/admin-web/      React + Tailwind Web 管理端
frontend/miniapp-wechat/ 微信小程序 Resident 端
docs/                    项目文档、上线基线与专项说明
ops/                     运维、脚本与部署相关内容
```

## 环境要求

- JDK 17
- Maven 3.9+
- Node.js 18+
- npm 9+
- MySQL 8
- Redis，可选；开启后用于支付回调锁、秒杀库存锁、Agent 会话缓存
- RabbitMQ，可选；开启后用于支付成功事件和优惠券秒杀订单异步消费

## 快速启动

### 1. 后端

本地开发至少需要 MySQL 和必要密钥。建议用环境变量显式注入：

```bash
export APP_JWT_SECRET='replace-with-a-long-random-secret'
export APP_SECURITY_CRYPTO_CONFIG_ENCRYPTION_KEY='replace-with-32-byte-key'
export APP_BOOTSTRAP_ADMIN_PASSWORD='123456'
export APP_AUTH_WECHAT_MODE=mock

mvn -f backend/pom.xml spring-boot:run
```

默认服务端口为 `8081`。本地 mock 微信登录支持：

- `resident-zhangsan`
- `resident-lisi`

生产或预发环境应设置 `APP_AUTH_WECHAT_MODE=real` 并补齐微信小程序配置。

### 2. Web 管理端

```bash
cd frontend/admin-web
npm install
npm run dev
```

默认开发地址：`http://127.0.0.1:5173`。

构建：

```bash
cd frontend/admin-web
npm run build
```

### 3. 微信小程序端

```bash
cd frontend/miniapp-wechat
npm install
npm run typecheck
```

然后用微信开发者工具打开 `frontend/miniapp-wechat`。

## 关键配置

基础安全配置：

```bash
APP_JWT_SECRET=
APP_SECURITY_CRYPTO_CONFIG_ENCRYPTION_KEY=
APP_BOOTSTRAP_ADMIN_USERNAME=admin
APP_BOOTSTRAP_ADMIN_PASSWORD=
```

微信登录：

```bash
APP_AUTH_WECHAT_MODE=real
APP_AUTH_WECHAT_APP_ID=
APP_AUTH_WECHAT_APP_SECRET=
APP_AUTH_WECHAT_JSCODE2SESSION_URL=https://api.weixin.qq.com/sns/jscode2session
```

Redis：

```bash
APP_INFRA_REDIS_ENABLED=true
REDIS_HOST=127.0.0.1
REDIS_PORT=6379
APP_INFRA_REDIS_KEY_PREFIX=wuye:
APP_INFRA_REDIS_CALLBACK_LOCK_SECONDS=300
APP_INFRA_REDIS_SECKILL_LOCK_SECONDS=10
```

RabbitMQ：

```bash
APP_INFRA_RABBIT_ENABLED=true
RABBITMQ_HOST=127.0.0.1
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=guest
RABBITMQ_PASSWORD=guest
APP_INFRA_RABBIT_PAYMENT_EXCHANGE=wuye.payment.events
APP_INFRA_RABBIT_PAYMENT_SUCCESS_QUEUE=wuye.payment.success
APP_INFRA_RABBIT_COUPON_EXCHANGE=wuye.coupon.events
APP_INFRA_RABBIT_COUPON_SECKILL_QUEUE=wuye.coupon.seckill.order
```

AI Runtime：

```bash
APP_AI_RUNTIME_ENABLED=true
APP_AI_RUNTIME_PROVIDER=openai
APP_AI_RUNTIME_API_BASE_URL=https://api.openai.com/v1
APP_AI_RUNTIME_MODEL=gpt-4o-mini
APP_AI_RUNTIME_API_KEY=
```

## 常用接口

Admin：

- `POST /api/v1/admin/auth/login/password`
- `GET /api/v1/admin/dashboard/summary`
- `GET /api/v1/admin/bills`
- `GET /api/v1/admin/billing/households`
- `POST /api/v1/admin/bills/generate/property-yearly`
- `POST /api/v1/admin/bills/generate/water`
- `POST /api/v1/admin/bills/{billId}/mark-paid`
- `POST /api/v1/admin/coupons/seckill-campaigns`

Resident：

- `POST /api/v1/auth/login/wechat`
- `GET /api/v1/me/rooms`
- `GET /api/v1/me/bills`
- `GET /api/v1/bills/{billId}`
- `POST /api/v1/payments`
- `GET /api/v1/payments/{payOrderNo}`
- `GET /api/v1/me/coupons`
- `POST /api/v1/coupons/seckill/{campaignId}/orders`
- `GET /api/v1/coupons/seckill/orders/{orderNo}`

AI：

- `GET /api/v1/admin/agent/runtime-config`
- `PUT /api/v1/admin/agent/runtime-config`
- `GET /api/v1/ai/agent/me/bill-summary`
- `GET /api/v1/ai/agent/admin/bill-stats`
- `GET /api/v1/ai/agent/activities/recent`
- `POST /api/v1/ai/agent/commands/preview`
- `POST /api/v1/ai/agent/commands/confirm`
- `POST /api/v1/ai/agent/conversation`
- `POST /api/v1/ai/agent/conversation/stream`

## 验证命令

```bash
mvn -f backend/pom.xml -DskipTests test-compile

cd frontend/admin-web
npm run build

cd ../miniapp-wechat
npm run typecheck
```

需要完整集成测试时，准备 MySQL 8 测试库后运行：

```bash
cd backend
MYSQL_TEST_PORT=13307 MYSQL_TEST_USER=root MYSQL_TEST_PASSWORD= \
  mvn test -Dtest=BuiltInAgentCommandIntegrationTest,CouponSeckillIntegrationTest,PaymentIdempotencyIntegrationTest,InfraAndAiRuntimeConfigIntegrationTest,AuthAndResidentRoomIntegrationTest
```

## 文档

- [完整项目文档](docs/01_完整项目文档.md)
- [上线基线说明](docs/00_上线基线说明.md)
- [真实微信登录对接指南](docs/05_真实微信登录对接指南.md)
- [真实支付渠道对接指南](docs/06_真实支付渠道对接指南.md)
- [安全加固与环境变量注入说明](docs/07_安全加固与环境变量注入说明.md)
