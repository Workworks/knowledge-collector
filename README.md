# Knowledge Collector

Knowledge Collector 是面向个人和小团队的本地资料采集与阅读管理系统。当前完成至 Stage 13：支持本地 Ollama 内容理解、持久化 AI 对话、容器部署和 Android 离线资料包。

## 当前可用能力

- 中文基础首页：`http://127.0.0.1:8080/`
- 页面测试人员使用的 Web 接口测试台：`/test-console`
- 系统状态 API：`/api/v1/system/status`
- Actuator 健康检查：`/actuator/health`
- IDEA HTTP Client 请求集：`http/knowledge-collector.http`
- Stage 4 管理接口请求集：`http/stage-4-management.http`
- 主题管理页面：`/topics`
- 采集源管理页面：`/sources`
- HTML 规则工作台：`/sources/{id}/rules`
- 采集任务与文章库：`/tasks`、`/articles`
- 待审核文章页面：`/articles/review`
- 文章质量评估 API：`/api/v1/articles/{articleId}/assessment`
- 资料库支持全文搜索、主题/来源/标签/状态/质量组合筛选
- AI 研究助手页面：`/ai-chat`，支持多轮会话及历史记录
- AI 回复可保存到待审核资料库，并明确标注“AI 内容”
- 阅读页支持收藏、已读/未读、归档、忽略、自定义标签和个人笔记
- Stage 8 完整 IDEA 请求任务：`http/stage-8-end-to-end.http`
- 调度与运维页面：`/operations`
- Stage 9 IDEA 请求集：`http/stage-9-operations.http`
- 固定周期调度、失败任务重试、待执行任务取消、运行仪表盘和本地备份
- OpenAPI JSON 与 Swagger UI：`/v3/api-docs`、`/swagger-ui.html`
- local Profile 测试工具：`/dev/tools`
- H2 文件数据库与 Flyway V1—V10 迁移
- 任务总请求超时、心跳租约与超时任务自动回收
- JDK、Windows 系统根证书和可选 PEM CA 的组合 TLS 信任
- RSS/Atom 支持内嵌全文和文章详情页正文提取、安全清洗及空正文回填
- MANUAL_URL 支持直接采集单篇网页并提取正文
- 本地 Ollama 文章摘要、核心观点、关键词、标签、分类和阅读价值
- Docker Compose 一键部署与 Android SQLite 离线阅读包
- 启动次数持久化，用于验证重启后数据不丢失
- 数据、正文、快照、导出和日志目录自动创建
- 全局异常响应、字段错误模型和请求关联编号
- Windows/Linux 启动脚本

## 环境要求

- JDK 17 或更高版本
- Maven 3.6.3 或更高版本
- Git

当前工程使用 Spring Boot 3.5.16。系统启动不依赖外部网络；第一次 Maven 构建可能需要下载依赖。

## 构建

```bash
./mvnw clean verify
```

可执行文件：

```text
knowledge-collector-boot/target/knowledge-collector.jar
```

## 启动

Windows：

```bat
set JAVA_HOME=C:\path\to\jdk-17
start.bat
```

Linux/macOS：

```bash
export JAVA_HOME=/path/to/jdk-17
chmod +x start.sh
./start.sh
```

也可直接执行：

```bash
java -jar knowledge-collector-boot/target/knowledge-collector.jar
```

按 `Ctrl+C` 正常停止。

local Profile（自动初始化演示主题与来源）：

```bash
./mvnw spring-boot:run -pl knowledge-collector-boot -am -Dspring-boot.run.profiles=local
```

## 配置

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `KNOWLEDGE_COLLECTOR_DATA_DIR` | `./data` | 数据根目录 |
| `KNOWLEDGE_COLLECTOR_SERVER_ADDRESS` | `127.0.0.1` | 监听地址 |
| `KNOWLEDGE_COLLECTOR_SERVER_PORT` | `8080` | HTTP 端口 |
| `KNOWLEDGE_COLLECTOR_TASK_STALE_TIMEOUT` | `PT10M` | 任务最大无心跳时间 |
| `KNOWLEDGE_COLLECTOR_TRUST_SYSTEM_STORE` | `true` | Windows 合并系统根证书 |
| `KNOWLEDGE_COLLECTOR_ADDITIONAL_CA_FILE` | 空 | 额外可信 PEM CA 文件 |
| `KNOWLEDGE_COLLECTOR_OLLAMA_ENABLED` | `true` | 启用 Ollama Provider |
| `KNOWLEDGE_COLLECTOR_OLLAMA_BASE_URL` | `http://127.0.0.1:11434` | Ollama 地址 |
| `KNOWLEDGE_COLLECTOR_OLLAMA_MODEL` | `qwen3:4b` | 默认模型 |
| `KNOWLEDGE_COLLECTOR_OLLAMA_TIMEOUT` | `PT2M` | 单次分析超时 |
| `KNOWLEDGE_COLLECTOR_AI_CHAT_MAX_HISTORY` | `20` | AI 对话携带的最近消息数 |

Profile 分工：

- `local`：演示主题/来源、`/dev/tools`、更多诊断详情。
- `test`：自动化测试、内存 H2、禁止真实网络配置。
- `production`：关闭测试工具与 OpenAPI，严格限制 Actuator。

第一版没有登录，默认只监听本机。请勿在未增加访问控制的情况下绑定公网地址。

## 数据目录

```text
data/
├─ database/        H2 数据库
├─ article-content/ 文章内容文件
├─ snapshots/       可选网页快照
├─ exports/         导出与备份产物
└─ logs/            应用日志
```

## 模块

- `knowledge-collector-domain`：领域模型与端口。
- `knowledge-collector-capability-api`：第三方数据源、AI、搜索、通知、存储和安全能力接口。
- `knowledge-collector-capability-provider`：RSS/Atom、Jsoup HTML、JDK HTTP 和 URL 安全默认实现。
- `knowledge-collector-application`：应用用例和查询接口。
- `knowledge-collector-infrastructure`：数据库、Flyway、存储等适配器。
- `knowledge-collector-web`：MVC、REST、模板和静态资源。
- `knowledge-collector-boot`：启动、配置和可执行 JAR。

## 文档

- 项目与假设：`docs/01-project`
- 需求：`docs/02-requirements`
- 架构与设计：`docs/03-design`
- 开发说明：`docs/04-development`
- 页面与接口测试：`docs/05-testing/manual-api-testing.md`
- 阶段报告：`docs/stages`

## 当前边界

当前支持 RSS、Atom、RSS 文章详情页通用正文抽取、基于 CSS Selector 的静态 HTML 采集、本地固定周期调度、H2 资料检索和 Ollama 内容分析。动态浏览器渲染、分布式调度、云端 AI Provider、专业搜索引擎及外部通知仍属于后续阶段。系统未加入任何绕过登录、验证码、付费墙或 robots.txt 的能力。
