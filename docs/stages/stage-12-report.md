# Stage 12 阶段报告

## 1. 阶段目标

接入本地 Ollama 内容理解，保持其他 AI 厂商可替换接口；同步升级前端，并提供 Docker Compose 与 Android 离线打包工具。

## 2. 本阶段范围

Ollama 结构化分析、Provider 状态、AI 结果持久化、文章与运维页面、Flyway V9、Docker 镜像/Compose、Android SQLite 离线阅读包和打包脚本。

## 3. 已完成事项

- 通用 `ContentIntelligenceProvider` 增加 Provider 标识和状态能力。
- Ollama 使用非流式 `/api/generate` 和 JSON Schema 结构化输出。
- 生成一句话摘要、核心观点、关键词、标签、分类和阅读价值。
- AI 结果、模型、Token 和耗时写入数据库。
- 文章详情新增 AI 阅读助手，运维页新增 Provider 状态面板。
- 新增 Docker 多阶段镜像、CPU Compose、NVIDIA GPU 覆盖和模型初始化服务。
- 新增 Android 离线工程，使用内置 SQLite 且不申请网络权限。
- 新增桌面资料导出和 APK 构建脚本。

## 4. 未完成事项

OpenAI、DeepSeek 等只保留 Provider 接口，未配置真实厂商实现。移动端暂不支持同步、采集、AI 推理或编辑阅读状态。

## 5. 新增文件

Ollama Provider、AI 应用服务/网关/Controller、V9、AI 集成测试、Docker/Compose、Android 工程、打包脚本和部署文档。

## 6. 修改文件

能力接口、配置、文章和运维页面、前端脚本与样式、测试迁移计数、README 和设计文档。

## 7. 核心设计说明

业务层按 Provider ID 选择实现，不引用 Ollama 类。模型返回值必须符合 JSON Schema 后才能持久化。移动端与服务端完全分离，APK 只导入清洗数据快照。

## 8. 数据库变更

Flyway V9 新增 `article_ai_analysis`，按文章一对一保存最近一次 AI 分析。

## 9. 接口变更

- `GET /api/v1/ai/providers`
- `GET /api/v1/articles/{id}/ai`
- `POST /api/v1/articles/{id}/ai/analyze`

`provider` 查询参数可选；省略时读取 `KNOWLEDGE_COLLECTOR_AI_PROVIDER`，当前默认值为 `ollama`。

## 10. 页面变更

文章阅读页新增 AI 阅读助手；运维页新增 AI Provider 可用性、模型和端点展示。

## 11. 测试情况

WireMock 模拟 Ollama `/api/tags` 与 `/api/generate`，覆盖状态、结构化分析、数据库持久化和页面渲染。全量共 14 个测试套件、26 个测试，失败 0、错误 0、跳过 0。

## 12. 构建与运行结果

- JDK 17 下 `mvnw clean verify` 成功。
- 发布 JAR 为 64,963,542 字节。
- JAR 启动后健康、首页、运维页和 OpenAPI 均通过，AI Provider 状态接口返回 1 个可用 Ollama Provider。
- CPU 与 NVIDIA GPU Compose 配置均通过 `docker compose config`。
- 本机 Docker 服务未启动，因此未执行镜像构建。
- 移动工程不声明 `INTERNET` 权限、使用 SQLite，内置 JSON 数据有效。
- 本机未安装 Android SDK，因此未执行实际 APK 编译。

## 13. Git 提交与标签

提交：`stage-12: 接入Ollama并提供容器与离线移动打包`；标签：`stage-12`。

## 14. 已知问题

Ollama 模型需要预先下载，CPU 推理可能较慢。Android APK 实际构建依赖本机 Android SDK/Gradle；Docker 镜像构建依赖已启动的 Docker 服务。

## 15. 风险与技术债务

长文章目前按字符数截断，后续可改为分块摘要。不同模型对结构化输出的稳定性不同，需要保持低温度和结果校验。

## 16. 验收结果

通过。AI 集成、前端、数据库迁移、发布 JAR、Compose 配置和移动端离线约束均已验收；Docker 镜像与 APK 的实际产物受本机运行环境限制，已提供可执行打包工具和文档。

## 17. 下一阶段计划

增加分块分析、批量 AI 队列、Provider 密钥安全配置，以及移动端增量导入和本地搜索。
