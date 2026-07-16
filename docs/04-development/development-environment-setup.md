# 开发环境搭建

## 1. JDK

安装 JDK 17，设置 `JAVA_HOME`，并确保 `%JAVA_HOME%\bin`/`$JAVA_HOME/bin` 位于 PATH 前部。

```bash
java -version
javac -version
```

两者主版本必须至少为 17。项目 Maven Enforcer 会拒绝 Java 8/11。

## 2. Maven

安装 Maven 3.6.3+：

```bash
mvn -version
```

输出中的 Java home 也必须指向 JDK 17。本机若已有 Maven Wrapper 缓存但 `mvn` 未加入 PATH，可直接调用缓存中的 `mvn.cmd`，但推荐规范安装。

## 3. Git

```bash
git --version
git clone <repository-url>
cd knowledge-collector
```

## 4. IDE

- IntelliJ IDEA 导入根 `pom.xml`。
- Project SDK、Maven Runner JRE 和语言级别统一设置为 17。
- 文件编码使用 UTF-8。
- 不提交 `.idea`、`*.iml` 和个人运行配置。

## 5. 构建与测试

```bash
mvn clean test
mvn clean package
```

Stage 3 集成测试使用临时 H2 文件数据库，不访问真实外部网站。

## 6. 运行

```bash
java -jar knowledge-collector-boot/target/knowledge-collector.jar
```

访问：

- 首页：`http://127.0.0.1:8080/`
- 状态：`http://127.0.0.1:8080/api/v1/system/status`
- 健康：`http://127.0.0.1:8080/actuator/health`

## 7. 配置文件

主配置位于：

```text
knowledge-collector-boot/src/main/resources/application.yml
```

本地覆盖优先使用环境变量，不提交包含秘密的 `application-local.*`。

## 8. 数据目录

默认 `./data`，可设置：

```bash
KNOWLEDGE_COLLECTOR_DATA_DIR=/absolute/path/to/data
```

应用启动时创建 `database`、`article-content`、`snapshots`、`exports` 和 `logs`。代码只保存配置根目录下的规范化路径。

## 9. 数据库迁移

迁移位于 infrastructure 模块的 `src/main/resources/db/migration`。已发布迁移不得修改；新增迁移使用 `V{id}__description.sql`。

JPA 配置固定为：

```yaml
spring.jpa.hibernate.ddl-auto: validate
```

## 10. 常见环境问题

- `invalid target release: 17`：Maven 实际使用了旧 JDK。
- `mvn not recognized`：Maven 未加入 PATH。
- 端口 8080 被占用：设置 `KNOWLEDGE_COLLECTOR_SERVER_PORT`。
- 数据库被占用：确认没有另一个实例使用同一数据目录。
- 首次构建下载失败：检查 Maven mirror、代理和仓库配置；应用运行本身不要求联网。
