# Stage 27：SearXNG 搜索与采集源发现

## 目标与结果

采集员页面形成“输入主题 → SearXNG 搜索 → 查看候选 → 单个/批量验证 → 单个/批量导入”的完整闭环。候选在导入前持久化，保留可靠性评分、推荐原因、验证状态和失败信息。

## 页面和接口

- 页面：`/sources` 的“自动发现采集源”区域。
- 搜索：`POST /api/v1/sources/search-discovery`
- 候选：`GET /api/v1/sources/discovery-candidates`
- 验证、导入、忽略：候选资源下的单个与批量 POST 接口。

## 数据与验证

V13 新增 `source_discovery_candidate`。集成测试使用 WireMock 模拟 SearXNG JSON 搜索，使用本地固定采集 Provider 验证候选并导入真实 `crawl_source`，同时检查调用日志和页面入口。

## 部署

`compose.yaml` 已加入 SearXNG 服务；独立部署可在第三方能力页填写 `http://127.0.0.1:8088`。
