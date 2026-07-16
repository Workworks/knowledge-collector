# 第三方组件与依赖

依赖版本由 Spring Boot 3.5.16 BOM 统一管理，除构建插件外不在子模块分散锁版本。

| 组件 | 版本基线 | 用途 | 许可证 | 必须 | 替代方案 |
| --- | --- | --- | --- | --- | --- |
| Spring Boot | 3.5.16 | 应用、自动配置与依赖管理 | Apache-2.0 | 是 | 手工 Spring Framework |
| Spring MVC | Boot 管理 | 页面与 REST | Apache-2.0 | 是 | JAX-RS |
| Spring Data JPA | Boot 管理 | 持久化基础 | Apache-2.0 | 是 | JDBC/MyBatis |
| Hibernate ORM | 6.6.x（Boot 管理） | JPA 实现与 validate | LGPL-2.1 | 是 | EclipseLink |
| H2 Database | 2.3.232 | 本地文件数据库 | MPL-2.0/EPL-1.0 | 是 | SQLite/PostgreSQL |
| Flyway | 11.7.2 | 数据库版本迁移 | Apache-2.0 | 是 | Liquibase |
| Thymeleaf | Boot 管理 | 服务端 HTML 模板 | Apache-2.0 | 是 | FreeMarker |
| Tomcat Embedded | 10.1.x（Boot 管理） | 本地 HTTP 服务 | Apache-2.0 | 是 | Jetty/Undertow |
| Logback/SLF4J | Boot 管理 | 控制台和滚动文件日志 | EPL/LGPL、MIT | 是 | Log4j2 |
| Hibernate Validator | 8.0.x（Boot 管理） | Bean Validation | Apache-2.0 | 是 | 手工校验 |
| Spring Boot Actuator | 3.5.16 | health/info | Apache-2.0 | 是 | 自建健康接口 |
| JUnit 5/AssertJ | Boot 管理 | 自动化测试 | EPL-2.0 / Apache-2.0 | 开发 | TestNG/Hamcrest |

## 尚未加入

Jsoup、Rome、Bootstrap 等会在对应采集或 UI 阶段加入，Stage 3 不提前引入未使用组件。

## 官方地址

- Spring Boot：<https://spring.io/projects/spring-boot>
- H2：<https://h2database.com/>
- Flyway：<https://documentation.red-gate.com/flyway>
- Thymeleaf：<https://www.thymeleaf.org/>
- Hibernate：<https://hibernate.org/>
- JUnit：<https://junit.org/junit5/>

## 安全与升级

- 定期关注 Spring、H2、Flyway 和模板引擎安全公告。
- 升级 Spring Boot 前阅读迁移指南并运行完整测试。
- H2/Flyway 升级前备份数据库并验证迁移兼容性。
- 不使用未知来源 JAR，不在仓库提交本地依赖缓存。
