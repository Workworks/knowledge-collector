# 安装手册

## Windows

1. 安装 64 位 JDK 17。
2. 在命令行执行 `java -version`。
3. 将发布目录放到有写权限的位置。
4. 双击或在终端运行 `start.bat`。
5. 浏览器打开 `http://127.0.0.1:8080`。

Windows 默认会同时使用系统根证书库，适合浏览器已经信任但旧 JDK 尚未包含的新根证书。

### Docker Desktop 使用 7890 代理

以 Clash HTTP 代理 `127.0.0.1:7890` 为例：

```powershell
Get-NetTCPConnection -LocalPort 7890 -State Listen
curl.exe -sS -o NUL -w "status=%{http_code} total=%{time_total}`n" `
  -x http://127.0.0.1:7890 --connect-timeout 5 --max-time 10 `
  https://auth.docker.io/token
```

预期 `status=200`。打开 Docker Desktop → Settings → Resources → Proxies，选择 Manual proxy configuration，HTTP Proxy 和 HTTPS Proxy 都填写 `http://127.0.0.1:7890`，点击 Apply & restart。设置会保存在 `%APPDATA%\Docker\settings-store.json` 的 `OverrideProxyHTTP`、`OverrideProxyHTTPS` 和 `ProxyHTTPMode=manual`。

```powershell
docker pull hello-world:latest
docker pull ollama/ollama:latest
docker compose up -d ollama
docker compose exec ollama ollama pull deepseek-r1:14b
docker compose exec ollama ollama list
docker compose up -d app ollama-init
```

`ollama list` 应显示 `deepseek-r1:14b`，模型保存在 `worktwo_ollama_models` 命名卷，应用容器使用 `http://ollama:11434`。下载中断后重新执行 pull 会复用分片。只有复用 Windows 宿主机 Ollama 时才加载 `compose.windows-host-ollama.yaml`；使用容器 Ollama 时只加载 `compose.yaml`。

## Linux/macOS

```bash
chmod +x start.sh
./start.sh
```

## 数据位置

默认使用程序目录下 `data/`。如需独立保存：

```text
KNOWLEDGE_COLLECTOR_DATA_DIR=D:\knowledge-collector-data
```

## 升级与卸载

- 升级前创建备份，停止旧程序后替换 JAR。
- 卸载程序时可保留数据目录，重新安装后继续使用。
- 删除数据目录会永久删除数据库、正文、备份和日志。
