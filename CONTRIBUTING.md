# 贡献指南

感谢参与 Knowledge Collector。提交代码即表示你有权贡献该内容，并同意按项目的 Apache License 2.0 发布。

## 开始之前

1. 阅读 [README](README.md)、[开发者指南](docs/04-development/developer-guide.md)和[系统架构](docs/03-design/system-architecture-design.md)。
2. Issue 中说明问题、预期行为和复现步骤；安全问题请遵循[安全策略](SECURITY.md)。
3. 大型架构或数据模型变化先讨论方案，避免重复工作。

## 本地验证

```bash
./mvnw clean verify
```

Windows：

```bat
mvnw.cmd clean verify
```

文档变更还需运行：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File ./scripts/check-doc-links.ps1
```

## 代码要求

- 保持 domain/application 与具体 Web、数据库和 Provider 解耦。
- 使用构造器注入、Java Time API 和 UTF-8。
- 数据库变化必须新增 Flyway 迁移，禁止修改已发布迁移。
- 外部网络测试使用固定夹具或 WireMock。
- 日志不得包含 Token、Cookie、密码、正文或个人敏感数据。
- 新功能同时补充页面/API、自动化测试、`.http` 请求和文档。

## 提交和 Pull Request

- 使用清晰的小提交，推荐 `feat:`、`fix:`、`docs:`、`test:`、`refactor:`。
- PR 描述应包含目标、实现、验证命令、数据库影响和页面变化。
- 不提交 `target/`、运行数据库、日志、备份、`.env`、IDE 配置或本机路径。
- 保持工作区中与本次变更无关的文件不进入提交。
