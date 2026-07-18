# 第三方服务账号与凭据管理手册

本文集中说明所有第三方服务是否有账号、凭据在哪里配置、Compose 初始化默认值、如何修改和如何重置。生产或团队环境不得继续使用示例密码。

## 1. 凭据唯一配置入口

Docker 部署统一使用项目根目录 `.env`。创建方法：

```powershell
Copy-Item .env.example .env
notepad .env
git check-ignore .env
```

最后一条必须输出 `.env`。真实密码、API Key、访问令牌不得写入 `.env.example`、Compose、IDEA `.http` 文件或 Git。建议用密码管理器生成并保存至少 20 位随机密码；修改后执行：

```powershell
docker compose up -d --force-recreate <服务名>
```

## 2. 服务账号总表

| 服务 | 默认用户名 | Compose 默认密码 | 推荐 `.env` 变量 | 是否有登录页 |
| --- | --- | --- | --- | --- |
| Knowledge Collector | 无 | 无 | 当前版本只监听本机 | 无 |
| Ollama | 无 | 无 | `OLLAMA_MODEL` | 无 |
| SearXNG | 无 | 无 | 无 | 无 |
| Playwright | 无 | 无 | 无 | 无 |
| Apache Tika | 无 | 无 | 无 | 无 |
| Qdrant | 无 | 无 | 可扩展 `QDRANT__SERVICE__API_KEY` | 无 |
| MinIO | `knowledge` | `collector-secret` | `MINIO_ROOT_USER`、`MINIO_ROOT_PASSWORD` | `http://127.0.0.1:9001` |
| Grafana | `admin` | `change-this-password` | `GRAFANA_ADMIN_USER`、`GRAFANA_ADMIN_PASSWORD` | `http://127.0.0.1:3000` |
| n8n 2.x | 无固定默认账号 | 无固定默认密码 | `N8N_ENCRYPTION_KEY`；首次打开页面创建 Owner | `http://127.0.0.1:5678` |
| ntfy | 当前配置无账号 | 无 | 当前仅绑定 `127.0.0.1` | `http://127.0.0.1:8085` |
| Prometheus | 无 | 无 | 无 | `http://127.0.0.1:9090` |
| Firecrawl | 按供应商 | 按供应商 | `KNOWLEDGE_COLLECTOR_FIRECRAWL_API_KEY` | 供应商控制台 |
| 云端模型 | 按供应商 | API Key | `KNOWLEDGE_COLLECTOR_CLOUD_LLM_API_KEY` | 供应商控制台 |
| Langfuse | 首次部署创建 | 首次部署创建 | `LANGFUSE_PUBLIC_KEY`、`LANGFUSE_SECRET_KEY` | 配置的 Langfuse 地址 |

表中的 Compose 默认值是代码兜底值，不是安全密码。`.env.example` 使用 `change-this-*` 明确要求部署前替换。

## 3. MinIO

初始化时 `minio` 和 `minio-init` 同时读取以下值，应用也使用同一组凭据：

```dotenv
MINIO_ROOT_USER=knowledge-admin
MINIO_ROOT_PASSWORD=请替换为随机密码
```

修改或重置 root 密码：编辑 `.env`，确保两个变量同时修改，然后：

```powershell
docker compose up -d --force-recreate minio minio-init app
docker compose logs --tail 50 minio minio-init app
```

验证：登录 9001，并在“第三方能力”测试 MinIO。命名卷不会因改密码丢失对象。若使用 MinIO 子用户，应在 MinIO Console 的 Identity → Users 单独轮换 Access Key，并同步修改应用配置。

## 4. Grafana

首次创建 `grafana_data` 卷时读取：

```dotenv
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=请替换为随机密码
```

已初始化的数据卷不会因为再次修改环境变量自动覆盖管理员密码。安全重置命令：

```powershell
docker compose exec grafana grafana cli admin reset-admin-password "新的随机密码"
```

若命令提示旧名称弃用，可用 `grafana-cli admin reset-admin-password`。随后用新密码登录 3000。不要删除 `grafana_data` 来重置密码，否则仪表盘、用户和设置都会丢失。

## 5. n8n 2.x

当前 n8n 没有可依赖的固定默认用户名密码。第一次打开 5678 时必须创建 Owner 邮箱和密码，例如邮箱 `admin@example.local`，密码使用密码管理器生成。`N8N_ENCRYPTION_KEY` 用于加密 n8n 保存的连接凭据，部署后必须长期保持不变：

```dotenv
N8N_ENCRYPTION_KEY=至少32位且长期保存的随机字符串
```

忘记 Owner 密码且邮件找回不可用时：

```powershell
docker compose exec n8n n8n user-management:reset
docker compose restart n8n
```

然后重新打开 5678 创建 Owner。此命令会重置用户管理状态，执行前必须备份 `n8n_data`。不要随意更换 `N8N_ENCRYPTION_KEY`，否则旧工作流凭据无法解密。

## 6. 无内置登录的本地服务

Ollama、SearXNG、Playwright、Tika、Prometheus、ntfy 和当前 Qdrant 配置没有用户密码，因此 Compose 端口必须继续绑定 `127.0.0.1`。不要改成 `0.0.0.0` 暴露到局域网或公网。若需要远程访问，应在反向代理层增加 TLS、身份认证和 IP 白名单。

Qdrant 需要 API Key 时，可在服务环境变量设置 `QDRANT__SERVICE__API_KEY`，并同步扩展应用 Provider 的 `api-key` 请求头；当前本机部署不启用该模式。ntfy 多用户部署应启用 auth-file 和 `auth-default-access: deny-all`，再用 `ntfy user add` 创建用户；当前本机版依靠回环地址隔离。

## 7. 外部 API Key 与系统能力页

Firecrawl、云模型和 Langfuse Key 应优先来自 `.env` 或部署平台 Secret。系统“第三方能力”页面中保存的 credential 会进入本地 H2 数据库，因此数据库备份也属于敏感文件；只能由管理员保管。轮换顺序：先在供应商创建新 Key，更新系统并测试成功，再撤销旧 Key，最后重新创建备份。

云模型业务页面的临时 API Key只用于本次请求，Stage 34 自动化测试已验证它不会写入执行日志。

## 8. 凭据泄露与整体重置

发现泄露时立即：撤销外部 Key；轮换 MinIO/Grafana/n8n；停止对外端口；检查 `git log -p` 与执行日志；重新构建容器。不要只删除当前文件，因为 Git 历史仍可能保留秘密。若秘密曾提交到 GitHub，应同时清理历史并在供应商侧作废。

完全删除本机第三方数据属于破坏性操作，仅在已有备份且明确需要全新初始化时手工执行 `docker compose down` 后删除指定命名卷；不要直接使用 `docker compose down -v` 清除全部数据。
