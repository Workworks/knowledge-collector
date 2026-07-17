# Stage 13 阶段报告

## 阶段目标

将本机 Ollama 聊天能力集成到系统，支持持久化会话和把 AI 回复保存到待审核资料库；修复文章 AI 分析后的布局溢出及 `MANUAL_URL` Provider 缺失问题。

## 已完成事项

- 新增通用 `ConversationalIntelligenceProvider`，Ollama 使用 `/api/chat`，其他 AI 可按接口扩展。
- 新增 AI 研究助手页面、多轮会话、历史消息和模型运行信息。
- 每条 AI 回复提供“保存到资料库”，保存时可编辑标题。
- AI 材料以 `PENDING_REVIEW` 入库，并记录 `contentOrigin=AI_GENERATED`、原会话和消息。
- 资料库卡片和详情页明确展示“AI 内容/AI 生成内容”，不伪装成公开来源。
- Flyway V10 新增会话、消息、AI 来源及文章来源标记。
- 修复文章分析后侧栏在两列响应式布局中的最小宽度和长文本溢出。
- 新增 `ManualUrlSourceProvider`，单网页采集不再出现“未找到 MANUAL_URL 对应的能力 Provider”。

## 新增接口

- `GET /api/v1/ai/chat/conversations`
- `POST /api/v1/ai/chat/conversations`
- `GET /api/v1/ai/chat/conversations/{id}`
- `POST /api/v1/ai/chat/conversations/{id}/messages`
- `POST /api/v1/ai/chat/messages/{messageId}/save`

## 验收结果

- JDK 17 下 `mvnw clean verify` 成功。
- 16 个测试套件、28 个测试，失败 0、错误 0、跳过 0。
- Flyway V1—V10 从空数据库迁移成功。
- 发布 JAR：64,998,931 字节。
- 本机 `deepseek-r1:14b` 实际聊天成功，耗时约 31.9 秒。
- AI 回复保存后为 `AI_GENERATED / PENDING_REVIEW / AI 生成内容`。
- 文章页面在 1280px 与 900px 视口均无横向溢出。

## 已知限制

当前为整轮非流式回答；后续可增加流式输出、会话重命名/删除、引用资料库上下文和人工审核通过/驳回操作。
