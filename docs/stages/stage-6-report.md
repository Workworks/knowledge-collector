# Stage 6 阶段报告

## 阶段目标

完成 HTML 列表与详情采集、规则版本管理、正文清洗，并按 `WorkTwoIncOut.md` 建立可插拔能力模块，方便后续接入 arXiv、Crossref、PubMed、GDELT、AI、搜索、通知和对象存储。

## 已完成

- 新增 `knowledge-collector-capability-api`，定义数据源、HTTP、浏览器渲染、内容智能、向量、翻译、聚类、搜索、通知、对象存储和 URL 安全接口。
- 新增 `knowledge-collector-capability-provider`，承载 Rome、Jsoup、JDK HTTP 与 SSRF 防护实现。
- 现有 RSS/Atom 采集迁移到统一 `ContentSourceProvider`。
- Flyway V4 新增 `source_rule` 规则版本表，并扩展文章正文、纯文本、字数和阅读时长字段。
- HTML 规则支持列表项、链接、标题、正文、作者、发布时间、时间格式和移除选择器。
- 使用 Jsoup CSS Selector 发现详情页，使用 Safelist 清洗不可信 HTML，移除脚本、iframe、事件属性和危险协议。
- 新增规则页面 `/sources/{id}/rules` 与 `/sources/{id}/test`。
- 新增规则 REST API、IDEA HTTP Client 请求和 OpenAPI 注解。
- Local 演示 HTML 来源自动创建幂等规则版本。

## 验证

- 固定 HTML Fixture 与 WireMock 覆盖列表发现、详情抓取、规则测试、正文清洗、文章入库和页面渲染。
- RSS/Atom 回归测试继续通过。
- URL 安全测试覆盖非法协议、回环地址和公共 HTTPS 地址。
- 默认测试不访问实时外部网站。

## 已知边界

- 当前 HTML Provider 只处理服务端返回的静态 HTML，不执行 JavaScript。
- 不绕过登录、验证码、付费墙、访问控制或反自动化措施。
- arXiv、Crossref、PubMed、GDELT 和 AI 等只完成稳定接口边界，具体 Provider 后续按优先级接入。
- 时间解析当前支持 ISO-8601 或单一配置格式，复杂多格式解析后续增强。

## 下一阶段

Stage 7 建议实现主题规则匹配、文章分类、质量评分和可解释的来源评级，并在能力接口之上增加首个结构化学术数据源 Provider。
