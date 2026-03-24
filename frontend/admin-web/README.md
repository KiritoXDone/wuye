# admin-web

物业管理系统 Web 管理端，当前基线为 `React + Tailwind CSS + TypeScript`。

## 当前已接通的核心页面

- `/dashboard`
- `/household-payments`
- `/billing-generate`
- `/water-readings`
- `/bills`
- `/dunning`
- `/fee-rules`
- `/invoice-applications`

## 当前已接通的核心接口

- `POST /api/v1/admin/auth/login/password`
- `GET /api/v1/admin/dashboard/summary`
- `GET /api/v1/admin/reports/monthly`
- `GET /api/v1/admin/bills`
- `GET /api/v1/admin/bills/{billId}`
- `GET /api/v1/admin/billing/households`
- `POST /api/v1/admin/bills/{billId}/mark-paid`
- `GET /api/v1/admin/fee-rules`
- `POST /api/v1/admin/fee-rules`
- `GET /api/v1/admin/water-readings`
- `POST /api/v1/admin/water-readings`
- `POST /api/v1/admin/water-meters`
- `POST /api/v1/admin/bills/generate/property-yearly`
- `POST /api/v1/admin/bills/generate/water`
- `GET /api/v1/admin/audit-logs`

## 缴费统计页说明

`/household-payments` 支持：

- 按房间查看指定年度物业费与指定月份水费状态
- 按小区 / 楼栋 / 单元 / 房号筛选
- 对未缴账单执行“手动标记已缴”
- 录入缴费时间与备注
- 将线下缴费确认写入后端审计日志

## 本地运行

```bash
npm install
npm run dev
```

默认开发地址：`http://127.0.0.1:5173`

## 构建

```bash
npm run build
```
