# 部署指南

## 环境

- JDK 17 或更高版本
- Windows、Linux 或 macOS
- 建议至少 1GB 可用内存和独立数据目录

## 构建与启动

```bash
./mvnw clean verify
java -jar knowledge-collector-boot/target/knowledge-collector.jar
```

Windows 可运行 `start.bat`，Linux/macOS 可运行 `start.sh`。默认访问 `http://127.0.0.1:8080`。

## 配置

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `KNOWLEDGE_COLLECTOR_DATA_DIR` | `./data` | 数据根目录 |
| `KNOWLEDGE_COLLECTOR_SERVER_PORT` | `8080` | 端口 |
| `KNOWLEDGE_COLLECTOR_SERVER_ADDRESS` | `127.0.0.1` | 监听地址 |
| `KNOWLEDGE_COLLECTOR_TASK_STALE_TIMEOUT` | `PT10M` | 任务最大无心跳时间 |
| `KNOWLEDGE_COLLECTOR_TRUST_SYSTEM_STORE` | `true` | Windows 合并系统根证书 |
| `KNOWLEDGE_COLLECTOR_ADDITIONAL_CA_FILE` | 空 | 附加 PEM CA 文件 |

## 升级

1. 在 `/operations` 创建备份。
2. 停止应用并保留旧 JAR。
3. 替换 JAR 后启动。
4. 检查 `/actuator/health` 和 `/api/v1/system/status`。
5. Flyway 自动升级，不要手工修改业务表。

V8 升级时会把历史遗留的 `CREATED/RUNNING` 任务标记为 `TASK-INTERRUPTED` 并释放采集源。

## 回滚

数据库迁移不支持直接降级。需要停止应用，恢复升级前备份，再换回旧 JAR。不要让旧版本直接打开已经升级到更高 Flyway 版本的数据库。

## 故障排查

- 端口占用：修改 `KNOWLEDGE_COLLECTOR_SERVER_PORT`。
- 同源任务一直运行：等待自动回收，或在运维页点击“回收超时任务”。
- PKIX：更新 JDK；Windows 保持系统证书合并；必要时配置 PEM CA。
- 数据库被占用：确认没有另一个实例使用同一数据目录。
- 日志：查看数据目录下 `logs/`。
