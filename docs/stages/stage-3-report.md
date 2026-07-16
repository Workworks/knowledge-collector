# Stage 3 阶段报告

## 1. 阶段目标

建立可在本地启动和验证的 Spring Boot 基础工程，完成 H2 文件数据库、Flyway、日志、全局异常、统一 API 响应、本地目录、启动脚本和开发文档。

## 2. 本阶段范围

实现基础运行与可观察性，不实现主题、采集源、采集任务和文章资料库业务。为验证持久化，使用 `system_setting` 保存启动次数；这不是最终业务功能。

## 3. 已完成事项

- 加入 Spring Web、Validation、Thymeleaf、Data JPA、H2、Flyway、Actuator 和测试依赖。
- 创建 Spring Boot 启动类与可执行 JAR 配置。
- 配置默认 `127.0.0.1:8080`、H2 文件库、Flyway 和 `ddl-auto=validate`。
- 创建 Flyway V1：`system_setting` 与 `audit_log`。
- 实现数据目录自动创建和路径规范化检查。
- 实现启动次数及最后启动时间持久化。
- 实现首页、状态 API 和健康检查。
- 实现统一成功/错误响应、字段错误、全局异常和请求关联编号。
- 配置控制台与滚动文件日志。
- 创建 Windows/Linux 启动脚本、README 和三份开发文档。
- 更新需求追踪矩阵的 Stage 3 状态。

## 4. 未完成事项

- 尚未创建主题、来源、规则、任务和文章业务表及实体。
- 尚未实现具体采集、搜索、阅读和调度能力。
- 当前异常处理以 REST 统一响应为主，完整页面错误视图将在页面业务阶段完善。
- `start.sh` 在 Windows Git Bash 中完成语法检查，未在真实 Linux 主机执行运行测试。

## 5. 新增文件

- `.gitattributes`
- `README.md`
- `start.bat`、`start.sh`
- boot 启动类、`application.yml`、`logback-spring.xml`
- Flyway `V1__initialize_foundation.sql`
- Storage、启动状态和系统状态基础设施类
- API 响应、异常处理、关联编号、状态 Controller
- 首页 Controller、模板和 CSS
- `FoundationIntegrationTest`
- 三份 `docs/04-development` 文档
- `docs/stages/stage-3-report.md`

## 6. 修改文件

- application/infrastructure/web/boot 四个模块 POM。
- `docs/02-requirements/requirements-traceability-matrix.md`。

## 7. 核心设计说明

状态查询以 application 的 `SystemStatusQuery` 为端口，由 infrastructure 的 JDBC 适配器实现，web 不直接访问数据库。启动时先由 Flyway 建表，再由 ApplicationRunner 创建本地目录并更新持久化启动状态。外部响应携带 correlationId，未知异常只对外返回稳定错误码。

## 8. 数据库变更

新增 Flyway V1：

- `system_setting`：基础配置与持久化验证。
- `audit_log`：为后续管理操作预留审计表。
- Flyway 自动创建 `flyway_schema_history`。

实际文件：`data/database/knowledge-collector.mv.db`。主题、来源等业务表按后续阶段迁移追加，禁止修改 V1。

## 9. 接口变更

- `GET /api/v1/system/status`：返回应用、H2、Flyway、启动次数和数据目录。
- `GET /actuator/health`：基础健康检查。
- 所有基础 API 响应采用 `ApiResponse<T>`，失败包含 code、message、fieldErrors、timestamp、correlationId。
- 响应头包含 `X-Correlation-Id`。

## 10. 页面变更

新增 `/` 基础状态首页，展示版本、数据库、迁移数、累计启动、数据目录和健康/状态入口。该页面仅用于基础工程验证，不是最终仪表盘。

## 11. 测试情况

最终自动化结果：

- `FoundationIntegrationTest` 3 个集成测试和 `GlobalExceptionHandlerTest` 1 个单元测试全部通过。
- 覆盖 Spring Context/Tomcat 启动、Flyway、H2、状态 API、本地目录和首页渲染。
- 覆盖业务异常到 422 统一错误响应及 correlationId 保留。
- 测试使用临时文件数据库，不依赖实时网络。
- `start.sh` 使用 Git Bash `bash -n` 检查通过。
- Markdown、POM、相对链接检查通过；Git diff 的文件末尾空行告警已在最终提交前清理。

实现过程中发现并修复：

1. 多余的 `flyway-database-h2` 未受 BOM 管理，移除后使用 `flyway-core`。
2. H2 CLOB 不能直接 CAST 为 BIGINT，改为 Java 解析。
3. Flyway 11 历史表使用带引号小写名称，状态查询显式引用。
4. Flyway 历史含 schema 行，迁移统计只计算 version 非空记录。
5. H2 2.3 不支持 `AUTO_SERVER=TRUE` 与 `DB_CLOSE_ON_EXIT=FALSE` 组合，单机应用移除 AUTO_SERVER。
6. `start.bat` 对带空格 JAVA_HOME 的管道解析失败，改用临时文件读取版本。

## 12. 构建与运行结果

- 环境：JDK 17.0.8、Maven 3.9.7。
- `mvn clean test`：BUILD SUCCESS，4 tests，0 failures/errors。
- `mvn clean package`：BUILD SUCCESS。
- 可执行 JAR：`knowledge-collector-boot/target/knowledge-collector.jar`，约 60.6 MB。
- `start.bat` 实际启动成功，端口 18081 验证。
- 首次状态：startupCount=1、migrationCount=1、database=H2。
- 重启后最终读取：startupCount=3，证明同一文件数据库跨重启保留数据。
- 首页 HTTP 200 且包含系统标题；Actuator health 为 UP。
- 每次验证后端口均释放，无遗留验证进程。

第二次重启验证的 PowerShell 汇总脚本误用了只读变量 `$HOME`，导致该次汇总失败；应用本身已启动并更新计数，第三次验证完整成功。

## 13. Git 提交与标签

提交信息：`stage-3: 完成基础工程与本地运行`。标签：`stage-3`。提交与标签均已完成。

## 14. 已知问题

- 系统全局 PATH 的 `java` 仍指向 Java 8；需设置 `JAVA_HOME` 为 JDK 17。
- `mvn` 未加入全局 PATH，本次使用本机 Maven Wrapper 缓存中的 Maven 3.9.7。
- H2 数据库设计为单实例使用，同一数据目录不能同时启动多个应用实例。
- Windows 验证进程使用 `Stop-Process` 停止；日常使用应按脚本提示按 Ctrl+C 正常关闭。

## 15. 风险与技术债务

- 当前无登录，必须继续默认仅监听本机。
- 基础首页尚未使用 Bootstrap，Bootstrap 将在正式管理 UI 阶段按离线资源方式加入。
- `audit_log` 只有结构，实际审计写入随管理功能实现。
- 尚无完整页面错误模板和配置元数据提示。

## 16. 验收结果

Stage 3 技术验收条件已全部满足：应用可启动、H2 文件持久化、Flyway 生效、目录和日志创建、统一响应与异常框架可用、测试与打包成功、Windows 启动脚本实际验证成功，并已完成 Git 提交与 `stage-3` 标签。

## 17. 下一阶段计划

Stage 4 将实现主题和采集源领域模型、Flyway 业务表、JPA Repository、应用服务、REST API、管理页面以及单元/集成测试。
