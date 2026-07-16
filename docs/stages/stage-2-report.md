# Stage 2 阶段报告

## 1. 阶段目标

建立原型、UI、系统架构、详细设计、数据库和 API 设计基线，并创建可由 Maven 解析、编译和打包的五模块项目骨架。

## 2. 本阶段范围

本阶段覆盖需求设计映射、页面原型、模块和数据边界、核心算法与接口契约、POM 和包目录。未创建 Spring Boot 启动类、应用配置、Flyway 脚本、实体、Controller、模板或业务实现，这些属于 Stage 3 及以后。

## 3. 已完成事项

- 复核 Stage 1 报告、需求、假设、Git 提交和 `stage-1` 标签。
- 确认模块化单体、正文混合存储、JSON API 基础映射和本机绑定决策。
- 创建需求追踪矩阵，覆盖全部 48 个唯一需求编号。
- 完成原型、UI、概要架构、详细设计、数据库和 API 六份设计文档。
- 创建根 POM、五个模块 POM 和标准 Java 17 包骨架。
- 配置 Java 17/Maven 3.6.3 最低版本门禁。
- 使用本机现有 JDK 17 和 Maven 3.9.7 完成测试与打包验证。

## 4. 未完成事项

- 尚无 Spring Boot 启动类和可运行 Web 应用。
- 尚未加入 Spring MVC、JPA、H2、Flyway、Thymeleaf、Jsoup、Rome 等具体依赖与配置。
- 尚无业务代码和自动化测试用例；Maven 输出明确为 `No tests to run`。
- Mermaid 仅完成源码结构检查，未在独立渲染器中逐图截图验收。

## 5. 新增文件

- `docs/02-requirements/requirements-traceability-matrix.md`
- `docs/03-design/prototype-design.md`
- `docs/03-design/ui-design-guidelines.md`
- `docs/03-design/system-architecture-design.md`
- `docs/03-design/detailed-design.md`
- `docs/03-design/database-design.md`
- `docs/03-design/api-design.md`
- 根目录及五模块 `pom.xml`
- 五个模块的 `package-info.java`
- `docs/stages/stage-2-report.md`

## 6. 修改文件

- `docs/01-project/assumptions.md`：关闭 A-001—A-004 决策。
- 原始需求输入在本阶段开始前已由用户工作区从 `WorkTwo.txt` 改名为 `WorkTwo.md`；本阶段保留并纳入 Git，不修改其正文。

## 7. 核心设计说明

采用五模块模块化单体：

```text
domain
  ↑
application
  ↑             ↑
infrastructure  web
       ↑        ↑
          boot
```

domain 保持框架无关；application 编排用例；infrastructure 实现 JPA、HTTP、解析和存储；web 提供页面与 REST；boot 负责启动和装配。外部网络 I/O 不持有长事务，每个候选文章使用独立短事务。

## 8. 数据库变更

没有执行数据库变更。数据库文档已定义 topic、crawl_source、crawl_rule、crawl_task、article、article_content、关系表、标签、笔记、设置和审计表及索引/生命周期。实际 Flyway 迁移从 Stage 3 开始。

## 9. 接口变更

没有实现接口。设计基线确定 REST 前缀 `/api/v1`、统一响应、分页、错误码、主题/来源/规则/任务/文章/运维接口清单以及未来安全预留。

## 10. 页面变更

没有实现页面。原型已定义仪表盘、主题、来源、规则测试、任务、资料库、阅读详情和系统设置的布局、字段、操作、跳转及加载/空/异常状态。

## 11. 测试情况

- 6 个 POM 均通过 XML 解析。
- 根 POM 的 5 个模块目录与子 POM 均存在。
- 模块依赖方向检查符合设计。
- 6 份必需设计文档存在且非空。
- 16 个 Markdown 文件均非空；相对链接检查无断链。
- 48 个唯一需求编号均进入追踪矩阵。
- Java 文件只有 5 个 `package-info.java`，未提前加入业务实现。
- `git diff --check` 未发现格式错误。
- `mvn clean test` 成功；所有模块显示无测试可运行。

## 12. 构建与运行结果

- 验证环境：JDK 17.0.8、Maven 3.9.7。
- `mvn clean test`：BUILD SUCCESS，六个 Reactor 项目成功。
- `mvn clean package`：BUILD SUCCESS，五个模块生成普通骨架 JAR。
- 未运行应用：boot 模块尚无启动类，当前 JAR 不是可执行 Spring Boot JAR。
- 系统默认 PATH 仍指向 Java 8，验证命令使用临时 `JAVA_HOME` 指向已安装的 JDK 17，未修改系统环境。

## 13. Git 提交与标签

提交 `stage-2: 完成架构设计与 Maven 项目骨架` 已成功执行，标签 `stage-2` 已成功创建。阶段报告回填后使用 amend 纳入同一阶段提交，并将标签更新到最终提交。

## 14. 已知问题

- 系统默认 `java` 为 1.8，后续开发命令必须配置 JDK 17。
- `mvn` 不在 PATH；本机 Maven 3.9.7 位于 Maven Wrapper 缓存。
- C4 Mermaid 图依赖渲染器扩展，不支持时需退化为普通 Mermaid flowchart。
- Git 分支继续保留为 `master`，避免在阶段交付中引入无业务价值的重命名；如需统一为 `main` 应单独决策。

## 15. 风险与技术债务

- H2 CLOB 模糊搜索性能仍需 Stage 10 基准验证。
- 文件和数据库混合存储需要在 Stage 3/9 实现一致性与备份恢复。
- 无登录模式必须落实 `127.0.0.1` 默认绑定。
- 模块 POM 尚未加入具体框架依赖，Stage 3 需谨慎维护依赖方向。

## 16. 验收结果

Stage 2 验收通过：设计文档齐全、需求可追踪、模块边界明确、POM 可解析，且在 JDK 17/Maven 3.9.7 下测试和打包成功；Git 提交和 `stage-2` 标签已创建。本结论不表示应用已可运行，也不表示已有业务自动化测试。

## 17. 下一阶段计划

Stage 3 将实现 Spring Boot 启动、H2 文件数据库、Flyway、日志、全局异常、统一响应、本地文件目录、启动脚本、README、开发环境与第三方依赖文档，并完成实际应用启动和持久化验证。
