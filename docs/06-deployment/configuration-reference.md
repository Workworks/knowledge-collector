# 配置参考

配置入口为 `knowledge-collector-boot/src/main/resources/application.yml`。生产环境优先使用环境变量，不要修改并提交包含本机路径或密钥的配置文件。

## 服务与数据

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `KNOWLEDGE_COLLECTOR_SERVER_ADDRESS` | `127.0.0.1` | HTTP 监听地址；无鉴权版本不要绑定公网 |
| `KNOWLEDGE_COLLECTOR_SERVER_PORT` | `8080` | HTTP 端口 |
| `KNOWLEDGE_COLLECTOR_DATA_DIR` | `./data` | H2、正文、快照、导出和日志根目录 |
| `KNOWLEDGE_COLLECTOR_TASK_STALE_TIMEOUT` | `PT10M` | 无心跳任务回收时间，使用 ISO-8601 Duration |

## 网络与 TLS

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `KNOWLEDGE_COLLECTOR_TRUST_SYSTEM_STORE` | `true` | Windows 下合并系统根证书库 |
| `KNOWLEDGE_COLLECTOR_ADDITIONAL_CA_FILE` | 空 | 额外可信 PEM CA 文件绝对路径 |

系统始终执行 HTTPS 证书验证，不提供忽略 SSL 的配置。采集源的 User-Agent、超时、重试和请求间隔在采集员页面单独设置。

## AI 与 Ollama

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `KNOWLEDGE_COLLECTOR_AI_PROVIDER` | `ollama` | 默认内容理解/聊天 Provider ID |
| `KNOWLEDGE_COLLECTOR_AI_MAX_CONTENT_CHARS` | `24000` | 单篇分析最多发送字符数 |
| `KNOWLEDGE_COLLECTOR_AI_CHAT_MAX_HISTORY` | `20` | 对话携带的最近消息数 |
| `KNOWLEDGE_COLLECTOR_OLLAMA_ENABLED` | `true` | 是否启用 Ollama |
| `KNOWLEDGE_COLLECTOR_OLLAMA_BASE_URL` | `http://127.0.0.1:11434` | Ollama API 地址 |
| `KNOWLEDGE_COLLECTOR_OLLAMA_MODEL` | `deepseek-r1:14b` | 默认模型名称 |
| `KNOWLEDGE_COLLECTOR_OLLAMA_TIMEOUT` | `PT2M` | 单次请求超时 |

## Spring Profile

- 默认：本地正式数据、OpenAPI 可用、无演示数据。
- `local`：初始化演示主题/来源并启用 `/dev/tools`。
- `test`：内存数据库和测试网络限制，只用于自动化测试。
- `production`：关闭开发工具和 Swagger，减少健康详情。

## 配置示例

复制仓库根目录的 `.env.example` 为 `.env`，再按本机环境修改。`.env` 已被 Git 忽略。

Windows PowerShell：

```powershell
$env:KNOWLEDGE_COLLECTOR_DATA_DIR='D:\knowledge-collector-data'
$env:KNOWLEDGE_COLLECTOR_OLLAMA_MODEL='deepseek-r1:14b'
java -jar knowledge-collector-boot\target\knowledge-collector.jar
```

Linux/macOS：

```bash
export KNOWLEDGE_COLLECTOR_DATA_DIR=/srv/knowledge-collector/data
export KNOWLEDGE_COLLECTOR_OLLAMA_MODEL=deepseek-r1:14b
java -jar knowledge-collector-boot/target/knowledge-collector.jar
```

升级、备份和生产建议见[部署指南](deployment-guide.md)与[备份恢复](backup-and-restore.md)。
