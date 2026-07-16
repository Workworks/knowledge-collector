# Stage 9 阶段报告

## 阶段目标

完成本地固定周期调度、失败任务重试、待执行任务取消、运行统计仪表盘和本地数据备份，并将首页“基础设施状态”升级为可直接进入运维操作的状态控制台。

## 已完成

- Flyway V7 新增采集调度、备份记录、任务触发方式、重试来源和取消标记。
- 每 30 秒扫描启用且到期的采集员调度，支持 1 分钟至 7 天固定周期。
- 任务区分 `MANUAL_SOURCE`、`SCHEDULED` 和 `RETRY` 触发方式。
- 失败任务可创建关联原任务的新重试任务；尚未开始的任务可取消。
- 新增文章、未读、收藏、待审核、采集员、任务及失败任务统计。
- 新增最近任务、最近文章、主题文章数和采集员健康度数据。
- 新增在线 H2 数据库备份，并打包正文、快照、导出目录与清单。
- 新增 `/operations` 调度与运维页面。
- 首页“基础设施状态”重新包装为运行控制台，展示数据库、Flyway、数据目录、版本、启动状态和快捷运维入口。
- 任务列表展示触发方式，失败任务详情提供重试按钮。
- 新增 `http/stage-9-operations.http`，使用完整 URL，避免 IDEA HTTP Client 环境变量未替换问题。

## 主要接口

- `GET /api/v1/dashboard`
- `GET /api/v1/operations/schedules`
- `PUT /api/v1/operations/schedules/{sourceId}`
- `POST /api/v1/operations/schedules/run-due`
- `GET /api/v1/operations/backups`
- `POST /api/v1/operations/backups`
- `POST /api/v1/tasks/{id}/retry`
- `POST /api/v1/tasks/{id}/cancel`

## 验证

- 阶段集成测试覆盖调度保存、到期执行、任务取消、仪表盘、页面渲染和真实文件数据库备份。
- 基础集成测试确认 Flyway 已执行 7 个迁移版本。
- 全量 Maven 测试与可执行 JAR 冒烟测试作为阶段提交门禁。

## 已知边界

- 当前调度为单机固定周期，不支持 Cron 表达式和分布式锁。
- 采集任务仍同步执行；取消只接受尚未开始的 `CREATED` 任务。
- 自动指数退避将在后续阶段增强。
- 恢复属于高风险停机操作，Stage 9 只提供备份生成和恢复指南，不暴露在线恢复接口。

## 下一阶段

Stage 10 进入版本收口：恢复演练、性能与安全回归、发布包检查、完整用户手册和验收报告。
