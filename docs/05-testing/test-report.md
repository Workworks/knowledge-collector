# 测试报告

## 执行信息

- 日期：2026-07-17
- Java：17.0.8
- 命令：`./mvnw clean verify`
- 测试套件：12
- 测试：23
- 失败：0
- 错误：0
- 跳过：0

## 重点结果

- Flyway V1—V8 从空数据库迁移成功。
- RSS、Atom、HTML、去重、主题、质量、阅读管理和运维回归通过。
- 2.5 秒慢响应在来源 1 秒超时后转为 `HTTP-REQUEST-TIMEOUT`。
- 超时任务结束后，同一采集员可以立即创建下一任务。
- 20 分钟无心跳任务被回收为 `TASK-TIMEOUT`，来源锁正常释放。
- 失败重试、待执行取消和调度触发通过。
- Windows 系统根证书库和附加 PEM CA 加载通过，未关闭证书校验。
- 备份 ZIP 成功创建；H2 在线备份被恢复到新目录，恢复库可查询文章表。

## 发布包验证

- JAR：`knowledge-collector-boot/target/knowledge-collector.jar`
- 大小：64,933,076 字节
- 健康状态：UP
- Flyway：8
- 首页：HTTP 200
- 运维页：HTTP 200
- 超时任务回收接口：成功
- favicon：HTTP 200

## 结论

第一版本核心闭环和本次两个故障修复通过自动化及发布包验收。真实外部 RSS 仍受目标站点可用性、证书配置、robots 和访问策略影响。
