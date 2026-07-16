# Stage 3 补充：页面与接口测试工具报告

## 1. 目标

在进入 Stage 4 前，为页面测试人员和接口测试人员提供可直接操作的测试入口，不提前实现主题、采集源等下一阶段业务。

## 2. 完成内容

- 新增 `/test-console` Web 接口测试台。
- 支持预置请求、自定义相对路径、常用 HTTP 方法、JSON 请求体和关联编号。
- 展示 HTTP 状态、耗时、响应头和格式化响应体。
- 新增 IDEA HTTP Client 请求文件及自动断言。
- 首页增加测试台入口。
- 新增页面与接口手工测试指南。
- 增加测试台页面和静态脚本的集成测试。

## 3. 安全边界

Web 测试台只接受以单个 `/` 开头的当前应用相对路径，不允许完整外部网址。它是本地开发测试工具，不提供外部网页代理或采集能力。

## 4. 文件

- `knowledge-collector-web/src/main/resources/templates/test-console.html`
- `knowledge-collector-web/src/main/resources/static/js/test-console.js`
- `http/knowledge-collector.http`
- `docs/05-testing/manual-api-testing.md`

## 5. 下一步

Stage 4 每增加一组主题或采集源接口，同步增加测试台预置入口、IDEA `.http` 请求和自动化测试。

## 6. 验证结果

- `mvn clean test`：成功，5 个测试全部通过。
- `mvn package -DskipTests`：成功，生成可执行 JAR。
- JavaScript `node --check`：通过。
- 真实应用运行验证：首页、测试台、静态脚本、状态 API 均返回 HTTP 200。
- Actuator 健康状态为 `UP`。
- 自定义 `X-Correlation-Id` 在响应头和统一响应体中均正确保留。
- Markdown 相对链接和 Git diff 检查通过。
- IDEA 请求文件使用明确的本机 URL，不依赖可能未启用的文件内变量替换。

当前会话未提供浏览器交互控制工具，因此未执行自动点击和截图级视觉回归；页面路由、资源加载和接口交互基础条件已通过真实 HTTP 与集成测试验证。

## 7. Git 里程碑

- 功能提交：`stage-3: 增加 Web 测试台与 IDEA 接口请求`。
- 兼容性修复：`fix: 修复 IDEA HTTP Client URI 变量解析`。
- 标签：`stage-3-testing-tooling`，指向包含兼容性修复的最终状态。
