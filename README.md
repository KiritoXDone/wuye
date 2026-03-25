# 物业管理系统

当前仓库以“上线冻结口径”为准，不再沿用旧的从零开发假设。处理需求、设计和实现时，事实来源优先级如下：

1. `AGENTS.md`
2. `docs/物业管理系统_生产实施与上线交付文档_v1.1.docx`
3. `docs/00_上线基线说明.md`
4. 当前代码实现

## 业务基线

- 账单主体是 `room`，不是 `account`
- 物业费按面积计费、按年开单、按年缴纳
- 水费按月抄表，录入后立即生成当月账单
- 账单按费用类型分单，不能把物业费和水费混成一张总账单
- 报表默认按房间统计，不按账号统计

## 技术栈

- 后端：Java 17、Spring Boot 3、MyBatis、MySQL 8、Redis、RabbitMQ、Maven
- Web 管理端：React、TypeScript、Tailwind CSS、Vite、React Router
- 微信小程序端：原生小程序 + TypeScript
- 发布基线：Web 管理端构建产物由 Nginx 托管，需支持静态包快速回滚

## 当前已打通的闭环

- JWT 鉴权、Resident 微信登录开发桩、Admin 密码登录
- 房间绑定与“我的房间”查询
- 年度物业费规则与年度开单
- 水表配置、月度抄表、录入即出账
- Resident 账单列表、账单详情、支付创建、支付结果查询
- 管理端账单列表、缴费统计、手动标记已缴、费用规则、水费录入
- 报表、导入导出、催缴、发票申请、审计日志基础能力

## 目录结构

```text
backend/                 Spring Boot 后端工程
frontend/admin-web/      React + Tailwind Web 管理端
frontend/miniapp-wechat/ 微信小程序 Resident 端
docs/                    上线基线与专项文档
ops/                     运维、脚本与部署相关内容
```

## 本地启动

### 1. 启动后端

```bash
mvn -f backend/pom.xml spring-boot:run
```

本地 `dev` 配置默认使用：

- 服务端口：`8081`
- MySQL：`127.0.0.1:3306/wuye_system`
- 用户名：`root`
- 密码：`password`
- 管理员账号：`admin / 123456`
- 微信登录模式：`mock`

本地开发桩可直接使用以下演示 code：

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

## 关键页面与接口

### 管理端页面

- `/dashboard`
- `/household-payments`
- `/billing-generate`
- `/water-readings`
- `/bills`
- `/dunning`
- `/fee-rules`
- `/invoice-applications`

### 当前关键接口

- `POST /api/v1/admin/auth/login/password`
- `GET /api/v1/admin/dashboard/summary`
- `GET /api/v1/admin/bills`
- `GET /api/v1/admin/bills/{billId}`
- `GET /api/v1/admin/billing/households`
- `POST /api/v1/admin/bills/{billId}/mark-paid`
- `POST /api/v1/admin/bills/generate/property-yearly`
- `POST /api/v1/admin/water-readings`
- `GET /api/v1/me/rooms`
- `GET /api/v1/me/bills`
- `GET /api/v1/bills/{billId}`
- `POST /api/v1/payments`
- `GET /api/v1/payments/{payOrderNo}`

## 文档入口

- [docs/00_上线基线说明.md](/D:/Code/JAVA/wuye/docs/00_上线基线说明.md)
- [docs/README.md](/D:/Code/JAVA/wuye/docs/README.md)
- [frontend/admin-web/README.md](/D:/Code/JAVA/wuye/frontend/admin-web/README.md)
- [frontend/miniapp-wechat/README.md](/D:/Code/JAVA/wuye/frontend/miniapp-wechat/README.md)
- `docs/物业管理系统_生产实施与上线交付文档_v1.1.docx`

## 最低冒烟关注点

- 同一房间同一年只能生成一张有效物业费账单
- 同一房间同一月只能录入一条有效抄表并生成一张有效水费账单
- 抄表录入后可立即在账单列表查到对应水费账单
- 已支付账单不会被直接覆盖
- 支付回调重复调用不会重复记账
- Resident / Agent / Admin 数据范围隔离正确
