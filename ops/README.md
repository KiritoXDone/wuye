# ops

本目录用于收口上线交付链路，目标包括：

- Nginx 托管管理端静态产物
- 前端静态包版本切换与快速回滚
- 前后端发布顺序说明
- 数据库迁移与账务清理脚本说明

当前状态：

- 后端仍可依赖本地 MySQL 启动，应用启动后会自动执行 Flyway migration
- 管理端已切换到 React + Tailwind 构建入口，需通过 Nginx 托管 `frontend/admin-web/dist`
- 当前已补齐基础 `nginx/` 与 `scripts/` 样例，用于静态包发布与快速回滚基线

建议发布顺序：

1. 执行数据库 migration 与账务清理脚本
2. 发布后端应用
3. 构建并发布管理端静态包
4. 验证 Nginx 切换与回滚链路
