# 页面与 IDEA 接口测试指南

## 1. 目标

本指南帮助页面测试人员和接口测试人员验证当前应用。所有测试均面向本机应用，不依赖外部网站。

## 2. 启动应用

先使用 JDK 17 构建并启动：

```bash
mvn clean package
java -jar knowledge-collector-boot/target/knowledge-collector.jar
```

Windows 也可以设置 `JAVA_HOME` 后运行：

```bat
start.bat
```

默认地址为 `http://127.0.0.1:8080`。

## 3. 使用 Web 接口测试台

浏览器打开：

```text
http://127.0.0.1:8080/test-console
```

测试台支持：

- 一键选择系统状态、健康检查、首页和测试台页面。
- 使用 GET、POST、PUT、PATCH、DELETE 方法。
- 编辑当前应用内的相对请求路径。
- 添加 `X-Correlation-Id`，用于关联响应和日志。
- 为非 GET 请求填写 JSON 请求体。
- 查看 HTTP 状态、耗时、响应头和格式化后的响应体。

测试台有意限制为当前应用的相对路径，不允许把它作为外部网址请求工具。

## 4. 使用 IDEA HTTP Client

IntelliJ IDEA 自带 HTTP Client，可直接运行项目中的：

```text
http/knowledge-collector.http
```

使用方法：

1. 启动应用。
2. 在 IDEA 中打开 `http/knowledge-collector.http`。
3. 点击每个请求左侧的绿色运行图标。
4. 在右侧响应窗口查看状态、响应头、响应体和测试结果。
5. 需要切换端口时，批量替换文件中的 `http://127.0.0.1:8080`。

请求文件有意使用完整 URL，不依赖 `{{baseUrl}}` 等变量替换，以兼容未启用变量解析的 IDEA HTTP Client 版本或配置。

该请求集目前包含：

- 系统状态接口及响应断言。
- Actuator 健康检查及 `UP` 断言。
- Web 测试台页面检查。
- 系统首页检查。

后续新增主题、来源、任务和文章接口时，应在同一目录按业务模块增加 `.http` 文件，并为关键字段添加响应断言。

## 5. 当前建议检查项

| 检查项 | 预期结果 |
| --- | --- |
| 首页 | HTTP 200，展示系统标题和测试台入口 |
| 测试台 | HTTP 200，可选择预置请求 |
| 系统状态 | HTTP 200，`success=true`，数据库为 H2 |
| 健康检查 | HTTP 200，`status=UP` |
| 关联编号 | 合法的请求编号在响应头和统一响应体中保留 |
| 重启持久化 | 重启后系统状态中的 `startupCount` 增加 |

## 6. 问题记录

发现问题时至少记录：

- 测试时间和应用版本。
- 请求方法、路径和请求体。
- HTTP 状态码。
- `X-Correlation-Id`。
- 实际响应与预期响应。
- 页面截图或 IDEA HTTP Client 响应。
- `data/logs` 中与关联编号对应的日志。
