# admin-web

物业管理系统 Web 管理端 MVP，基于 **Vue 3 + TypeScript + Vite + Element Plus + Pinia + Vue Router** 搭建。

当前已接通的真实后台接口：

- `POST /api/v1/admin/auth/login/password`
- `GET /api/v1/admin/dashboard/summary`
- `GET /api/v1/admin/reports/monthly`
- `GET /api/v1/admin/bills`
- `GET /api/v1/admin/bills/{billId}`
- `GET /api/v1/admin/fee-rules`
- `POST /api/v1/admin/fee-rules`
- `GET /api/v1/admin/water-readings`
- `POST /api/v1/admin/water-readings`
- `POST /api/v1/admin/water-meters`
- `POST /api/v1/admin/bills/generate/property`
- `POST /api/v1/admin/bills/generate/water`
- `GET /api/v1/admin/audit-logs`

## 页面范围

- 登录页
- 仪表盘 / 月报摘要
- 账单列表 + 详情抽屉
- 费用规则列表 + 新增表单
- 水表配置 + 抄表记录 / 新增抄表
- 物业费 / 水费开单页
- 审计日志列表 + 明细抽屉

补充说明：
- 物业费规则支持“月 / 年”两种周期录入。
- 当物业费规则选择“年”时，单价按“元/㎡/年”录入，后台开单仍按月生成账单，并自动折算为当月金额。

## 本地运行

```bash
npm install
npm run dev
```

默认开发地址：`http://127.0.0.1:5173`

Vite 已代理 `/api` 到 `http://127.0.0.1:8080`，请先启动后端。

## 默认账号

- 用户名：`admin`
- 密码：`123456`

## 构建

```bash
npm run build
```

## 说明

- 请求层统一处理后端 `{ code, message, data }` 响应结构。
- 登录态使用 Pinia + LocalStorage 持久化。
- 路由守卫会在进入业务页前校验 token，并按需拉取 `/api/v1/me/profile`。
- 页面均补了加载态、空态和错误态。
- 当前后端仅确认存在 `POST /api/v1/admin/water-meters`，因此水表能力在页面内以“配置/更新”表单呈现，未额外虚构水表列表接口。
