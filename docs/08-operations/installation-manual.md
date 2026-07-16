# 安装手册

## Windows

1. 安装 64 位 JDK 17。
2. 在命令行执行 `java -version`。
3. 将发布目录放到有写权限的位置。
4. 双击或在终端运行 `start.bat`。
5. 浏览器打开 `http://127.0.0.1:8080`。

Windows 默认会同时使用系统根证书库，适合浏览器已经信任但旧 JDK 尚未包含的新根证书。

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
