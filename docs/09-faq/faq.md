# 常见问题

## 同一采集源已有运行中任务，后续无法采集

升级到 Stage 10 后，任务使用心跳租约。默认 10 分钟无心跳会自动变为 `TASK-TIMEOUT` 并释放来源。也可以进入“运维”点击“回收超时任务”。应用升级时，V8 会自动清理历史遗留的 `RUNNING/CREATED` 任务。

## 为什么请求超时后以前仍显示 RUNNING

旧实现先取得响应流再读取正文，正文读取可能长时间阻塞。新实现使用受总请求超时控制的限长响应订阅器，超时后任务会正常进入 `FAILED`。

## RSS 出现 PKIX path building failed

这表示证书链不受当前 Java 信任，不是 RSS 格式错误。

1. 优先升级到最新 JDK 17 补丁版本。
2. Windows 保持 `KNOWLEDGE_COLLECTOR_TRUST_SYSTEM_STORE=true`。
3. 企业代理或私有 CA 环境中，将可信 CA 导出为 PEM，并配置 `KNOWLEDGE_COLLECTOR_ADDITIONAL_CA_FILE`。

系统不会提供“忽略 SSL”开关，因为这会允许中间人攻击。

## RSS 解析失败

确认地址返回 XML/RSS/Atom，而不是 HTML 登录页、403 页面或动态网页。

## 网站返回 403/429

检查 User-Agent、访问频率和站点规则。不要通过代理池或伪造身份绕过限制。

## 页面乱码

检查来源字符集和响应 Content-Type。RSS 通常应声明 XML 编码。

## 文章重复

系统按规范化 URL 哈希去重。重复采集会增加任务的重复计数，不会重复建文。

## 定时任务没有执行

确认采集员和调度均启用、下次运行时间已到，并查看是否存在有效运行任务。

## 如何备份和恢复

运维页创建备份；恢复必须停止应用，参照 `docs/06-deployment/backup-and-restore.md`。

## 动态网页无法采集

第一版只支持静态 HTML，不执行页面 JavaScript，也不绕过登录和验证码。
