# 备份与恢复

## 创建备份

在“调度与运维”页面点击“创建备份”，或请求 `POST /api/v1/operations/backups`。

备份文件写入数据根目录下的 `backups/`，ZIP 内包含：

- H2 在线备份包 `database/h2-backup.zip`
- `article-content/` 清洗正文
- `snapshots/` 网页快照
- `exports/` 导出文件
- `manifest.txt` 备份清单

## 恢复原则

恢复会替换持久化数据，必须停机进行。Stage 9 不提供在线恢复 REST 接口，避免误操作覆盖正在使用的数据库。

建议流程：

1. 停止 Knowledge Collector，并确认 Java 进程已经释放 H2 文件。
2. 对当前数据目录再做一份只读副本。
3. 解压目标业务备份 ZIP。
4. 使用 H2 官方 `Restore` 工具把 `database/h2-backup.zip` 恢复到数据目录的 `database/`。
5. 将正文、快照和导出目录恢复到对应位置。
6. 启动应用，确认 Flyway 版本、系统状态、文章数量和最近任务。

Stage 10 将补充独立恢复脚本和完整的恢复演练验证。
