# 物业管理系统

当前仓库以上线口径为准，核心事实来源依次为：

1. `AGENTS.md`
2. `docs/物业管理系统_生产实施与上线交付文档_v1.1.docx`
3. `docs/00_上线基线说明.md`
4. 当前代码实现

## 当前冻结口径

- 账单主体是房间，不是账号
- 物业费按面积计费、按年开单、按年缴纳
- 水费按月抄表，录入即出账
- 账单按费用类型分单
- 报表与统计默认按房间口径
- 管理端基线为 `React + Tailwind CSS + TypeScript`

## 当前已打通的后端闭环

- Spring Boot 3 + MyBatis + Flyway 基础架构
- 统一响应、统一异常、JWT 鉴权
- Resident 微信登录开发桩、Admin 密码登录
- 房间绑定 / 我的房间
- 年度物业费规则与年度开单
- 水表配置、抄表录入、录入即出账
- Resident 账单列表 / 账单详情 / 支付闭环
- 报表、导入导出、催缴、发票、券基础能力
- 管理端“缴费统计”页：按户查看年度物业费与月度水费状态
- 后台手动标记账单已缴：适配线下转账、现金收款、历史系统已缴场景，并写审计日志

## 目录结构

```text
backend/                 后端工程
frontend/admin-web/      Web 管理端
frontend/miniapp-wechat/ 微信小程序端
docs/                    上线基线与专项说明
ops/                     运维与本地脚本
```

## 本地运行

### 1. 启动后端

```powershell
"D:\Application\IntelliJ IDEA 2025.3\plugins\maven\lib\maven3\bin\mvn.cmd" -f backend/pom.xml spring-boot:run
```

本地 `dev` 环境默认使用：
- MySQL `127.0.0.1:3306/wuye_system`
- 用户名 `root`
- 密码 `password`
- 管理员登录账号 `admin / 123456`
- 小程序开发登录 code `resident-zhangsan`、`resident-lisi`

说明：本地默认启用 mock 微信登录，方便微信开发者工具直接联调；如需切回真实微信登录，可显式设置 `APP_AUTH_WECHAT_MODE=real` 并补齐微信配置。

### 2. 运行后端测试

```powershell
"D:\Application\IntelliJ IDEA 2025.3\plugins\maven\lib\maven3\bin\mvn.cmd" -f backend/pom.xml test
```

### 3. 启动管理端

```bash
cd frontend/admin-web
npm install
npm run dev
```

## 关键页面与接口

### 管理端页面

- `/dashboard`：运营总览
- `/household-payments`：缴费统计，按户查看物业费 / 水费状态并支持手动标记已缴
- `/billing-generate`：年度物业费开单 / 水费补齐出账
- `/water-readings`：抄表录入
- `/bills`：账单管理

### 新增关键接口

- `GET /api/v1/admin/billing/households`
- `POST /api/v1/admin/bills/{billId}/mark-paid`

## 文档入口

- [docs/00_上线基线说明.md](/D:/Code/JAVA/wuye/docs/00_上线基线说明.md)
- [docs/README.md](/D:/Code/JAVA/wuye/docs/README.md)
- [frontend/admin-web/README.md](/D:/Code/JAVA/wuye/frontend/admin-web/README.md)
- `docs/物业管理系统_生产实施与上线交付文档_v1.1.docx`
