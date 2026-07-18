# Docker Compose 部署

CPU 模式运行 `.\scripts\package-docker.ps1` 或 `./scripts/package-docker.sh`。脚本构建应用镜像，启动 Ollama、SearXNG、Playwright 和 MinIO，并下载默认 `deepseek-r1:14b` 模型。`minio-init` 会创建 `knowledge-collector` 存储桶。

NVIDIA GPU 模式：

```powershell
.\scripts\package-docker.ps1 -Gpu
```

```bash
./scripts/package-docker.sh --gpu
```

GPU 模式要求 Docker 主机已安装 NVIDIA Container Toolkit。更换模型可使用 `-Model "qwen3:8b"` 或设置 `OLLAMA_MODEL`。

常用命令：

```bash
docker compose ps
docker compose logs -f app
docker compose logs -f ollama
docker compose logs -f searxng
docker compose logs -f playwright
docker compose logs -f minio
docker compose down
```

数据、模型、SearXNG 配置和证据对象分别保存在 `knowledge_data`、`ollama_models`、`searxng_data`、`minio_data` 命名卷。只有显式执行 `docker compose down -v` 才会删除它们。

首次部署前在 `.env` 至少修改：

```dotenv
MINIO_ROOT_USER=knowledge-admin
MINIO_ROOT_PASSWORD=使用至少16位的独立强密码
```

MinIO 控制台为 `http://127.0.0.1:9001`。Firecrawl 未强行内嵌到本 Compose，因为其官方自托管栈还需要 Redis 等配套服务；可以单独按 Firecrawl 官方自托管 Compose 启动后，在第三方能力填写 `http://宿主机:3002`，或填写云端 API 地址和 Key。
