# 测试用例

| 编号 | 场景 | 预期结果 |
| --- | --- | --- |
| TC-001 | 启动空数据库 | Flyway 执行 V1—V8，健康状态为 UP |
| TC-002 | 创建、修改、删除主题 | 校验、冲突和关联保护正确 |
| TC-003 | 创建采集员并关联主题 | 分页查询和启停正确 |
| TC-004 | RSS/Atom 测试与采集 | 解析条目、创建任务和文章 |
| TC-005 | 重复执行同一 Feed | 不重复建文，记录 DUPLICATE |
| TC-006 | HTML 规则预览与采集 | CSS Selector 提取、清洗和入库 |
| TC-007 | 慢响应超过来源超时 | 任务变为 FAILED，错误为 HTTP-REQUEST-TIMEOUT |
| TC-008 | 超时后再次采集同一来源 | 可创建新任务，不再被旧 RUNNING 锁住 |
| TC-009 | 模拟无心跳 RUNNING 任务 | 自动/手动回收为 TASK-TIMEOUT 并释放来源 |
| TC-010 | 失败任务重试 | 新任务记录 RETRY 和 retryOfTaskId |
| TC-011 | 待执行任务取消 | 状态变为 CANCELED，释放来源 |
| TC-012 | 资料组合搜索 | 主题、来源、标签、状态和质量条件生效 |
| TC-013 | 阅读操作 | 已读、收藏、归档、标签和笔记持久化 |
| TC-014 | TLS 默认信任 | 使用 JDK CA，并在 Windows 合并系统根证书 |
| TC-015 | 配置附加 PEM CA | CA 文件成功加载，仍保留主机名与证书链校验 |
| TC-016 | PKIX 错误 | 返回 TLS-CERTIFICATE-UNTRUSTED 和修复提示 |
| TC-017 | 创建备份 | ZIP 包含 H2、正文、快照、导出和清单 |
| TC-018 | 恢复备份 | H2 Restore 成功，恢复库可查询文章表 |
| TC-019 | favicon | `/favicon.ico` 返回 200 |
| TC-020 | JAR 冒烟 | 首页、运维页、仪表盘和健康接口正常 |
