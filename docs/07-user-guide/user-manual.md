# Knowledge Collector 完整系统用户手册

本手册以一条可复现的“人工智能治理资料”流程为例，覆盖系统当前全部页面。示例值可以直接照填；每节末尾给出验收方法。系统默认地址为 `http://127.0.0.1:8080`。

## 1. 启动与首次检查

1. 启动 Ollama，并确认已经安装模型：`deepseek-r1:14b`。
2. 启动 Knowledge Collector，浏览器打开首页。
3. 首页“基础设施状态”应显示数据库、Flyway、存储目录和 Java 运行时。
4. 打开“第三方能力”，检查是否同时显示 `ollama` 和 `searxng`。

验收：访问“接口测试”，选择“系统状态”并发送，HTTP 状态应为 200，`success` 为 `true`，Flyway 迁移数为 13。

## 2. 配置第三方能力

### 2.1 Ollama

进入“第三方能力 → Ollama 本地大模型”，按下表填写。

| 字段 | 示例 | 说明 |
| --- | --- | --- |
| 服务地址 | `http://127.0.0.1:11434` | Ollama API 根地址，不要填写 `/api/tags` |
| 默认模型 | `deepseek-r1:14b` | 必须与 `ollama list` 中的名称一致 |
| 认证方式 | `NONE` | 本机 Ollama 通常无需认证 |
| 认证信息 | 留空 | 选择 `BEARER` 或 `API_KEY` 时再填写 |
| 启用 | 勾选 | 停用后文章分析和 AI 对话会明确报错 |
| 默认 Provider | 勾选 | 文章分析与对话未指定 Provider 时使用它 |
| 备用 Provider | 不勾选 | 后续增加其他模型服务时使用 |

点击“保存配置”，再点击“测试连接”和“查看可用模型”。

验收：卡片状态变为 `AVAILABLE`，结果中出现 `deepseek-r1:14b`；下方调用记录新增 `TEST_CONNECTION / SUCCESS`。

### 2.2 SearXNG

本机独立部署示例填写：

| 字段 | 示例 |
| --- | --- |
| 服务地址 | `http://127.0.0.1:8088` |
| 默认模型 | 留空 |
| 认证方式 | `NONE` |
| 认证信息 | 留空 |
| 启用 | 勾选 |
| 默认 Provider | 勾选 |
| 备用 Provider | 不勾选 |

Docker Compose 部署时，应用容器使用 `http://searxng:8080`，无需改容器内地址。点击“保存配置”和“测试连接”。

验收：状态为 `AVAILABLE`，调用记录显示 `searxng / TEST_CONNECTION / SUCCESS`。若失败，失败原因会显示在服务卡片和调用记录中；点击“重试检测”可重新执行。

## 3. 创建研究主题

进入“主题 → 新建主题”，填写：

| 字段 | 可验证示例 |
| --- | --- |
| 编码 | `AI_GOVERNANCE` |
| 名称 | `人工智能治理` |
| 颜色 | `#6D4AFF` |
| 图标名称 | `shield` |
| 默认语言 | `zh-CN` |
| 排序 | `10` |
| 描述 | `跟踪人工智能监管、标准、风险与产业治理。` |
| 关键词 | `人工智能治理, AI监管, 大模型安全, 算法治理` |
| 排除关键词 | `培训广告, 招聘, 优惠券` |
| 启用主题 | 勾选 |

关键词支持逗号或换行分隔；排除词优先于普通关键词。点击“保存主题”。

验收：主题列表出现 `AI_GOVERNANCE / 人工智能治理`，状态为启用；搜索“治理”可以查到该记录。

## 4. 使用 SearXNG 自动发现采集源

进入“采集员 → 自动发现采集源”，填写：

| 字段 | 示例 | 选择建议 |
| --- | --- | --- |
| 研究主题 | `人工智能治理` | 建议与已创建主题名称完全一致，导入时会自动关联 |
| 资料语言 | `中文` | 英文资料选择 `英文` |
| 期望数量 | `5` | 允许 1—50 |
| 来源类型 | `网页列表` | 已知需要订阅源时选 RSS/Atom |
| 质量等级 | `权威` | 优先政府、大学和研究机构 |

点击“发现采集源”。候选列表会显示名称、网站、采集地址、类型、语言、可靠性评分、验证状态和推荐原因。

后续操作：

1. 点击单行“验证”，或勾选多行后点击“批量验证”。
2. 只有状态为 `VERIFIED` 的候选可以导入。
3. 勾选已验证候选，点击“批量导入”；也可点击单行“导入”。
4. 不需要的结果点击“忽略”，它不会进入采集员列表。

验收：导入后候选状态为 `IMPORTED` 并显示采集源 ID；采集员列表出现新记录，健康状态为“正常 · 已验证”；第三方能力调用记录依次出现 `DISCOVER_SOURCES` 和 `VALIDATE_SOURCE`。

## 5. 手工创建采集源

当自动发现结果不适用时，点击“新建采集源”。以下示例使用 Spring 官方博客 Atom：

| 字段 | 示例 |
| --- | --- |
| 编码 | `SPRING_BLOG` |
| 名称 | `Spring 官方博客` |
| 类型 | `ATOM` |
| 所属主题 | 选择 `人工智能治理`（按 Ctrl 可多选） |
| 首页地址 | `https://spring.io/blog` |
| 订阅/API 地址 | `https://spring.io/blog.atom` |
| 语言 | `en` |
| 字符集 | `UTF-8` |
| 超时 | `15` 秒 |
| 最大重试 | `2` |
| 请求间隔 | `2000` 毫秒 |
| User-Agent | `KnowledgeCollector/1.0 (+local-admin)` |
| 备注 | `官方公开 Atom，只采集公开内容。` |
| 遵守 robots.txt | 勾选 |
| 允许采集正文 | 勾选 |
| 只保存摘要 | 不勾选 |
| 保存快照 | 按需要勾选 |
| 启用采集源 | 勾选 |

类型说明：`RSS/ATOM` 填订阅地址；`HTML_LIST` 填列表页并继续配置 CSS 规则；`JSON_API` 填公开 JSON 接口；`MANUAL_URL` 填单篇公开网页。

验收：点击该行“刷新”，状态应变为正常或明确显示 TLS、超时、robots 等失败原因；点击“立即采集”后跳转到任务详情。

## 6. 配置 HTML 列表规则

仅 `HTML_LIST` 类型需要此步骤。点击采集源行中的“规则”，示例填写：

| 字段 | 示例 |
| --- | --- |
| 列表项 | `article, .post, .item` |
| 详情链接 | `a[href]` |
| 标题 | `h1` |
| 正文 | `article, main, .content, .post-content` |
| 作者 | `.author` |
| 发布时间 | `time` |
| 时间格式 | `yyyy-MM-dd'T'HH:mm:ssXXX` |
| 正文内移除选择器 | `script`、`style`、`nav`、`footer`、`aside`，每行一个 |
| 保存后立即启用 | 勾选 |

先点击“测试当前规则”，确认能返回标题、地址和正文，再点击“保存新版本”。

验收：预览至少返回一条结果；规则版本列表出现新版本并标记启用。预览不会写入资料库。

## 7. 采集、任务查询与失败重试

在采集员列表点击“立即采集”。任务详情应显示发现、新增、重复、失败数量。

任务列表默认只显示最近 7 天，可填写：

| 字段 | 示例 |
| --- | --- |
| 搜索 | `Spring 官方博客` 或任务号 |
| 采集员 | `Spring 官方博客` |
| 状态 | `SUCCESS` |
| 触发方式 | `MANUAL_SOURCE` |
| 开始日期 | `2026-07-18` |
| 结束日期 | `2026-07-18` |

失败任务打开详情后点击“重试失败任务”。常见错误：`HTTP-REQUEST-TIMEOUT` 表示超时；`TLS-CERTIFICATE-UNTRUSTED` 表示证书链不受信任；`TASK-TIMEOUT` 表示任务失去心跳后被回收。

验收：成功任务状态为 `SUCCESS`，新增数大于 0；重复执行同一来源时重复数增加而不会重复建文。

## 8. 资料库检索和阅读

资料库筛选示例：全文搜索填 `人工智能`，主题选“人工智能治理”，阅读状态选“未读”，最低质量选 60，排序选“质量从高到低”。点击“应用筛选”。

打开文章后可执行：

- 点击“收藏”；按钮状态应立即变化。
- 点击“标记已读”“标记未读”“归档”或“忽略”。
- 自定义标签填写 `精读,政策,待核验`。
- 个人笔记填写 `核对原始政策文件发布日期，并与第二来源交叉验证。`。

验收：刷新页面后收藏、阅读状态、标签和笔记仍存在；正文保持显示，操作提示以右上角悬浮消息出现。

## 9. AI 分析与知识卡片

在文章详情点击“使用默认 AI 分析”。完成后应显示摘要、关键点、关键词、分类、阅读价值、模型、Token 和耗时。

沉淀知识卡片示例：

| 字段 | 示例 |
| --- | --- |
| 标题 | `风险分级治理原则` |
| 卡片类型 | `CONCEPT` |
| 内容 | `监管强度应与模型用途和潜在损害等级相匹配。` |
| 原文摘录 | 在正文选择对应段落后点击“使用选中文本” |

点击“保存到知识工作台”。

验收：AI 状态为“已完成”，刷新后分析仍存在；知识工作台“知识卡片”页签出现新卡片，并保留文章来源。

## 10. AI 研究助手与材料入库

进入“AI 助手”，点击“新建对话”，输入：

```text
请基于可验证事实，列出人工智能风险分级治理的研究提纲。对不确定内容明确标注待核验。
```

发送后可继续追问。对有价值的助手回复点击“保存到资料库”，标题填写 `AI整理：人工智能风险分级治理提纲`。

验收：资料库出现该材料，来源类型标记为 `AI_GENERATED`，审核状态为 `PENDING_REVIEW`；第三方能力调用记录出现 `AI_CHAT`。

## 11. 归档库和整理规则

文章详情点击“归档”后，在“归档库”可查看。新增规则示例：

| 字段 | 示例 |
| --- | --- |
| 规则名称 | `高质量人工智能治理资料` |
| 关键词 | `人工智能` |
| 主题 | `人工智能治理` |
| 来源 | 不限 |
| 最低质量 | `70` |
| 顺序 | `10` |
| 启用规则 | 勾选 |

验收：点击规则后仅显示同时满足条件的归档文章。

## 12. 知识工作台完整示例

各页签均可使用下列示例，保存后记录会显示 ID，后续关联操作使用该 ID。

| 页签 | 必填/主要字段示例 | 验收 |
| --- | --- | --- |
| 知识卡片 | 标题 `风险分级`；类型 `CONCEPT`；内容 `按风险等级配置治理措施`；置信度 `80` | 卡片列表出现记录 |
| 观点证据 | 观点 `高风险用途需要更严格审查`；状态 `PENDING_VERIFICATION`；证据填写文章 ID、摘录和强度 `80` | 观点下可追溯证据 |
| 实体概念 | 标准名称 `欧盟人工智能法案`；类型 `POLICY`；别名 `EU AI Act`；简介填写政策说明 | 实体列表可检索；别名实体可按 ID 合并 |
| 事件聚合 | 标题 `人工智能法案生效`；日期 `2026-08-01`；聚类键 `EU_AI_ACT`；摘要填写事件事实 | 使用事件 ID 和文章 ID 关联，来源角色选 `OFFICIAL` |
| 专题知识页 | 名称 `全球人工智能监管`；简介、知识体系摘要、未解决问题均填写 | 专题列表出现记录 |
| 研究项目 | 标题 `风险分级治理比较`；目标 `比较主要司法辖区规则`；核心问题和待验证假设填写完整 | 用项目 ID、卡片 ID 和用途 `CORE_EVIDENCE` 加入资料 |
| 综合归纳 | 标题 `风险分级治理研究简报`；类型 `RESEARCH_BRIEF`；结论填写；来源引用 `article:1, card:1` | 无来源引用时系统拒绝保存 |
| 写作草稿 | 标题 `风险分级治理的实践路径`；项目 ID；提纲；正文；来源引用 `card:1, article:1` | 草稿列表出现记录 |
| 知识缺口 | 类型 `MISSING_COUNTERPOINT`；描述 `缺少反对风险分级方法的研究`；到期时间选择明日 | 待办列表出现记录 |
| 到期复习 | 打开到期卡片，完成复习 | 复习次数增加并生成下一次日期 |

## 13. 运维、调度和备份

进入“运维”：

1. 采集员调度中启用目标来源，周期示例填写 `3600` 秒，表示每小时检查一次。
2. 点击“立即检查到期任务”可手动触发本轮调度。
3. 点击“回收超时任务”释放长期无心跳的任务锁。
4. 点击“创建备份”，等待列表出现 `SUCCESS`。

验收：调度触发的任务类型为 `SCHEDULED`；备份文件位于数据目录 `backups/`，记录显示文件大小。恢复必须停机并按《备份与恢复》执行。

## 14. 接口测试页面

进入“接口测试”，示例：方法选 `GET`，路径填 `/api/v1/capabilities`，关联编号填 `manual-stage27-check`，请求体留空，点击“发送请求”。

验收：HTTP 200；响应 `success=true`；响应头包含 `X-Correlation-Id`。POST/PUT 请求必须把请求体填写为合法 JSON。

## 15. 完整闭环验收清单

按顺序确认：主题已创建；Ollama 与 SearXNG 连接成功；搜索得到候选；候选验证并导入；采集任务成功；文章正文可读；AI 分析已保存；知识卡片已创建；文章已归档；研究项目引用了卡片；第三方调用日志可查看成功或失败原因。以上全部成立，即完成一次“配置—发现—采集—分析—沉淀—研究—归档”的可追溯闭环。

## 16. Stage 28：Firecrawl 与 Playwright 网页提取

### 16.1 配置和连接验证

进入“第三方能力”，分别配置以下服务。凭据保存后不会明文回显。

| 服务 | 服务地址示例 | 模型/存储桶 | 认证方式 | 凭据示例 | 选项 |
| --- | --- | --- | --- | --- | --- |
| Firecrawl | 自托管 `http://127.0.0.1:3002`；云端 `https://api.firecrawl.dev` | 留空 | `BEARER` | `fc-你的真实API密钥`；自托管未启用认证时留空 | 启用；设为 EXTRACTION 默认 |
| Playwright | Docker Compose 内部由应用自动使用 `http://playwright:3003`；本机独立运行填 `http://127.0.0.1:3003` | 留空 | `NONE` | 留空 | 启用；可设为备用 |

逐项点击“保存配置”，再点击“测试连接”。Firecrawl 返回“服务可访问”，Playwright 返回“浏览器服务连接成功”即通过。若 Firecrawl 首页返回 401/404 但抓取接口可用，可直接按下一步发起一次示例提取，以业务调用结果为最终验证。

### 16.2 从文章详情重新提取

1. 打开资料库中的文章，例如 `/articles/550`。
2. 在右侧“正文与原始证据”点击“重新提取正文”。系统自动填写文章 ID `550` 和该文章原始 URL。
3. “网页 URL”示例填写 `https://spring.io/blog/2025/05/20/your-article`；实际验收请使用当前文章的公开原文地址。
4. 普通静态网页先选“直接重新提取正文”；正文由 JavaScript 动态加载时选 `Playwright 浏览器`；页面结构复杂但无需浏览器交互时选 `Firecrawl`。
5. 点击“开始提取”。

验收：结果区状态为 `SUCCESS`，并显示实际方式、标题、作者、发布时间、正文长度和耗时；返回文章详情并刷新，正文和新标题仍存在。提取记录中的“原始 HTML”“抓取截图”可直接打开。

### 16.3 手动 URL、采集源和失败任务入口

- 手动 URL：直接打开“网页提取”，文章 ID 留空，URL 填 `https://example.org/public-article`，方式选 `FIRECRAWL`。成功后结果保留在任务列表，但不会覆盖任何文章。
- 采集源：在“采集员”目标行点击“测试抓取”，系统把订阅/API 地址带入网页提取工作台；RSS 地址通常用直接方式，动态列表页用 Playwright。
- 失败任务：进入任务详情；未生成文章的任务项会显示“更换方式抓取”。点击后选 Firecrawl 或 Playwright。
- 失败重试：在最近提取任务或结果区点击“重新执行”。新记录会显示“重试 #原任务号”，原失败记录不会被覆盖。

可验证失败示例：URL 填 `https://127.0.0.1:1/unreachable` 并选“直接重新提取”，结果应为 `FAILED` 且显示连接或 URL 安全错误；修正为真实公开 URL 后点击“重新执行”，新记录应为 `SUCCESS`。

## 17. Stage 29：MinIO 文件与快照

### 17.1 配置 MinIO

Docker Compose 会创建 `knowledge-collector` 存储桶。本机独立 MinIO 的第三方能力填写：

| 字段 | 可验证示例 |
| --- | --- |
| 服务地址 | `http://127.0.0.1:9000` |
| 模型/存储桶 | `knowledge-collector` |
| 认证方式 | `ACCESS_SECRET` |
| 凭据 | `knowledge:collector-secret`，格式必须为 `accessKey:secretKey` |
| 启用 | 勾选 |
| 默认 Provider | 勾选 |

保存后点击“测试连接”。显示“MinIO 连接成功，存储桶可用”即通过。MinIO 控制台默认在 `http://127.0.0.1:9001`，Compose 登录示例为用户 `knowledge`、密码 `collector-secret`；正式部署必须在 `.env` 修改密码。

### 17.2 查看文章原始证据

打开文章详情，点击“查看原始网页、快照与版本”。归属类型和归属 ID 会自动填写，例如 `ARTICLE`、`550`。由成功抓取生成的记录包括：

- `RAW_HTML`：抓取时的原始网页，可下载并用浏览器离线打开；
- `SCREENSHOT`：Playwright 全页 PNG 快照；
- `ATTACHMENT`：原始附件；
- `SUPPLEMENT`：人工补充材料。

验收：点击“下载原始文件”得到非空文件；列表显示文件名、MIME 类型、大小、SHA-256 摘要、版本和创建时间。

### 17.3 上传补充材料和验证版本

在“文件与快照”填写：

| 字段 | 第一次上传示例 | 第二次上传示例 |
| --- | --- | --- |
| 归属类型 | `ARTICLE` | `ARTICLE` |
| 归属 ID | `550` | `550` |
| 文件类型 | `补充材料` | `补充材料` |
| 文件 | `policy-note-v1.txt`，内容 `2026-07-18 首次核验记录` | `policy-note-v2.txt`，内容 `2026-07-19 已与官方公告交叉验证` |

每次点击“上传到 MinIO”。验收：两条记录都保留；同属 `ARTICLE #550` 的 `SUPPLEMENT` 分别显示 `v1`、`v2`；下载后内容与上传文件一致。知识卡片、观点证据和写作草稿使用归属类型 `CARD`、`CLAIM`、`DRAFT`，归属 ID 填对应页面显示的记录 ID。

### 17.4 故障验证

将 MinIO 临时停用后上传示例文件，页面应明确提示 `MINIO-DISABLED`，不会生成空的文件记录。重新启用、测试连接成功后再次上传即可。第三方能力页的调用记录应出现 `SAVE_OBJECT` 或 `READ_OBJECT`，失败时保留具体原因并可从能力页检查配置。

## 18. Windows Docker Desktop 从零完整部署（Stage 38）

本章是一篇可独立使用的部署说明：从空白 Windows 环境完成 Web 打包、`.env` 创建、全栈容器启动和验收。命令均在 PowerShell 执行。

### 18.1 软件、仓库与环境检查

安装 Docker Desktop（启用 WSL 2）、Git、JDK 17。等待 Docker Desktop 显示 Engine running：

```powershell
docker version
docker compose version
git --version
java -version
Set-Location $HOME\Desktop
New-Item -ItemType Directory -Force aiWork | Out-Null
Set-Location aiWork
git clone git@github.com:Workworks/knowledge-collector.git WorkTwo
Set-Location WorkTwo
git status --short
```

Docker 应同时返回 Client/Server，Java 第一行含 `17`，最后一条无输出。HTTPS 克隆地址为 `https://github.com/Workworks/knowledge-collector.git`。

### 18.2 创建 `.env`，逐项填写

```powershell
Copy-Item .env.example .env
notepad .env
```

可直接验证的本机示例：

```dotenv
KNOWLEDGE_COLLECTOR_PORT=127.0.0.1:8080
KNOWLEDGE_COLLECTOR_DATA_DIR=./data
KNOWLEDGE_COLLECTOR_SERVER_ADDRESS=127.0.0.1
KNOWLEDGE_COLLECTOR_SERVER_PORT=8080
KNOWLEDGE_COLLECTOR_TASK_STALE_TIMEOUT=PT10M
KNOWLEDGE_COLLECTOR_AI_PROVIDER=ollama
KNOWLEDGE_COLLECTOR_OLLAMA_ENABLED=true
KNOWLEDGE_COLLECTOR_OLLAMA_BASE_URL=http://127.0.0.1:11434
KNOWLEDGE_COLLECTOR_OLLAMA_MODEL=deepseek-r1:14b
OLLAMA_MODEL=deepseek-r1:14b
MINIO_ROOT_USER=knowledge-admin
MINIO_ROOT_PASSWORD=KcMinio_2026_Local_9pV4xQ7m
MINIO_CONSOLE_PORT=127.0.0.1:9001
TIKA_PORT=127.0.0.1:9998
QDRANT_PORT=127.0.0.1:6333
NTFY_PORT=127.0.0.1:8085
PROMETHEUS_PORT=127.0.0.1:9090
GRAFANA_PORT=127.0.0.1:3000
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=KcGrafana_2026_Local_7mQ2vR8p
N8N_PORT=127.0.0.1:5678
N8N_ENCRYPTION_KEY=KcN8n_2026_Local_32chars_4pR8mQ2v
N8N_USER=knowledge-admin
N8N_PASSWORD=KcN8n_Login_2026_8vQ3mP6r
# 无云账号时保持空白，默认使用 Ollama
KNOWLEDGE_COLLECTOR_CLOUD_LLM_ENDPOINT=
KNOWLEDGE_COLLECTOR_CLOUD_LLM_API_KEY=
KNOWLEDGE_COLLECTOR_LANGFUSE_URL=
LANGFUSE_PUBLIC_KEY=
LANGFUSE_SECRET_KEY=
```

示例密码只用于本机，实际部署必须更换。验证密钥文件不会进 Git：`git check-ignore .env`，预期输出 `.env`。禁止把真实值写进 `.env.example`。

### 18.3 Ollama 方式与 Web 打包

Windows 已安装 Ollama 时推荐复用宿主服务，以免容器再次下载 9 GB 模型：

```powershell
ollama list
Invoke-RestMethod http://127.0.0.1:11434/api/tags
.\mvnw.cmd clean package
Get-Item .\knowledge-collector-boot\target\knowledge-collector.jar
```

能看到 `deepseek-r1:14b` 且 JAR 存在即通过。正式部署前必须至少执行一次不带 `-DskipTests` 的构建。

### 18.4 拉取、启动全栈并验证

复用 Windows Ollama：

```powershell
docker compose -f compose.yaml -f compose.windows-host-ollama.yaml pull
docker compose -f compose.yaml -f compose.windows-host-ollama.yaml up -d --build
docker compose -f compose.yaml -f compose.windows-host-ollama.yaml ps
```

若 Ollama 也要进容器，改为 `docker compose up -d --build`。正常服务包括 app、searxng、playwright、minio、tika、qdrant、ntfy、prometheus、grafana、n8n；`minio-init` 显示 `Exited (0)` 是成功。普通 `down` 保留命名卷；不要在未备份时执行 `down -v`。

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health
Invoke-WebRequest http://127.0.0.1:8080/advanced -UseBasicParsing
Invoke-RestMethod http://127.0.0.1:6333/collections
Invoke-WebRequest http://127.0.0.1:9090/-/ready -UseBasicParsing
Invoke-WebRequest http://127.0.0.1:3000/api/health -UseBasicParsing
Invoke-WebRequest http://127.0.0.1:8085/v1/health -UseBasicParsing
```

预期：应用 `UP`，页面 200，Qdrant 返回 collections，Prometheus ready，Grafana database ok。入口为系统 `:8080`、MinIO `:9001`、Grafana `:3000`、ntfy `:8085/knowledge-alerts`、n8n `:5678`。

### 18.5 Docker Hub 超时绕行（实机记录）

2026-07-19 实机验收中，`auth.docker.io:443` 曾持续超时，标准构建 15 分钟无进度。此时停止等待，复用 Windows Ollama，并用已缓存的 Playwright Ubuntu 镜像构建 Java 运行层：

```powershell
docker image inspect worktwo-playwright:latest
.\mvnw.cmd -DskipTests package
docker compose -f compose.yaml -f compose.windows-host-ollama.yaml -f compose.windows-cached.yaml build app
docker compose -f compose.yaml -f compose.windows-host-ollama.yaml -f compose.windows-cached.yaml up -d
```

绕行 Dockerfile 为 `deploy/windows/Dockerfile.cached-runtime`。若 `apt-get` 也无法联网，应恢复网络或配置 Docker Desktop 代理，不要无限等待；已有容器可继续服务，待镜像就绪再更新 app。

### 18.6 n8n 首次导入

打开 `http://127.0.0.1:5678`，用 `.env` 的 N8N 账号登录，选择 Import from File，导入 `deploy/n8n/knowledge-collector-workflow.json`，再点击 Active。Windows 调试 URL 是 `http://127.0.0.1:5678/webhook/knowledge-collector`；系统容器中填写 `http://n8n:5678/webhook/knowledge-collector`。

## 19. Stage 30：文档导入

进入“增强能力 → 30 文档导入”。创建 `stage30-example.txt`，内容填写 `Knowledge Collector Stage 30 验证材料。结论：Tika 已能提取本文并创建资料库文章。`；选 AUTO、勾选创建文章并上传。通过标准：`SUCCESS`、method 为 TIKA、text 含 Stage 30、articleId 大于 0，资料库出现 `stage30-example`。图片应选 OCR，结果会显示置信度。

## 20. Stage 31：Qdrant 向量索引

进入“31 向量索引”点击批量生成/重建。通过标准：collection=`knowledge`、model=`LOCAL_HASH_64`、status=`READY`、indexed 大于 0。执行 `Invoke-RestMethod http://127.0.0.1:6333/collections/knowledge`，返回 status=green 证明真实 Qdrant 可用。

## 21. Stage 32：混合检索与重排

先完成 Stage 31。查询填 `Knowledge Collector Stage 30`，方式 HYBRID，召回 10，最终 5，可信度 0.2，新鲜度 0.2，启用重排。结果应含 keyword、semantic、fused、reranked 和 finalScore。切到 KEYWORD 再执行可直观看到重排前后变化。

## 22. Stage 33：Crossref / PubMed 学术资料

关键词填 `knowledge management`，日期 `2024-01-01`，其余留空；结果 source=Crossref 且 items 非空。DOI 精确示例为 `10.1038/s41586-020-2649-2`。导入时标题填返回标题、DOI 填原值、摘要可填 `等待补充全文`；返回 articleId 后即可继续文章分析、卡片和研究项目。

## 23. Stage 34—35：云模型与 AI 调用

无云账号继续使用 Ollama。验证云接口时，地址填供应商完整 Chat Completions URL（示例 `https://api.example.com/v1/chat/completions`），模型填供应商模型 ID，Key 填临时密钥，任务选 ARTICLE_ANALYSIS，内容填 `请把这段公开材料整理为三条结论：……`。确认发送后，成功结果含 output、tokens、duration、traceId。进入 Stage 35 刷新记录；requestJson 不得含 API Key。配置 Langfuse 后用 traceId 对照云端；未配置时本地审计仍完整。

## 24. Stage 36：Prometheus / Grafana

点击刷新指标，应看到 articles、cards、tasks、failedTasks 和 Ollama、Tika、Qdrant、MinIO 状态。用 `.env` 的 Grafana 账号登录 `http://127.0.0.1:3000`，打开 Knowledge Collector 面板。Prometheus 验证 URL：`http://127.0.0.1:9090/graph?g0.expr=process_uptime_seconds`，出现时间序列即通过。

## 25. Stage 37：ntfy 通知

浏览器或手机订阅 `http://127.0.0.1:8085/knowledge-alerts`。页面填写地址 `http://ntfy:80`、主题 `knowledge-alerts`、标题 `Knowledge Collector 测试`、消息 `Stage 37 通知链路验证通过`、场景 TASK_FAILED、免打扰 `22:00-08:00`。返回 delivered=true 且订阅端收到同文即通过。

## 26. Stage 38：n8n 工作流

先激活示例工作流。Webhook 填 `http://n8n:5678/webhook/knowledge-collector`，类型 ARTICLE，业务 ID 填实际文章 ID（示例 1），摘要填 `Stage 38 可验证工作流示例`。返回 accepted=true，n8n Executions 有记录即通过。临时改为 `/webhook/not-found` 可验证失败提示和重试按钮。

## 27. IDEA HTTP Client 与运维

打开 `http/stage-30-38-advanced.http`；首行已有 `@baseUrl = http://127.0.0.1:8080`，不会再出现未替换变量。依次运行 Stage 31—38 请求，均应 HTTP 200、success=true。升级、停止和日志：

```powershell
git pull --ff-only
.\mvnw.cmd clean package
docker compose -f compose.yaml -f compose.windows-host-ollama.yaml up -d --build
docker compose -f compose.yaml -f compose.windows-host-ollama.yaml down
docker compose -f compose.yaml -f compose.windows-host-ollama.yaml logs --tail 200 app qdrant tika n8n prometheus grafana
```

n8n 404 表示工作流未激活；Tika 检查 9998；语义检索先重建索引并检查 6333；通知必须订阅完全相同主题；云模型 401 表示 endpoint、模型或 Key 不匹配。所有密码只放 `.env`，不得提交。
