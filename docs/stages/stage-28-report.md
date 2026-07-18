# Stage 28：Firecrawl 与 Playwright 网页提取

## 结果

新增统一 `ContentExtractionProvider`，实现直接 HTTP、Firecrawl `/v1/scrape` 和独立 Playwright Chromium 渲染服务。文章详情、采集源列表、手动 URL 工作台和失败任务项均有业务入口。

抓取记录持久化实际 Provider、标题、作者、发布时间、正文长度、耗时、状态、错误、原始 HTML、截图和重试关系。成功且指定文章 ID 时立即更新文章正文、标题和作者；失败记录可更换方式重新执行。

## 验证

`Stage28To29ExtractionEvidenceIntegrationTest` 使用 WireMock 验证 Firecrawl 与 Playwright 确实收到请求，检查正文回写、截图、重试链和页面入口。V14 从空数据库迁移通过。
