# Stage 5 阶段报告

## 阶段目标

完成 RSS/Atom 采集闭环：安全访问 Feed、解析条目、创建手动采集任务、记录任务结果、规范化 URL、基础去重并保存文章元数据。

## 已完成

- Flyway V3 新增 `crawl_task`、`crawl_task_item`、`article`，任务号和文章 URL 哈希唯一。
- 数据库唯一约束保证同一采集源不能同时存在多个活动任务。
- RSS Provider 与 Atom Provider 使用 Rome 解析 Feed。
- HTTP 客户端支持超时、User-Agent、Accept-Language、最多 5 次重定向、每次重定向重新校验地址、5 MB 响应上限和 XML/Feed Content-Type 校验。
- URL 安全校验默认阻止回环、链路本地和私有网络地址；测试环境可显式放行 WireMock。
- URL 规范化处理相对地址、大小写、默认端口、重复斜杠、片段、跟踪参数和查询参数顺序，并以 SHA-256 去重。
- 手动采集接口：`POST /api/v1/sources/{id}/crawl`。
- 来源测试接口：`POST /api/v1/sources/{id}/test`，只请求和解析，不写入文章。
- 任务查询接口与页面：`/api/v1/tasks`、`/tasks`、`/tasks/{id}`。
- 文章查询接口与页面：`/api/v1/articles`、`/articles`、`/articles/{id}`。
- IDEA HTTP Client 请求集已补齐，全部使用完整 URL，不依赖未替换变量。

## 测试

- WireMock 提供固定 RSS 响应，不访问真实互联网。
- 覆盖来源测试、首次采集、任务成功、文章入库、二次采集去重、任务查询、文章查询和页面渲染。
- 覆盖 URL 规范化与哈希稳定性。
- 测试 Profile 默认禁用网络，只有 Stage 5 WireMock 测试显式开启并放行回环地址。

## 已知边界

- 本阶段只采集 Feed 元数据与摘要，不抓取文章 HTML 正文。
- 调度、重试队列和失败任务重跑属于后续阶段。
- 文章主题匹配、阅读状态与全文检索属于后续阶段。
- Local 演示源仍需由测试人员配置可访问的 RSS/Atom 地址后执行真实采集。

## 下一阶段

进入 Stage 6：采集规则、选择器版本、HTML 列表与详情页采集，以及对应的规则测试工作台。
