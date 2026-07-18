# WorkTwo8 使用与验收手册

本文是登录、权限、用户管理和第三方系统入口的一站式操作手册。示例环境为 Windows 11、Docker Desktop，应用地址 `http://127.0.0.1:8080`。

## 1. 首次部署与管理员初始化

在项目根目录复制环境文件：

```powershell
Copy-Item .env.example .env
notepad .env
```

至少修改以下内容，示例密码只用于演示，正式环境必须换成独立随机密码：

```dotenv
KNOWLEDGE_COLLECTOR_SECURITY_ENABLED=true
KNOWLEDGE_COLLECTOR_INITIAL_ADMIN_USERNAME=admin
KNOWLEDGE_COLLECTOR_INITIAL_ADMIN_PASSWORD=AdminChangeMe123!
KNOWLEDGE_COLLECTOR_SESSION_TIMEOUT=8h
```

验证密码必须不少于 12 位，并同时包含大写字母、小写字母和数字。启动并检查：

```powershell
docker compose up -d --build
docker compose ps
docker compose logs --tail 100 app
```

浏览器打开 `/login` 应显示登录页；未登录直接打开 `/articles` 应跳回登录页。首次初始化后可从 `.env` 删除 `KNOWLEDGE_COLLECTOR_INITIAL_ADMIN_PASSWORD`，再重建 app 容器，数据库中的 BCrypt 密码不受影响。

## 2. 登录、退出与暴力尝试保护

在登录页填写：用户名 `admin`，密码为上一步实际配置值，点击“登录”。成功后进入首页；错误密码统一提示“用户名或密码错误”，不会说明用户名是否存在。相同用户名连续失败 5 次后锁定 10 分钟。点击顶部“退出”后，再打开 `/users` 应重新要求登录。

## 3. 创建可验证的普通用户

进入“用户管理”，填写：

| 字段 | 可验证示例 |
| --- | --- |
| 用户名 | `researcher` |
| 显示名称 | `研究员示例` |
| 角色 | `USER` |
| 启用 | 勾选 |
| 初始密码 | `Researcher123!` |

点击“保存用户”。列表应显示角色、启用状态和“从未登录”。退出管理员，使用该账号登录：应能打开文章、资料库、归档库、知识工作台、AI 助手和个人信息；顶部不应出现用户管理、第三方系统、运维、接口测试。即使手工输入 `/users` 也会进入“无权访问”，请求 `/api/v1/users` 返回 403。

编辑用户并取消“启用”，保存后该用户下一次登录必定失败。点击“重置密码”，输入 `ResearcherReset123!`；重新启用后，新密码可登录、旧密码不可登录。角色变更在用户重新登录后生效。

## 4. 修改自己的密码

登录后进入“个人信息”，填写当前密码 `ResearcherReset123!` 和新密码 `ResearcherFinal123!`。提交成功后退出，用新密码登录验证。当前密码错误或新密码不满足规则时，数据不会改变。

## 5. 第三方系统统一入口

管理员进入“第三方系统”。系统首次迁移会生成 MinIO、Grafana、Ollama、Langfuse、SearXNG、Firecrawl、Qdrant、Prometheus、ntfy、n8n 十项入口。以 Grafana 为例填写：

| 字段 | Docker Desktop 示例 |
| --- | --- |
| 代码 | `GRAFANA` |
| 名称 | `Grafana` |
| 类型 | `OBSERVABILITY` |
| 图标 | `📊` |
| 访问地址 | `http://127.0.0.1:3000` |
| 健康检查地址 | `http://grafana:3000/api/health` |
| 打开方式 | `新标签页` |
| 允许角色 | `ADMIN` |
| 排序 | `20` |
| 启用 | 勾选 |

点击“测试连接”。容器可访问且响应小于 500 时状态为“正常”，并更新最近检测和最近成功时间；超时、拒绝连接或 5xx 为“异常”并保存原因；地址为空为“未配置”；停用后为“已停用”。检测失败只影响该入口，不影响知识采集主业务。

“打开系统”先由后端再次验证状态和 URL，再按当前页/新标签配置跳转。仅允许无用户信息的 HTTP/HTTPS URL，例如 `http://admin:password@host` 会被拒绝；系统不会把密码、Token 拼到跳转 URL，也不会绕过 Grafana/MinIO 自己的登录机制。

容器内健康地址与浏览器地址不同是正常现象：应用容器访问 Grafana 用 `http://grafana:3000/api/health`，Windows 浏览器打开则用 `http://127.0.0.1:3000`。

## 6. IDEA HTTP Client 验证

打开项目 [worktwo8-auth-and-external-systems.http](../../http/worktwo8-auth-and-external-systems.http)，先将密码变量改成 `.env` 中实际值，按文件顺序运行。IDEA 会保存 Cookie：获取 CSRF、登录、读取个人信息、查询用户和第三方系统都应返回成功。普通用户会话请求管理员接口应返回 403。

## 7. 运维检查

```powershell
docker compose ps
docker compose logs --since 10m app
docker compose logs --since 10m grafana minio ollama
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
```

应用启动时报“首次启动必须设置”时，说明数据库没有用户且初始化变量为空。登录失败统一排查用户名、是否启用、密码和锁定时间。第三方连接异常先用 `docker compose ps` 确认容器，再从 app 容器内验证健康 URL；浏览器地址必须使用 Windows 可访问端口。所有用户和第三方配置变更可在 `audit_log` 中核对操作类型、目标、摘要和时间，日志不应出现密码。

## 8. 验收清单

- 未登录页面跳登录、API 返回 401；普通用户管理员页面跳无权、API 返回 403。
- 管理员可创建、编辑、停用、重置并分页查询用户；用户 JSON 不包含密码或哈希。
- 数据库 `password_hash` 以 BCrypt 格式保存，明文不出现于页面、API 和审计。
- 退出后旧会话立即失效；会话超过配置时间失效。
- 十项第三方系统存在，状态、时间和错误可持久化，入口不携带凭证。
- `mvnw.cmd clean test` 与 `mvnw.cmd clean package` 均通过。
