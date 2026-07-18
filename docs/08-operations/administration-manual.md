# 管理手册

## 调度

运维页可为每个启用采集员设置 1—10080 分钟周期。扫描器默认每 30 秒检查到期配置。

## 任务租约与回收

- 任务开始和处理条目时更新心跳。
- 默认 10 分钟没有心跳视为失效。
- 后台每分钟回收一次，也可调用 `POST /api/v1/operations/tasks/recover-stale`。
- 回收任务记录 `TASK-TIMEOUT`，释放 `active_source_id`，后续任务可正常创建。
- 已被回收的旧线程不能再覆盖最终状态。

可通过 `KNOWLEDGE_COLLECTOR_TASK_STALE_TIMEOUT` 调整，例如 `PT20M`。

## TLS 证书

系统不会关闭证书或主机名校验。信任来源依次为：

1. JDK 默认 CA；
2. Windows 系统根证书库（默认启用）；
3. `KNOWLEDGE_COLLECTOR_ADDITIONAL_CA_FILE` 指定的 PEM CA。

附加文件应只包含管理员确认可信的根证书或中间证书，不要导入来源站点随意提供的未知证书。

## 来源健康

首页和仪表盘展示成功、失败和连续失败次数。连续失败来源应检查 URL、超时、证书、返回格式和访问频率；确认不可用后应停用。

## 备份恢复

日常升级前、批量修改前和重要采集后创建备份。恢复必须停机。恢复完成后检查 Flyway 版本、文章数、最近任务和数据目录。

## 合规

仅采集公开允许访问的内容，优先 RSS/Atom；遵守 robots.txt，不绕过登录、验证码、访问控制和付费墙。

## 运维工具

| 工具 | 用途 | 推荐场景 |
| --- | --- | --- |
| Knowledge Collector 运维页 `/operations` | 业务指标、失败任务、调度、备份 | 每日检查 |
| 增强能力 `/advanced#stage36` | Ollama、Tika、Qdrant、MinIO 和业务统计 | 第三方能力检查 |
| Docker Desktop Containers | 容器状态、资源占用、单服务日志、重启 | 图形化初查 |
| `docker compose` | 状态、日志、进入容器、重建服务 | 标准命令行运维 |
| PowerShell `Invoke-RestMethod` | 健康接口和 API 验证 | 判断网络层或应用层问题 |
| Grafana `:3000` | JVM、HTTP 和趋势指标 | 性能与历史趋势 |
| Prometheus `:9090` | 原始指标、Targets 抓取状态 | Grafana 无数据时 |
| IDEA HTTP Client | 重放 `http/*.http` 请求 | 接口参数和错误响应复现 |

账号和重置操作统一参见[第三方服务账号与凭据管理手册](credential-management.md)。

## 每日检查流程

```powershell
Set-Location C:\Users\你的用户名\Desktop\aiWork\WorkTwo
docker compose ps
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
Invoke-RestMethod http://127.0.0.1:8080/api/v1/advanced/monitor
docker compose logs --since 24h app | Select-String -Pattern 'ERROR|Exception|FAILED|TIMEOUT'
```

通过标准：app 为 healthy，应用为 UP，依赖服务 `available=true`，最近 24 小时没有未处理 ERROR。`minio-init` 为 `Exited (0)` 属于正常的一次性初始化。

## Docker 日志检查

查看全部服务最近 200 行：

```powershell
docker compose logs --tail 200
```

按服务和时间范围检查：

```powershell
docker compose logs --since 30m app
docker compose logs --since 30m ollama qdrant tika minio
docker compose logs --since 30m prometheus grafana ntfy n8n
docker compose logs --follow --tail 100 app
```

`--follow` 会持续占用终端，按 `Ctrl+C` 只退出日志跟随，不会停止容器。筛选错误：

```powershell
docker compose logs --since 2h app 2>&1 |
  Select-String -Pattern 'ERROR|Exception|Caused by|FAILED|TIMEOUT|PKIX'
```

保存故障证据：

```powershell
New-Item -ItemType Directory -Force .\data\logs\support | Out-Null
docker compose ps --all | Out-File .\data\logs\support\containers.txt -Encoding utf8
docker compose logs --since 2h --no-color | Out-File .\data\logs\support\compose.log -Encoding utf8
docker version | Out-File .\data\logs\support\docker-version.txt -Encoding utf8
```

发送日志前检查并删除 API Key、密码、私人 URL、文章正文等敏感内容。

## 分层排错顺序

1. `docker compose ps`：确认容器是否 running/healthy，端口是否冲突。
2. `docker compose logs --tail 200 <service>`：找到最早出现的 ERROR，而不是只看最后一条连锁异常。
3. `Invoke-RestMethod`：直接测试服务健康地址，区分浏览器、应用和容器网络问题。
4. 在 `/capabilities` 测试对应 Provider，检查 endpoint、启停状态与最近错误。
5. 在 `/advanced` 的执行记录查看 request、耗时和失败原因，再按原参数重试。
6. Grafana 看异常发生时间，Prometheus Status → Targets 检查抓取失败。
7. 仍无法定位时导出上述 support 日志包，保留发生时间、操作步骤、关联 ID 和任务 ID。

## 常见服务检查命令

```powershell
# Ollama 模型与日志
docker compose exec ollama ollama list
docker compose logs --tail 100 ollama

# Qdrant collection
Invoke-RestMethod http://127.0.0.1:6333/collections

# Tika
Invoke-WebRequest http://127.0.0.1:9998/tika -UseBasicParsing

# Prometheus 与 Grafana
Invoke-WebRequest http://127.0.0.1:9090/-/ready -UseBasicParsing
Invoke-RestMethod http://127.0.0.1:3000/api/health

# ntfy、n8n、MinIO Console
Invoke-RestMethod http://127.0.0.1:8085/v1/health
Invoke-WebRequest http://127.0.0.1:5678/healthz -UseBasicParsing
Invoke-WebRequest http://127.0.0.1:9001 -UseBasicParsing
```

## 典型错误解释

- `connection refused`：目标容器未启动、端口错误或使用了错误网络地址；容器访问另一个容器应使用服务名，例如 `http://ollama:11434`。
- `401/403`：凭据错误或权限不足；按凭据手册核对 `.env`，不要反复猜密码。
- `PKIX path building failed`：证书链不受信任；检查系统根证书或附加 CA，不允许关闭 TLS 校验。
- `No space left on device`：Docker Desktop 虚拟磁盘或宿主磁盘不足；先备份，再清理不用的构建缓存，不能删除正在使用的数据卷。
- `manifest unknown` / `TLS handshake timeout`：镜像标签或 Docker Hub 网络问题；检查 Docker Desktop Proxy 和 7890 代理。
- Prometheus Target `DOWN`：打开 Target 的 last error，再检查 app 是否暴露 `/actuator/prometheus`。
- n8n Webhook 404：工作流未发布或未 Active；进入 n8n 编辑器激活后重试。
- Ollama 模型不存在：执行 `docker compose exec ollama ollama pull deepseek-r1:14b`，下载完成后 `ollama list` 验证。

## 安全重启与升级

单服务重启优先使用：

```powershell
docker compose restart app
docker compose up -d --force-recreate grafana
```

不要用 Docker Desktop 的“Delete volume”处理普通故障。升级前创建应用备份并记录 `docker compose images`；升级后依次检查 Flyway、health、Provider 和关键业务请求。
