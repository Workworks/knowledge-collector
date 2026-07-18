# Docker Compose 部署

CPU 模式运行 `.\scripts\package-docker.ps1` 或 `./scripts/package-docker.sh`。脚本构建应用镜像，启动 Ollama 与 SearXNG，并下载默认 `deepseek-r1:14b` 模型。

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
docker compose down
```

数据、模型和 SearXNG 配置分别保存在 `knowledge_data`、`ollama_models`、`searxng_data` 命名卷。只有显式执行 `docker compose down -v` 才会删除它们。
