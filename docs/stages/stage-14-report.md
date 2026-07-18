# Stage 14 阶段报告

## 完成范围

- AI 自动发现采集源：输入主题、语言、数量和质量等级，Ollama 给出公开来源候选。
- 候选来源在写库前执行类型、URL、重复和真实访问验证；仅通过者以 `VERIFIED` 状态入库。
- 采集员列表展示健康状态、检查说明和最近刷新时间，支持单条及全部刷新。
- 采集任务支持任务号、采集员、状态、触发方式和日期范围过滤；页面默认最近七天。
- 新增 `/articles/archive` 归档资料库，可查看所有归档文章。
- 归档整理规则支持名称、关键词、主题、来源、最低质量、顺序和启停配置。
- Flyway V11 增加采集源健康字段、任务查询索引和 `archive_rule` 表。

## 新增接口

- `POST /api/v1/sources/discover`
- `POST /api/v1/sources/{id}/health`
- `POST /api/v1/sources/health/refresh`
- `GET /api/v1/tasks` 新增过滤参数
- `GET/POST /api/v1/archive-rules`
- `PUT/DELETE /api/v1/archive-rules/{id}`

## 验证

- `mvn verify`：30 个测试，失败 0，错误 0。
- Flyway：11 个迁移从空库执行成功。
- 发布 JAR：65,033,704 字节。
- AI 自动识别并验证 RSS、Atom、HTML List、JSON API 和 Manual URL；HTML 自动生成通用规则。
- 900px 视口下采集源、任务和归档页面均无横向溢出。
- IDEA HTTP Client：`http/stage-14-management.http`。
