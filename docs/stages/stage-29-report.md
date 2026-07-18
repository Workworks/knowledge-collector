# Stage 29：MinIO 原始证据存储

## 结果

新增 MinIO `ObjectStorageProvider`，支持连接检查、对象保存、读取和删除。`evidence_file` 记录归属对象、文件类型、对象键、MIME、大小、SHA-256、版本、Provider 与创建时间。

“文件与快照”页面支持按文章、知识卡片、观点证据、草稿或通用对象上传补充材料，查看原始 HTML/截图/附件版本并下载。文章抓取成功后会在 MinIO 可用时自动沉淀原始 HTML 和 Playwright 截图。

## 部署与验证

Compose 包含 MinIO、存储桶初始化和 Playwright 服务。集成测试使用同一 Provider 的内存验收协议验证两次补充材料上传形成 v1/v2、抓取证据自动保存及下载字节一致；生产 HTTP 端点使用 MinIO Java SDK。
