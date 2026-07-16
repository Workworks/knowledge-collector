# Knowledge Collector

Knowledge Collector 是面向个人和小团队的本地资料采集与阅读管理系统。当前完成至 Stage 4：基础工程、主题管理、采集源管理、主题与来源关联、管理页面和 REST API 已经可运行。

## 当前可用能力

- 中文基础首页：`http://127.0.0.1:8080/`
- 页面测试人员使用的 Web 接口测试台：`/test-console`
- 系统状态 API：`/api/v1/system/status`
- Actuator 健康检查：`/actuator/health`
- IDEA HTTP Client 请求集：`http/knowledge-collector.http`
- Stage 4 管理接口请求集：`http/stage-4-management.http`
- 主题管理页面：`/topics`
- 采集源管理页面：`/sources`
- OpenAPI JSON 与 Swagger UI：`/v3/api-docs`、`/swagger-ui.html`
- local Profile 测试工具：`/dev/tools`
- H2 文件数据库与 Flyway V1 迁移
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

当前首页仍是基础状态入口。Stage 4 已实现主题和采集源配置；真实来源连通性测试、采集规则、采集任务、文章资料库和调度将在后续阶段实现。系统未加入任何绕过登录、验证码、付费墙或 robots.txt 的能力。
