# Changelog

## Operations and Docker proxy

- 验证 Docker Desktop 通过 Clash `127.0.0.1:7890` 连接 Docker Hub，并补充 Ollama 容器模型部署流程。
- 新增第三方账号、默认凭据、配置位置、轮换和重置手册。
- 扩展运维手册：Docker Desktop、Compose、PowerShell、Grafana、Prometheus、IDEA HTTP Client 及分层日志排错。
- 移除 n8n 2.x 已不适用的 Basic Auth 环境变量，明确首次创建 Owner 账号。

## Stage 30-38

- Apache Tika/OCR 文档导入并创建资料库文章。
- Qdrant 向量索引、关键词/语义/混合搜索与重排调试。
- Crossref 学术搜索、DOI 元数据导入。
- OpenAI 兼容云模型的内容预览、确认调用和安全审计。
- AI 调用记录、Prometheus/Grafana 监控、ntfy 通知与 n8n Webhook 工作流。
- Windows Docker Desktop 全栈 Compose、宿主机 Ollama 与缓存运行时绕行方案。

本项目采用阶段标签记录可运行里程碑。正式版本发布后将遵循 [Semantic Versioning](https://semver.org/)。

## [Unreleased]

- Stage 16–25：文章结构化分析、知识卡片、观点证据、实体概念和卡片关系。
- 相似事件聚合、专题知识页、研究项目、带来源综合归纳和写作草稿。
- 知识缺口、间隔复习、知识资产统计与统一知识研究工作台。
- V12 数据库迁移、REST API、IDEA HTTP 示例和端到端集成测试。
- 统一所有页面的固定顶部导航，修复跨页面切换时菜单增减、改名和换序问题；Stage 29 后为十二项。
- 文章阅读操作与 AI 分析提示改为悬浮通知，修复正文被提示块挤出阅读区的问题。
- Stage 26：统一第三方能力配置、健康检测、默认/备用 Provider、模型列表、调用日志和失败重试。
- Stage 27：SearXNG 搜索、候选持久化、单个/批量验证、导入与忽略闭环。
- Stage 28：Firecrawl/Playwright 可选抓取、正文回写、原始 HTML、截图、失败信息和重试链。
- Stage 29：MinIO 原始证据、补充材料上传下载、归属查询与自动文件版本。
- V14 数据迁移、Playwright 容器服务、MinIO Compose 存储桶初始化和 Stage 28–29 端到端测试。

## [1.0.0] - 2026-07-18

### Added

- 主题、公开采集源、CSS 规则、定时调度和采集任务管理。
- RSS、Atom、HTML List、JSON API、Manual URL Provider 与安全正文清洗。
- URL 去重、主题匹配、质量评估、审核、全文搜索和阅读管理。
- 采集源健康检查、AI 自动发现来源、任务过滤和规则化归档。
- Ollama 文章分析、多轮聊天和 AI 材料待审核入库。
- H2/Flyway、本地备份恢复、Docker Compose 和 Android 离线资料包。

### Security

- 默认仅监听回环地址。
- URL 安全校验、响应大小/超时限制、TLS 证书验证和安全 HTML 清洗。
- 不提供绕过登录、验证码、robots.txt、付费墙或证书验证的能力。

各阶段明细见[阶段报告索引](docs/stages/README.md)。
