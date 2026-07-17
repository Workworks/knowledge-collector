# API 设计

## 1. 约定

- 基础路径：`/api/v1`
- 内容类型：`application/json`
- 时间：ISO-8601，包含偏移量，例如 `2026-07-16T21:00:00+08:00`
- ID：JSON number；业务编码为 string
- 分页：`page` 从 0 开始，`size` 默认 20、最大 100
- 排序：`sort=field,direction`，字段使用白名单

## 2. 响应结构

成功：

```json
{
  "success": true,
  "data": {},
  "timestamp": "2026-07-16T21:00:00+08:00",
  "correlationId": "..."
}
```

失败：

```json
{
  "success": false,
  "error": {
    "code": "VAL-001",
    "message": "请求参数不符合要求",
    "fieldErrors": [{"field": "sourceCode", "message": "不能为空"}]
  },
  "timestamp": "2026-07-16T21:00:00+08:00",
  "correlationId": "..."
}
```

分页 `data` 包含 `content, page, size, totalElements, totalPages, sort`。

## 3. HTTP 状态

- 200 查询/修改成功；201 创建成功；204 无正文成功。
- 400 校验或非法状态；404 资源不存在；409 冲突/重复；422 可解析但业务规则不满足。
- 429 本地任务触发频率受限；500 未分类内部错误；502/504 外部来源代理型失败仅在同步测试接口使用。

## 4. 校验与幂等

- 编码：1—64，建议 `[A-Z0-9_-]+`。
- URL 只允许 HTTP/HTTPS；超时、重试、间隔有系统上下限。
- 创建接口可接受 `Idempotency-Key`（后续实现）；状态切换接口以目标状态而非 toggle 为主。
- 任务重试每次创建新任务；重复请求通过原任务状态和幂等键避免多建。

## 5. 接口清单

### 5.1 主题

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/topics` | 分页查询 |
| POST | `/topics` | 创建 |
| GET | `/topics/{id}` | 详情 |
| PUT | `/topics/{id}` | 修改 |
| PATCH | `/topics/{id}/enabled` | 设置启用状态 |
| DELETE | `/topics/{id}` | 受控删除 |

### 5.2 来源

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET/POST | `/sources` | 查询/创建 |
| GET/PUT | `/sources/{id}` | 详情/修改 |
| PATCH | `/sources/{id}/enabled` | 启停 |
| POST | `/sources/{id}/test` | 测试连通与基础解析 |
| POST | `/sources/{id}/crawl` | 创建手动采集任务 |

### 5.3 规则

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET/POST | `/sources/{sourceId}/rules` | 版本列表/创建新版本 |
| GET | `/sources/{sourceId}/rules/active` | 当前启用版本 |
| POST | `/sources/{sourceId}/rules/test` | 使用当前规则测试，不入库 |
| PUT | `/sources/{sourceId}/rules/{ruleId}/active` | 激活历史版本 |

### 5.4 任务

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/tasks` | 查询任务 |
| GET | `/tasks/{id}` | 任务详情 |
| GET | `/tasks/{id}/items` | 任务项分页 |
| POST | `/tasks/{id}/cancel` | 请求取消 |
| POST | `/tasks/{id}/retry` | 创建重试任务 |

### 5.5 文章与阅读

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| GET | `/articles` | 搜索、筛选、分页 |
| GET | `/articles/{id}` | 详情与安全正文 |
| GET | `/ai/providers` | AI Provider 状态 |
| GET | `/articles/{id}/ai` | 最近一次 AI 分析 |
| POST | `/articles/{id}/ai/analyze` | 使用指定 Provider 分析文章 |
| PUT | `/articles/{id}/reading-status` | 设置阅读状态 |
| PUT | `/articles/{id}/favorite` | 设置收藏状态 |
| PUT | `/articles/{id}/topics` | 人工调整主题 |
| GET/POST | `/articles/{id}/notes` | 查询/新增笔记 |
| PUT/DELETE | `/notes/{id}` | 修改/删除笔记 |
| PUT | `/articles/{id}/tags` | 替换标签集合 |

### 5.6 标签

`GET/POST /tags`、`PUT/DELETE /tags/{id}`。

### 5.7 仪表盘与运维

- `GET /dashboard`
- `GET /operations/schedules`
- `PUT /operations/schedules/{sourceId}`
- `POST /operations/schedules/run-due`
- `POST /operations/tasks/recover-stale`
- `POST /operations/backups`
- `GET /operations/backups`

恢复操作具有高风险，第一版优先通过停机脚本执行，不默认暴露在线 REST 恢复接口。

## 6. 请求示例

创建来源：

```json
{
  "sourceCode": "SPRING_BLOG",
  "sourceName": "Spring Blog",
  "sourceType": "RSS",
  "homeUrl": "https://spring.io/blog",
  "feedUrl": "https://spring.io/blog.atom",
  "timeoutSeconds": 15,
  "maxRetries": 2,
  "requestIntervalMillis": 2000,
  "obeyRobots": true,
  "fetchFullContent": true,
  "summaryOnly": false,
  "saveSnapshot": false,
  "enabled": true
}
```

文章查询：

```text
GET /api/v1/articles?keyword=spring&topicId=1&reviewStatus=AUTO_ACCEPTED&minQuality=60&page=0&size=20
```

文章质量评估：

```text
GET  /api/v1/articles/{articleId}/assessment
POST /api/v1/articles/{articleId}/assessment
```

评估响应包含 `qualityScore`、`reviewStatus`、`sourceLevel`、`evidenceCount`、
`hasDoi`、`contentFingerprint`、`warnings` 和多主题匹配结果。采集入库后自动执行首次评估；
主题规则变化后可通过 POST 接口重新评估。排除词命中时优先阻止自动接收。

阅读管理：

```text
GET   /api/v1/articles/{articleId}/reading
PATCH /api/v1/articles/{articleId}/reading/state
PUT   /api/v1/articles/{articleId}/reading/tags
PUT   /api/v1/articles/{articleId}/reading/note
GET   /api/v1/tags
```

组合查询支持 `keyword`、`sourceId`、`topicId`、`reviewStatus`、`readingStatus`、
`favorite`、`archived`、`tagId`、`minQuality` 和受控 `sort`。普通资料库默认传入
`archived=false`，归档资料需要显式筛选。

AI 研究助手：

```text
GET  /api/v1/ai/chat/conversations
POST /api/v1/ai/chat/conversations
GET  /api/v1/ai/chat/conversations/{conversationId}
POST /api/v1/ai/chat/conversations/{conversationId}/messages
POST /api/v1/ai/chat/messages/{messageId}/save
```

发送消息时携带最近若干轮历史；保存接口仅接受 AI 助手消息，可编辑材料标题。保存结果进入
`PENDING_REVIEW`，并记录 `AI_GENERATED`、原会话和原消息，供资料库审核及追溯。

## 7. 页面 Controller

页面路由不放在 `/api/v1` 下。POST 表单使用 PRG（Post/Redirect/Get），校验失败返回原表单和字段错误；异步局部操作调用 REST API。页面与 API 复用应用服务，但分别维护 ViewModel 与 API DTO。

## 8. 安全预留

- 后续加入 Spring Security 后，页面使用会话/CSRF，API 可使用同源会话或 Token。
- 当前无登录版本默认只监听本机。
- 对状态变更保留 CSRF 接入点，不使用 GET 执行写操作。
- 日志与错误响应不返回内部路径、堆栈和敏感请求头。

## 9. 版本与兼容

第一版固定 `/api/v1`。新增可选字段保持兼容；删除/改名字段需要新版本或弃用周期。API 文档与 Controller 测试在实现阶段同步维护。
