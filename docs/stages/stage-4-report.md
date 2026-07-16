# Stage 4 阶段报告

## 1. 阶段目标

完成主题和采集源管理闭环，包括领域模型、数据表、持久化、应用服务、REST API、管理页面、主题—来源关系和自动化测试。

## 2. 范围

本阶段管理来源配置，不执行真实网络请求、不解析 RSS/Atom、不创建采集任务。来源测试接口明确返回未开放状态，真实 Provider 从 Stage 5 开始实现。

## 3. 已完成事项

- Topic、CrawlSource、SourceType 领域模型和编码/关键词/URL 规则。
- Flyway V2：`topic`、`crawl_source`、`topic_source_rel`。
- JPA 实体、Repository 适配器、分页和筛选。
- 主题创建、查询、修改、启停和受控删除。
- 采集源创建、查询、修改、启停和删除。
- 一个采集源关联多个主题。
- 重复编码/名称、非法 URL、无效主题关系和删除冲突处理。
- `/topics`、`/sources` 中文管理页面。
- Web 测试台 Stage 4 预置入口和 IDEA HTTP Client 请求集。
- OpenAPI 3、Swagger UI 和 local/test/production Profile。
- local-only `/dev/tools` 与幂等演示主题/来源初始化、清理。
- Maven Wrapper 和依赖收敛、重复依赖 Enforcer 门禁。
- 自定义目录健康指标。
- 单元测试和完整 CRUD 集成测试。

## 4. 数据库变更

Flyway V2 新增：

- `topic`：唯一编码和名称、关键词文本、展示属性、启停、排序和乐观锁。
- `crawl_source`：类型、地址、访问限制、内容策略、健康字段和乐观锁。
- `topic_source_rel`：主题和来源的唯一多对多关系。

继续使用 `ddl-auto=validate`，未使用 Hibernate 自动建表。

## 5. REST API

主题：

- `GET/POST /api/v1/topics`
- `GET/PUT/DELETE /api/v1/topics/{id}`
- `PATCH /api/v1/topics/{id}/enabled`
- `GET /api/v1/topics/options`

采集源：

- `GET/POST /api/v1/sources`
- `GET/PUT/DELETE /api/v1/sources/{id}`
- `PATCH /api/v1/sources/{id}/enabled`
- `POST /api/v1/sources/{id}/test`

来源测试在 Stage 4 返回 HTTP 422 和 `SOURCE-TEST-NOT-AVAILABLE`。

## 6. 页面

- `/topics`：搜索、状态筛选、分页、新建、编辑、启停、删除和空状态。
- `/sources`：搜索、类型/状态筛选、分页、新建、编辑、多选主题、启停、删除及测试能力提示。
- 首页增加主题、来源入口。
- 所有数据操作调用 REST API，页面和 IDEA 请求验证同一契约。

## 7. 业务规则

- 编码规范化为大写，主题/来源编码唯一，主题名称唯一。
- 关键词支持逗号、中文逗号和换行输入，去空白并去重。
- 来源 URL 只接受 HTTP/HTTPS。
- 超时 1—120 秒，重试 0—10 次，请求间隔 0—3600000 毫秒。
- 关联来源的主题禁止删除；来源删除先清理关系。
- 停用不等同删除。

## 8. 测试

测试覆盖：

- 编码、关键词和 URL 规则。
- Flyway V1/V2 与 Hibernate validate。
- 主题创建和规范化。
- 来源创建与主题关系。
- 按主题筛选来源。
- 关联主题删除冲突。
- 来源启停和删除。
- Stage 5 来源测试边界响应。
- 主题和来源管理页面渲染。

最终结果：

- `mvnw.cmd clean verify`：BUILD SUCCESS。
- 10 个自动化测试全部通过，0 failures/errors。
- Java、Maven、依赖收敛和重复依赖 Enforcer 规则通过。
- JavaScript 语法、IDEA 请求 URI、Markdown 链接和 Git diff 检查通过。
- 真实运行 CRUD、409 删除冲突、422 来源测试边界和页面访问通过。
- OpenAPI 3.1 文档包含主题和来源路径。
- local Profile 真实启动：3 个演示主题、2 个演示来源，`/dev/tools` HTTP 200，自定义目录健康组件为 UP。

## 9. 已知边界

- 来源测试、RSS/Atom 解析和手动采集从 Stage 5 实现。
- 采集规则页面和版本管理从 Stage 6 实现。
- 尚未产生文章，因此删除来源只涉及配置和主题关系。
- 当前单用户无登录，继续只监听本机。
- WireMock、网络 Fixture、DNS/IP SSRF 校验和重定向复检依赖 Stage 5 的 HTTP 客户端与 Provider。
- 演示文章和成功/失败任务依赖 Stage 5 的任务、文章表；本阶段只初始化已有主题与来源。
- 调度器和最近任务健康详情明确标记为后续阶段，不伪造状态。

## 10. 补充要求处理

开发过程中新增的 `WorkTwoInc.md` 已纳入检查。本阶段补齐 OpenAPI、Profile、local 工具、Maven Wrapper、Enforcer、细分 HTTP 用例和目录健康检查。与尚未存在的采集、规则、文章、调度模型绑定的要求，分别进入 Stage 5、6、8、9。

## 11. 下一阶段

Stage 5 实现 RSS/Atom Provider、受限 HTTP 客户端、任务模型、URL 标准化、固定测试样例、去重和文章基础入库。

## 12. Git 里程碑

提交信息：`stage-4: 完成主题与采集源管理`。标签：`stage-4`。
