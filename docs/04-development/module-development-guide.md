# 模块开发说明

## 1. 依赖方向

```text
domain <- application <- infrastructure
                     <- web
infrastructure + web <- boot
```

禁止 domain/application 依赖具体 Web、JPA、Jsoup 或文件实现。

## 2. 模块职责

- domain：实体、值对象、枚举、领域规则和端口。
- application：命令、查询、DTO、用例编排和事务意图。
- infrastructure：JPA/JDBC、HTTP、Feed、HTML、存储和调度适配器。
- web：页面/REST Controller、校验、统一响应、模板。
- boot：启动类、配置、装配和测试入口。

## 3. 当前基础调用

`HomeController/SystemStatusController → SystemStatusQuery → JdbcSystemStatusQuery → H2`。Controller 不直接查询数据库。

## 4. 增加新能力

- 新主题/来源用例：先在 application 定义命令或查询，再在 infrastructure 实现持久化。
- 新采集类型：实现 domain 端口 `CrawlProvider`，在 infrastructure 注册。
- 新正文解析器：实现 `ArticleContentExtractor`。
- 新主题分类器：实现 `TopicClassifier`。
- 新搜索实现：实现 `ArticleSearchService`，调用方不感知数据库或 Lucene。

## 5. 异常与响应

- 资源不存在：`ResourceNotFoundException`。
- 可预期业务拒绝：`BusinessRuleException` + 稳定错误码。
- Web 校验错误由 `GlobalExceptionHandler` 转换为统一响应。
- 未知异常记录关联编号和堆栈，对外不暴露内部路径。

## 6. 配置规范

配置属性放在 boot 的 `application.yml`，复杂配置使用 `@ConfigurationProperties`。默认值必须安全，网络绑定默认为 `127.0.0.1`。

## 7. 数据库规范

- 结构只通过 Flyway 变更。
- 迁移不可回改。
- 网络 I/O 不放在数据库长事务中。
- Repository 不承担质量评分、主题匹配等业务规则。

## 8. 测试规范

- 规则类优先单元测试。
- Web/数据库流程使用集成测试。
- 自动化测试使用本地固定资源和临时目录，不依赖实时网站。
- 测试失败必须修复或在阶段报告记录，禁止跳过后声称成功。

## 9. 编码规范

- Java 17，UTF-8，Java Time API。
- 构造器注入，不使用字段注入。
- 类保持单一职责，DTO 不直接替代领域模型。
- 不在日志记录 Token、完整 Cookie 或文章正文。

## 10. 提交规范

推荐 `feat:`、`fix:`、`test:`、`docs:`；阶段提交使用 `stage-N:`。阶段完成创建 `stage-N` 标签。
