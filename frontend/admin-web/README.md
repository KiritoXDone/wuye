# admin-web

物业管理系统 Web 管理端，当前上线基线为 `React + Tailwind CSS + TypeScript`。

## 当前接通的核心接口

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
- `POST /api/v1/admin/bills/generate/property-yearly`
- `POST /api/v1/admin/bills/generate/water`
- `GET /api/v1/admin/audit-logs`

## 本地运行

```bash
npm install
npm run dev
```

默认开发地址：`http://127.0.0.1:5173`

## 登录说明

- 默认管理员用户名：`admin`
- 管理员初始化密码：由后端启动时的 `APP_BOOTSTRAP_ADMIN_PASSWORD` 注入
- 前端页面不再内置或展示固定弱口令

## 构建

```bash
npm run build
```
