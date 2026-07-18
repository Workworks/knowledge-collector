# Stage 26：第三方能力接入框架

## 目标与结果

建立可从页面完成配置、连接测试、启停、默认/备用选择、健康查看、调用日志和失败重试的统一能力中心。Ollama 已实现 `ManagedCapabilityProvider`，文章分析与 AI 对话通过统一管理服务记录调用，不依赖具体厂商类。

## 页面和接口

- 页面：`/capabilities`
- 配置：`GET /api/v1/capabilities`、`PUT /api/v1/capabilities/{providerId}`
- 检测：`POST /api/v1/capabilities/{providerId}/test`
- 日志与重试：`GET /api/v1/capabilities/calls`、`POST /api/v1/capabilities/calls/{id}/retry`

## 数据与验证

Flyway V13 新增 `third_party_service` 和 `third_party_call_log`。WireMock 验证 Ollama 模型列表、健康检测、动态配置和日志持久化；浏览器验收服务卡片、模型展示和失败原因。

## 边界

认证信息只在本机数据库保存且 API 不回显明文。当前 Ollama 不需要认证；云端 Provider 将在后续阶段接入。
