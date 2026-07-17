# 测试报告

## 执行信息

- 日期：2026-07-17
- Java：17.0.8
- 命令：`./mvnw clean verify`
- 测试套件：16
- 测试：28
- 失败：0
- 错误：0
- 跳过：0

## 重点结果

- Flyway V1—V10 从空数据库迁移成功。
- RSS、Atom、HTML、去重、主题、质量、阅读管理和运维回归通过。
- RSS 文章详情页正文提取、安全清洗和已有空正文文章回填通过。
- Windows Surefire XML 中中文超时提示保持正常，不再出现重复解码乱码。
- 2.5 秒慢响应在来源 1 秒超时后转为 `HTTP-REQUEST-TIMEOUT`。
- 超时任务结束后，同一采集员可以立即创建下一任务。
- 20 分钟无心跳任务被回收为 `TASK-TIMEOUT`，来源锁正常释放。
- 失败重试、待执行取消和调度触发通过。
- Windows 系统根证书库和附加 PEM CA 加载通过，未关闭证书校验。
- 备份 ZIP 成功创建；H2 在线备份被恢复到新目录，恢复库可查询文章表。
- WireMock 模拟 Ollama 状态与结构化生成接口，AI 摘要、要点、关键词、标签、分类、评分和持久化通过。
- AI Provider 状态接口、文章 AI 接口、文章详情页和运维状态面板通过。
- AI 多轮会话、历史持久化和回复保存资料库通过。
- AI 回复以 `AI_GENERATED`、`PENDING_REVIEW` 入库并显示 AI 内容标记。
- `MANUAL_URL` Provider 注册及单页正文提取通过。
- 文章 AI 分析结果在 1280px 和 900px 视口下均无横向溢出。
- CPU 与 NVIDIA GPU 两套 Docker Compose 配置解析通过。
- Android 离线工程确认不声明网络权限、使用 SQLite，内置 JSON 数据可解析。

## 发布包验证

- JAR：`knowledge-collector-boot/target/knowledge-collector.jar`
- 大小：64,998,931 字节
- 健康状态：UP
- Flyway：10
- 首页：HTTP 200
- 运维页：HTTP 200
- OpenAPI：HTTP 200
- AI Provider：1 个，本机 Ollama 可用
- Ollama 实机聊天：`deepseek-r1:14b` 成功，单次约 31.9 秒
- Compose CPU/GPU 配置：有效
- Android APK：本机未配置 Android SDK，未执行实际 APK 编译

## 结论

第一版本核心闭环及 Stage 10—13 通过自动化及发布包验收。Ollama AI、持久化聊天、AI 材料审核入库、可替换 Provider 接口、容器部署配置和移动端离线工程已具备；实际 Docker 镜像构建需先启动 Docker 服务，实际 APK 构建需安装 Android SDK。
