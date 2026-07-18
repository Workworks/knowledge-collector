# WorkTwo8 测试报告

## 自动化覆盖

`WorkTwo8SecurityIntegrationTest` 在真正开启安全配置的独立 H2 数据库上验证：未登录 401/跳转、管理员登录、用户创建、BCrypt 非明文、普通用户菜单和路由/API 403、账户停用、退出失效、审计记录、十项第三方系统种子、WireMock 健康成功、打开方式和普通用户越权。`FoundationIntegrationTest` 同步验证 Flyway V16 和完整管理员导航。

执行命令：

```powershell
mvnw.cmd clean test
mvnw.cmd clean package
```

人工验收按 [WorkTwo8 使用与验收手册](../07-user-guide/worktwo8-security-and-external-systems.md) 执行，并使用登录页、用户管理、普通用户首页、无权页面、第三方系统页面进行桌面和窄屏检查。
