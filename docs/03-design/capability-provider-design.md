# 可插拔能力 Provider 设计

## 模块边界

`knowledge-collector-capability-api` 只包含稳定接口与请求/响应模型，不依赖 Spring、数据库或具体厂商 SDK。

`knowledge-collector-capability-provider` 包含默认本地实现：

- `RomeFeedSourceProvider`：RSS/Atom；
- `JsoupHtmlSourceProvider`：静态 HTML 列表与详情；
- `JdkWebContentProvider`：受限 HTTP 客户端；
- `DefaultUrlSecurityValidator`：协议、DNS 和私有地址校验。

应用层只依赖 `ContentSourceProvider` 等能力接口，通过 Spring 注入可用实现。后续 arXiv、Crossref、PubMed、GDELT、Ollama、OpenAI、Webhook、Lucene 或 S3 实现应放在 Provider 模块或拆分后的独立 Provider 子模块中。

## 已预留接口

- `ContentSourceProvider`
- `WebContentProvider`
- `BrowserRenderingProvider`
- `ContentIntelligenceProvider`
- `EmbeddingProvider`
- `TranslationProvider`
- `ContentClusteringProvider`
- `SearchProvider`
- `NotificationProvider`
- `ObjectStorageProvider`
- `UrlSecurityValidator`

## 依赖规则

```text
application -> capability-api
capability-provider -> capability-api
infrastructure -> application + capability-provider
boot -> infrastructure + web
```

业务代码不得直接依赖具体第三方 SDK。Provider 选择通过 `supports(sourceType)` 完成；外部凭证后续通过环境变量或密钥管理注入，不进入业务模型和日志。
