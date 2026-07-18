# 开发者指南

## 1. 环境

- JDK 17 或更高版本；
- Git；
- IntelliJ IDEA 2024+（推荐但非强制）；
- 项目自带 Maven Wrapper；
- 可选 Ollama，用于 AI 联调。

详细安装见[开发环境搭建](development-environment-setup.md)。

## 2. 获取代码和验证

```bash
git clone <your-repository-url>
cd WorkTwo
./mvnw clean verify
```

Windows 使用 `mvnw.cmd clean verify`。成功后可执行 JAR 位于：

```text
knowledge-collector-boot/target/knowledge-collector.jar
```

## 3. 模块依赖

```text
domain <- application <- infrastructure
                     <- web
capability-api <- capability-provider
infrastructure + web + capability-provider <- boot
```

- `domain` 不依赖 Spring Web、JPA 或具体 Provider。
- `application` 定义用例、端口和查询模型。
- `infrastructure` 实现数据库、存储、调度和备份端口。
- `capability-provider` 实现 HTTP、Feed、HTML、JSON 与 AI 能力。
- `web` 负责协议转换、校验和页面。
- `boot` 负责装配、配置和集成测试。

更多约束见[模块开发说明](module-development-guide.md)和[系统架构](../03-design/system-architecture-design.md)。

## 4. 本地运行

```bash
./mvnw spring-boot:run -pl knowledge-collector-boot -am
```

使用演示数据：

```bash
./mvnw spring-boot:run -pl knowledge-collector-boot -am -Dspring-boot.run.profiles=local
```

默认访问 `http://127.0.0.1:8080`。本地私有配置通过环境变量或未提交的 `.env` 管理，禁止把 Token、密码或证书私钥写入仓库。

## 5. 开发一个垂直功能

1. 在 domain/application 定义规则、命令、查询模型和端口。
2. 在 infrastructure/provider 实现持久化或外部能力。
3. 在 web 增加 REST、MVC、模板和静态资源。
4. 数据库变化新增不可回改的 Flyway 迁移。
5. 同时增加 JUnit、`.http` 请求和阶段文档。
6. 执行 `./mvnw clean verify` 和 `powershell.exe -ExecutionPolicy Bypass -File ./scripts/check-doc-links.ps1`。

## 6. 测试策略

- 纯规则使用快速单元测试。
- 数据库、Flyway、页面和 REST 使用 Spring Boot 集成测试。
- 外部 HTTP/AI 使用 WireMock 或测试 Provider，不依赖实时互联网。
- 不通过跳过测试来声明构建成功。

测试资料见[测试计划](../05-testing/test-plan.md)和[测试报告](../05-testing/test-report.md)。

## 7. 提交与发布

推荐提交前缀：`feat:`、`fix:`、`docs:`、`test:`、`refactor:`。阶段完成后创建 `stage-N` 标签。提交前执行[贡献指南](../../CONTRIBUTING.md)中的检查清单。
