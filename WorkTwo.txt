# 角色定位

你是一名资深 Java 架构师、Spring Boot 全栈开发工程师、爬虫系统工程师、产品经理、测试工程师和技术文档工程师。

你需要从零开始设计并实现一个可以在本地单机部署运行的“爬虫资料收集与阅读管理系统”。

你必须同时完成：

* 产品与需求设计；
* 系统架构设计；
* UI 与交互设计；
* 数据库设计；
* 后端开发；
* 前端开发；
* 自动化测试；
* Git 版本管理；
* 本地部署；
* 全套 Markdown 项目文档；
* 分阶段迭代记录。

不要只输出理论方案，必须在当前项目目录中创建真实、可运行、可验证的项目文件。

---

# 一、项目背景

我要构建一个个人或小团队使用的资料收集系统。

系统管理员可以预设不同的信息主题，例如：

```text
科学
认知
新闻
人工智能
软件开发
商业
经济
历史
健康
教育
```

系统按照这些主题，从管理员配置的信息源中定期收集最新内容。

信息源包括但不限于：

* RSS；
* Atom；
* 公开网站文章列表；
* 新闻网站；
* 博客；
* 科技媒体；
* 研究机构网站；
* 公开 API；
* 公开网页；
* 用户手动提交的网址。

采集后的内容需要经过：

```text
信息源请求
  ↓
文章列表发现
  ↓
链接提取
  ↓
文章正文抽取
  ↓
内容清洗
  ↓
主题匹配
  ↓
重复检测
  ↓
质量评分
  ↓
保存入库
  ↓
方便阅读、搜索和管理
```

第一版本只需要：

* Java；
* Spring Boot；
* 本地单机部署；
* 单用户或管理员模式；
* 前后端一体化；
* 本地数据库；
* 本地文件存储；
* 不使用微服务；
* 不依赖必须付费的外部服务。

---

# 二、核心目标

系统必须形成以下完整业务闭环：

```text
创建主题
  ↓
配置采集源
  ↓
配置采集规则
  ↓
手动或定时执行采集
  ↓
发现最新文章
  ↓
正文抽取与清洗
  ↓
去重和主题匹配
  ↓
进入资料库
  ↓
列表、搜索、筛选、阅读
  ↓
收藏、标记已读、归档
  ↓
查看采集任务和失败记录
```

系统不能只是一个 Jsoup 爬虫示例。

它必须是一个具有管理后台、采集任务、资料库和阅读体验的完整应用。

---

# 三、第一版本技术栈

## 3.1 基础环境

使用：

* Java 17；
* Spring Boot 3.x；
* Maven；
* Git；
* Spring MVC；
* Spring Validation；
* Spring Data JPA；
* H2 文件数据库；
* Flyway；
* Thymeleaf；
* Bootstrap；
* 原生 JavaScript；
* JUnit 5；
* Spring Boot Test；
* SLF4J；
* Logback。

## 3.2 内容采集组件

优先使用：

* Jsoup：HTML 获取、解析和正文抽取；
* Rome：RSS 与 Atom 解析；
* Java HttpClient 或 Spring RestClient：HTTP 请求；
* Jackson：JSON 解析；
* Bucket4j 或自定义限速器：访问频率控制，可选；
* Apache Tika：文本与文档元数据抽取，可作为后续扩展。

## 3.3 第一版本暂不引入

不要在第一版本中引入：

* 微服务；
* Spring Cloud；
* Nacos；
* Redis；
* Kafka；
* RabbitMQ；
* Elasticsearch；
* Kubernetes；
* Docker 编排；
* Selenium；
* Playwright；
* 浏览器自动化集群；
* 分布式任务调度；
* 大语言模型强依赖；
* 付费新闻 API；
* 强制联网才能启动的服务。

对于必须执行 JavaScript 才能加载内容的网站，第一版本允许标记为：

```text
UNSUPPORTED_DYNAMIC_PAGE
```

为后续引入浏览器采集 Provider 预留接口，但不得影响第一版本启动。

---

# 四、推荐项目名称

项目名称：

```text
knowledge-collector
```

包名：

```text
com.example.knowledgecollector
```

Maven 信息：

```xml
<groupId>com.example</groupId>
<artifactId>knowledge-collector</artifactId>
<version>1.0.0</version>
```

---

# 五、项目目录结构

建议采用 Maven 模块化单体：

```text
knowledge-collector
├── .gitignore
├── README.md
├── pom.xml
├── start.bat
├── start.sh
├── docs
│   ├── 01-project
│   ├── 02-requirements
│   ├── 03-design
│   ├── 04-development
│   ├── 05-testing
│   ├── 06-deployment
│   ├── 07-user-guide
│   ├── 08-operations
│   ├── 09-faq
│   └── stages
├── data
│   ├── database
│   ├── article-content
│   ├── snapshots
│   ├── exports
│   └── logs
├── scripts
├── knowledge-collector-domain
├── knowledge-collector-application
├── knowledge-collector-infrastructure
├── knowledge-collector-web
└── knowledge-collector-boot
```

如果多模块会明显增加第一阶段复杂度，也可以先建立单个 Maven 工程，但 Java 包必须严格按照分层结构划分：

```text
com.example.knowledgecollector
├── common
├── config
├── topic
├── source
├── crawler
├── task
├── article
├── search
├── reading
├── dashboard
├── audit
└── web
```

第一版本优先保证：

* 可以运行；
* 职责清晰；
* 后续可以拆模块；
* 不过度设计。

---

# 六、系统角色

第一版本只设置一个角色：

```text
ADMIN
```

管理员拥有：

* 主题管理；
* 采集源管理；
* 采集规则管理；
* 手动执行采集；
* 定时任务管理；
* 文章管理；
* 收藏管理；
* 标签管理；
* 失败任务重试；
* 系统设置；
* 数据备份和导出。

第一版本可以暂不实现登录。

但 Controller、Service 和数据访问层必须保持清晰边界，为后续增加 Spring Security 预留空间。

---

# 七、核心业务模块

## 7.1 主题管理

管理员可以创建主题，例如：

```text
科学
认知
新闻
人工智能
软件开发
商业
```

主题字段至少包括：

* 主题名称；
* 主题编码；
* 主题描述；
* 主题颜色；
* 主题图标；
* 关键词；
* 排除关键词；
* 默认语言；
* 是否启用；
* 排序值；
* 创建时间；
* 修改时间。

一个主题可以配置：

* 多个关键词；
* 多个排除关键词；
* 多个采集源；
* 多个标签；
* 多套匹配规则。

---

## 7.2 采集源管理

采集源类型至少支持：

```text
RSS
ATOM
HTML_LIST
JSON_API
MANUAL_URL
```

采集源字段至少包括：

* 名称；
* 编码；
* 来源类型；
* 首页地址；
* 订阅地址或接口地址；
* 所属主题；
* 默认语言；
* 字符编码；
* 请求超时时间；
* 最大重试次数；
* 请求间隔；
* User-Agent；
* 是否遵守 robots.txt；
* 是否允许采集正文；
* 是否只保存摘要；
* 是否保存网页快照；
* 是否启用；
* 最后成功时间；
* 最后失败时间；
* 连续失败次数；
* 备注。

系统不得在代码中写死采集网站。

所有采集源都应通过后台配置或初始化数据管理。

---

## 7.3 采集规则管理

对于 HTML 列表和文章页面，需要支持配置选择器。

至少支持：

```text
列表区域选择器
文章链接选择器
标题选择器
正文选择器
摘要选择器
作者选择器
发布时间选择器
标签选择器
下一页选择器
需要移除的元素选择器
```

规则字段示例：

```text
listSelector
linkSelector
titleSelector
contentSelector
summarySelector
authorSelector
publishTimeSelector
tagSelector
removeSelectors
```

支持在管理后台：

* 新建规则；
* 编辑规则；
* 测试规则；
* 输入测试 URL；
* 预览提取结果；
* 启用规则；
* 停用规则；
* 复制规则；
* 保存规则版本。

规则变更不能直接覆盖历史版本。

---

## 7.4 采集任务

系统支持：

* 手动采集；
* 定时采集；
* 单个采集源采集；
* 按主题批量采集；
* 失败任务重试；
* 任务取消；
* 查看任务日志。

任务状态：

```java
public enum CrawlTaskStatus {

    CREATED,

    RUNNING,

    PARTIAL_SUCCESS,

    SUCCESS,

    FAILED,

    CANCELED
}
```

任务至少记录：

* 任务编号；
* 任务类型；
* 主题；
* 采集源；
* 开始时间；
* 结束时间；
* 发现链接数；
* 新增文章数；
* 重复文章数；
* 更新文章数；
* 失败文章数；
* 跳过文章数；
* 错误信息；
* 执行耗时；
* 是否人工触发。

第一版本可以使用 Spring `@Scheduled`。

不要引入 Quartz，除非现有需求确实需要动态 Cron 管理。

---

## 7.5 内容发现

采集过程至少包括：

```text
读取信息源
  ↓
发现候选文章链接
  ↓
URL 标准化
  ↓
检查 URL 是否已处理
  ↓
获取文章页面
  ↓
抽取正文和元数据
```

URL 标准化需要处理：

* 相对地址；
* URL Fragment；
* 无意义追踪参数；
* 重复斜杠；
* HTTP 与 HTTPS；
* URL 编码；
* 常见 UTM 参数；
* 页面内锚点。

不得把所有查询参数都直接删除。

需要保留可能影响实际文章内容的参数。

---

## 7.6 正文抽取与清洗

正文抽取至少支持：

* 按配置的 CSS Selector 提取；
* 移除脚本；
* 移除样式；
* 移除导航；
* 移除广告；
* 移除推荐内容；
* 移除评论；
* 移除空标签；
* 规范化段落；
* 保留标题；
* 保留正文段落；
* 保留列表；
* 保留引用；
* 保留基础图片链接；
* 生成纯文本；
* 生成安全 HTML。

保存内容时至少包含：

* 原始 HTML，可选；
* 清洗后的 HTML；
* 纯文本正文；
* 内容摘要；
* 字数；
* 阅读时长；
* 来源信息；
* 原文链接。

不能直接展示未经清洗的第三方 HTML。

必须防止：

* XSS；
* 恶意脚本；
* 内联事件；
* iframe 注入；
* 危险链接协议。

---

## 7.7 文章去重

至少实现以下去重机制：

### URL 去重

对标准化后的 URL 计算哈希。

### 标题去重

对标题进行：

* 去空格；
* 去特殊符号；
* 大小写归一化；
* 全角半角归一化。

### 内容指纹去重

对清洗后的正文计算：

* SHA-256；
* SimHash，可作为第二阶段实现。

第一版本至少实现：

```text
标准化 URL 哈希 + 正文 SHA-256
```

重复内容不得反复创建新记录。

对于同一篇文章内容更新的情况，应保留：

* 首次采集时间；
* 最后采集时间；
* 内容更新时间；
* 内容版本号。

---

## 7.8 主题匹配

文章与主题的关系不能完全依赖采集源。

需要支持：

* 采集源默认主题；
* 标题关键词匹配；
* 正文关键词匹配；
* 排除关键词；
* 人工调整主题；
* 一篇文章属于多个主题。

第一版本采用：

```text
采集源默认主题
+
关键词规则匹配
+
人工审核调整
```

不要强制引入 AI 分类服务。

需要定义可扩展接口：

```java
public interface TopicClassifier {

    TopicClassificationResult classify(ArticleContent content);
}
```

第一版本实现：

```text
KeywordTopicClassifier
```

后续可以增加：

```text
MachineLearningTopicClassifier
LargeLanguageModelTopicClassifier
```

---

## 7.9 文章质量评分

系统可以根据以下因素计算基础质量分：

* 是否有标题；
* 是否有正文；
* 正文字数；
* 是否有发布时间；
* 是否有作者；
* 是否命中主题关键词；
* 是否存在大量重复文本；
* 是否为广告页面；
* 是否过短；
* 是否为错误页面；
* 来源可信度。

质量分取值：

```text
0—100
```

低质量文章可以进入：

```text
待审核
```

第一版本不需要建立复杂机器学习模型。

采用可解释规则即可。

---

## 7.10 阅读资料库

文章列表需要支持：

* 按主题筛选；
* 按来源筛选；
* 按发布时间筛选；
* 按采集时间筛选；
* 按状态筛选；
* 按标签筛选；
* 按质量分筛选；
* 按收藏状态筛选；
* 按已读状态筛选；
* 标题和正文关键词搜索；
* 分页；
* 排序。

排序方式至少包括：

```text
最新发布
最新采集
质量最高
最近阅读
```

---

## 7.11 文章阅读页面

阅读页面需要提供：

* 文章标题；
* 来源名称；
* 原文链接；
* 作者；
* 发布时间；
* 采集时间；
* 所属主题；
* 标签；
* 预计阅读时间；
* 清洗后的正文；
* 收藏；
* 标记已读；
* 归档；
* 添加个人笔记；
* 上一篇；
* 下一篇。

正文阅读体验要求：

* 内容区域宽度适中；
* 字号舒适；
* 行高合理；
* 支持浅色和深色阅读模式，可作为第二阶段；
* 图片自适应；
* 长文章可显示目录，可作为第二阶段。

不要在阅读页面直接嵌入第三方完整网页。

---

## 7.12 收藏、已读、归档和笔记

文章状态至少包括：

```text
UNREAD
READ
ARCHIVED
IGNORED
```

支持：

* 收藏文章；
* 取消收藏；
* 标记已读；
* 标记未读；
* 归档；
* 忽略；
* 添加个人笔记；
* 添加自定义标签。

---

## 7.13 搜索

第一版本使用数据库查询实现基础搜索：

* 标题模糊搜索；
* 摘要模糊搜索；
* 正文纯文本搜索；
* 来源名称搜索；
* 标签搜索。

为后续引入 Lucene 或 Elasticsearch 预留：

```java
public interface ArticleSearchService {

    SearchResult search(ArticleSearchQuery query);
}
```

第一版本不要引入 Elasticsearch。

如果 H2 大文本模糊查询性能不足，可在后续阶段增加本地 Apache Lucene。

---

## 7.14 仪表盘

首页至少展示：

* 今日新增文章数；
* 今日采集任务数；
* 待阅读文章数；
* 收藏文章数；
* 采集失败数量；
* 启用主题数量；
* 启用采集源数量；
* 最近采集任务；
* 最近新增文章；
* 各主题文章数量；
* 各来源成功率。

---

# 八、合规与安全要求

采集系统必须遵守以下规则：

1. 优先使用 RSS、Atom 和公开 API。
2. 采集公开可访问的信息。
3. 支持检查并遵守 robots.txt。
4. 不绕过登录、验证码、付费墙和访问控制。
5. 不绕过反爬安全机制。
6. 不伪造登录身份。
7. 不批量抓取个人隐私信息。
8. 不采集明确禁止自动访问的内容。
9. 支持配置请求频率。
10. 默认限制同一域名的并发请求。
11. 使用明确、可配置的 User-Agent。
12. 记录文章来源和原文链接。
13. 阅读页面展示内容来源。
14. 支持只保存摘要和链接。
15. 支持删除来源及其采集数据。
16. 不把爬虫设计成绕过网站限制的工具。
17. 不提供验证码破解、代理池攻击、账号批量注册等能力。
18. 不默认保存第三方页面中的脚本和危险内容。

系统需要在文档中明确：

* 数据使用边界；
* 来源版权说明；
* 采集合规说明；
* 管理员责任；
* 删除机制；
* 内容保留策略。

---

# 九、HTTP 请求要求

HTTP 客户端需要支持：

* 连接超时；
* 读取超时；
* 最大重试次数；
* 重试间隔；
* 自动跟随有限次数重定向；
* User-Agent；
* Accept-Language；
* 字符编码识别；
* GZIP；
* 响应大小限制；
* Content-Type 校验。

不得无限读取响应体。

建议限制：

```text
单个 HTML 响应最大 5MB
单张图片最大 10MB
单次重定向最大 5次
```

对于以下状态进行区分处理：

```text
200
301
302
304
400
401
403
404
408
429
500
502
503
504
```

对于 `429` 和临时服务器错误，可执行有限次数退避重试。

不得无限重试。

---

# 十、数据库设计

至少设计以下数据表。

## 10.1 主题表

```text
topic
```

字段至少包括：

* id
* topic_code
* topic_name
* description
* keywords
* excluded_keywords
* color
* icon
* language
* enabled
* sort_order
* created_at
* updated_at

## 10.2 采集源表

```text
crawl_source
```

字段至少包括：

* id
* source_code
* source_name
* source_type
* home_url
* feed_url
* language
* charset
* user_agent
* timeout_seconds
* max_retries
* request_interval_millis
* obey_robots
* fetch_full_content
* save_snapshot
* enabled
* last_success_at
* last_failure_at
* consecutive_failures
* created_at
* updated_at

## 10.3 主题与采集源关系表

```text
topic_source_rel
```

字段至少包括：

* id
* topic_id
* source_id
* created_at

## 10.4 采集规则表

```text
crawl_rule
```

字段至少包括：

* id
* source_id
* version
* list_selector
* link_selector
* title_selector
* content_selector
* summary_selector
* author_selector
* publish_time_selector
* tag_selector
* remove_selectors
* active
* created_at
* updated_at

## 10.5 采集任务表

```text
crawl_task
```

字段至少包括：

* id
* task_no
* trigger_type
* topic_id
* source_id
* status
* discovered_count
* created_count
* duplicate_count
* updated_count
* skipped_count
* failed_count
* started_at
* finished_at
* duration_millis
* error_code
* error_message
* created_at

## 10.6 采集任务明细表

```text
crawl_task_item
```

字段至少包括：

* id
* task_id
* original_url
* normalized_url
* status
* article_id
* retry_count
* error_code
* error_message
* started_at
* finished_at
* created_at

## 10.7 文章表

```text
article
```

字段至少包括：

* id
* source_id
* title
* subtitle
* author
* summary
* original_url
* normalized_url
* url_hash
* language
* publish_time
* first_collected_at
* last_collected_at
* content_updated_at
* content_version
* word_count
* reading_minutes
* quality_score
* review_status
* reading_status
* favorite
* archived
* created_at
* updated_at

## 10.8 文章内容表

```text
article_content
```

字段至少包括：

* id
* article_id
* raw_html_path
* clean_html
* plain_text
* content_hash
* snapshot_path
* created_at
* updated_at

对于正文较长的内容，可以选择：

* 存储在数据库 CLOB；
* 或存储在本地文件中，数据库保存路径。

第一版本选择一种方案，并在架构文档中说明原因。

## 10.9 文章与主题关系表

```text
article_topic_rel
```

字段至少包括：

* id
* article_id
* topic_id
* match_type
* confidence
* manually_adjusted
* created_at

## 10.10 标签表

```text
tag
```

## 10.11 文章标签关系表

```text
article_tag_rel
```

## 10.12 笔记表

```text
article_note
```

字段至少包括：

* id
* article_id
* note_content
* created_at
* updated_at

## 10.13 系统配置表

```text
system_setting
```

## 10.14 审计日志表

```text
audit_log
```

所有表结构必须通过 Flyway 管理。

禁止使用 Hibernate 自动创建正式数据库结构。

配置：

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
```

---

# 十一、接口设计

系统同时提供页面 Controller 和基础 REST API。

REST API 路径统一使用：

```text
/api/v1
```

至少包括：

## 主题

```http
GET    /api/v1/topics
POST   /api/v1/topics
GET    /api/v1/topics/{id}
PUT    /api/v1/topics/{id}
DELETE /api/v1/topics/{id}
```

## 采集源

```http
GET    /api/v1/sources
POST   /api/v1/sources
GET    /api/v1/sources/{id}
PUT    /api/v1/sources/{id}
DELETE /api/v1/sources/{id}
POST   /api/v1/sources/{id}/test
```

## 采集规则

```http
GET  /api/v1/sources/{sourceId}/rules
POST /api/v1/sources/{sourceId}/rules
POST /api/v1/sources/{sourceId}/rules/test
```

## 任务

```http
POST /api/v1/crawl/tasks
GET  /api/v1/crawl/tasks
GET  /api/v1/crawl/tasks/{id}
POST /api/v1/crawl/tasks/{id}/retry
POST /api/v1/crawl/tasks/{id}/cancel
```

## 文章

```http
GET    /api/v1/articles
GET    /api/v1/articles/{id}
PUT    /api/v1/articles/{id}
POST   /api/v1/articles/{id}/favorite
DELETE /api/v1/articles/{id}/favorite
POST   /api/v1/articles/{id}/read
POST   /api/v1/articles/{id}/archive
POST   /api/v1/articles/{id}/ignore
```

## 笔记

```http
GET  /api/v1/articles/{id}/notes
POST /api/v1/articles/{id}/notes
PUT  /api/v1/notes/{noteId}
DELETE /api/v1/notes/{noteId}
```

统一响应结构：

```json
{
  "success": true,
  "code": "SUCCESS",
  "message": "操作成功",
  "data": {},
  "timestamp": "2026-07-16T20:00:00"
}
```

分页响应：

```json
{
  "items": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0
}
```

---

# 十二、错误码设计

至少定义以下错误码：

```text
TOPIC_NOT_FOUND
SOURCE_NOT_FOUND
RULE_NOT_FOUND
ARTICLE_NOT_FOUND
TASK_NOT_FOUND
SOURCE_DISABLED
SOURCE_TYPE_NOT_SUPPORTED
INVALID_SOURCE_URL
ROBOTS_NOT_ALLOWED
REQUEST_TIMEOUT
HTTP_STATUS_ERROR
RESPONSE_TOO_LARGE
CONTENT_TYPE_NOT_SUPPORTED
RSS_PARSE_FAILED
HTML_PARSE_FAILED
ARTICLE_LINK_NOT_FOUND
ARTICLE_CONTENT_NOT_FOUND
DUPLICATE_ARTICLE
DYNAMIC_PAGE_NOT_SUPPORTED
RATE_LIMITED
TASK_ALREADY_RUNNING
DATABASE_ERROR
FILE_STORAGE_ERROR
```

错误信息使用中文。

日志保留详细异常。

页面不得直接显示完整异常堆栈。

---

# 十三、前端页面

第一版本使用：

* Thymeleaf；
* Bootstrap；
* 原生 JavaScript；
* 前后端一体化。

页面至少包括：

```text
/dashboard
/topics
/sources
/sources/{id}
/sources/{id}/rule-test
/tasks
/tasks/{id}
/articles
/articles/{id}
/favorites
/settings
/help
```

## 页面导航

左侧导航建议包含：

* 仪表盘；
* 资料库；
* 收藏；
* 主题管理；
* 采集源；
* 采集任务；
* 标签管理；
* 系统设置；
* 帮助文档。

## 文章列表卡片

至少展示：

* 标题；
* 摘要；
* 来源；
* 主题；
* 发布时间；
* 采集时间；
* 阅读时长；
* 质量分；
* 是否已读；
* 是否收藏。

## 管理页面要求

* 表单校验；
* 成功提示；
* 错误提示；
* 删除确认；
* 空状态；
* 加载状态；
* 分页；
* 筛选条件保留；
* 中文界面。

---

# 十四、配置要求

建议配置：

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:h2:file:./data/database/knowledge-collector
    driver-class-name: org.h2.Driver
    username: sa
    password:

  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false

  flyway:
    enabled: true

app:
  storage:
    root-path: ./data
    article-path: ./data/article-content
    snapshot-path: ./data/snapshots
    export-path: ./data/exports

  crawler:
    enabled: true
    default-user-agent: KnowledgeCollector/1.0
    connect-timeout-seconds: 10
    read-timeout-seconds: 20
    max-response-size-bytes: 5242880
    default-request-interval-millis: 2000
    max-redirects: 5
    max-retries: 2
    scheduler:
      enabled: true
      fixed-delay-millis: 1800000

  content:
    save-raw-html: false
    save-clean-html: true
    save-snapshot: false
    minimum-content-length: 100
```

不得在代码中写死绝对路径。

Windows、Linux 和 macOS 路径必须兼容。

---

# 十五、Git 版本控制要求

项目必须使用 Git。

如果当前目录不是 Git 仓库，执行：

```bash
git init
```

创建：

```text
.gitignore
.gitattributes
```

`.gitignore` 至少忽略：

```text
target/
.idea/
*.iml
.vscode/
data/database/
data/logs/
data/article-content/
data/snapshots/
data/exports/
*.log
.env
```

不得提交：

* 数据库运行文件；
* 日志；
* 用户采集内容；
* 密钥；
* Token；
* 本地临时文件；
* IDE 私有配置。

## 版本策略

第一版本使用简化的主干开发：

```text
main
```

每个阶段完成后：

1. 运行测试；
2. 更新对应阶段报告；
3. 更新 CHANGELOG；
4. 提交 Git；
5. 创建阶段标签。

提交信息格式：

```text
stage-{id}: 阶段说明
```

标签格式：

```text
stage-{id}
```

例如：

```bash
git add .
git commit -m "stage-1: 完成项目立项与需求文档"
git tag stage-1
```

不得伪造 Git 命令执行结果。

如果当前运行环境无法提交 Git，需要明确说明，并给出用户可以执行的命令。

---

# 十六、项目文档要求

所有文档使用 Markdown 格式。

文档内容必须与实际代码一致。

禁止生成只有标题、没有实际内容的空文档。

建议文档目录：

```text
docs
├── 01-project
│   └── project-charter.md
├── 02-requirements
│   ├── user-requirements.md
│   ├── product-requirements-document.md
│   └── software-requirements-specification.md
├── 03-design
│   ├── prototype-design.md
│   ├── ui-design-guidelines.md
│   ├── system-architecture-design.md
│   ├── detailed-design.md
│   ├── database-design.md
│   └── api-design.md
├── 04-development
│   ├── development-environment-setup.md
│   ├── module-development-guide.md
│   └── third-party-dependencies.md
├── 05-testing
│   ├── test-plan.md
│   ├── test-cases.md
│   └── test-report.md
├── 06-deployment
│   └── deployment-guide.md
├── 07-user-guide
│   └── user-manual.md
├── 08-operations
│   ├── administration-manual.md
│   └── installation-manual.md
├── 09-faq
│   └── faq.md
└── stages
```

必须完成以下文档。

---

## 16.1 项目立项说明书

文件：

```text
docs/01-project/project-charter.md
```

至少包含：

* 项目背景；
* 建设目标；
* 建设范围；
* 项目价值；
* 用户群体；
* 项目边界；
* 第一版本范围；
* 非目标；
* 技术约束；
* 项目风险；
* 合规风险；
* 里程碑；
* 验收标准。

---

## 16.2 用户需求文档

文件：

```text
docs/02-requirements/user-requirements.md
```

至少包含：

* 用户画像；
* 用户目标；
* 使用场景；
* 用户痛点；
* 用户故事；
* 功能期望；
* 非功能期望；
* 用户验收条件。

---

## 16.3 产品需求文档

文件：

```text
docs/02-requirements/product-requirements-document.md
```

至少包含：

* 产品定位；
* 产品目标；
* 核心指标；
* 功能地图；
* 业务流程；
* 页面结构；
* 功能优先级；
* MVP 范围；
* 后续规划；
* 需求验收条件。

---

## 16.4 需求规格说明书

文件：

```text
docs/02-requirements/software-requirements-specification.md
```

至少包含：

* 功能需求；
* 非功能需求；
* 性能要求；
* 安全要求；
* 合规要求；
* 数据要求；
* 接口要求；
* 异常流程；
* 边界条件；
* 约束条件；
* 可追踪需求编号。

需求编号格式：

```text
FR-001
NFR-001
SEC-001
DATA-001
```

---

## 16.5 原型设计文档

文件：

```text
docs/03-design/prototype-design.md
```

使用 Markdown、Mermaid 和文本线框图描述：

* 信息架构；
* 页面导航；
* 页面布局；
* 页面字段；
* 页面操作；
* 页面跳转；
* 关键交互；
* 空状态；
* 异常状态；
* 加载状态。

---

## 16.6 UI 设计规范

文件：

```text
docs/03-design/ui-design-guidelines.md
```

至少包含：

* 颜色规范；
* 字体规范；
* 字号；
* 行高；
* 间距；
* 圆角；
* 阴影；
* 按钮；
* 表格；
* 表单；
* 标签；
* 卡片；
* 弹窗；
* 状态颜色；
* 图标；
* 响应式规则；
* 阅读页面排版规则；
* 无障碍要求。

---

## 16.7 系统概要设计文档

文件：

```text
docs/03-design/system-architecture-design.md
```

至少包含：

* 系统上下文；
* 总体架构；
* 模块划分；
* 分层设计；
* 依赖关系；
* 数据流；
* 采集流程；
* 调度流程；
* 内容处理流程；
* 异常处理；
* 日志；
* 安全；
* 部署架构；
* 后续扩展路线。

使用 Mermaid 绘制：

* 系统上下文图；
* 组件图；
* 采集时序图；
* 数据流图。

---

## 16.8 详细设计文档

文件：

```text
docs/03-design/detailed-design.md
```

至少包含：

* 核心类设计；
* 接口设计；
* Service 职责；
* Repository 职责；
* DTO；
* 状态机；
* 去重算法；
* 主题匹配算法；
* 正文清洗；
* 质量评分；
* URL 标准化；
* 重试策略；
* 事务边界；
* 异常码；
* 核心时序。

---

## 16.9 数据库设计文档

文件：

```text
docs/03-design/database-design.md
```

至少包含：

* ER 图；
* 表说明；
* 字段说明；
* 主键；
* 外键；
* 索引；
* 唯一约束；
* 枚举；
* 数据生命周期；
* 数据备份；
* 数据迁移；
* 数据清理策略。

---

## 16.10 接口设计文档

文件：

```text
docs/03-design/api-design.md
```

至少包含：

* API 约定；
* 请求结构；
* 响应结构；
* 分页；
* 错误码；
* 接口清单；
* 请求示例；
* 响应示例；
* 校验规则；
* 幂等性；
* 安全预留。

---

## 16.11 开发环境搭建文档

文件：

```text
docs/04-development/development-environment-setup.md
```

至少包含：

* JDK 安装；
* Maven 安装；
* Git 安装；
* IDE 配置；
* 项目克隆；
* 配置文件；
* 数据目录；
* 运行命令；
* 测试命令；
* 常见环境问题。

---

## 16.12 模块开发说明

文件：

```text
docs/04-development/module-development-guide.md
```

至少包含：

* 模块职责；
* 包结构；
* 调用关系；
* 扩展新主题；
* 增加新采集源；
* 增加新采集类型；
* 增加新正文解析器；
* 增加新主题分类器；
* 增加新搜索实现；
* 编码规范；
* 提交规范。

---

## 16.13 第三方组件与依赖清单

文件：

```text
docs/04-development/third-party-dependencies.md
```

至少包含：

* 组件名称；
* 版本；
* 用途；
* 官方地址；
* 开源协议；
* 是否必须；
* 替代方案；
* 安全注意事项；
* 升级注意事项。

---

## 16.14 测试文档

文件：

```text
docs/05-testing/test-plan.md
docs/05-testing/test-cases.md
docs/05-testing/test-report.md
```

测试范围至少包括：

* 单元测试；
* 集成测试；
* 页面测试；
* 采集规则测试；
* RSS 解析测试；
* HTML 解析测试；
* URL 标准化测试；
* 去重测试；
* 主题匹配测试；
* 安全测试；
* 异常测试；
* 数据持久化测试；
* 部署测试。

---

## 16.15 部署文档

文件：

```text
docs/06-deployment/deployment-guide.md
```

至少包含：

* 环境要求；
* 构建命令；
* JAR 启动；
* 配置说明；
* 数据目录；
* 日志目录；
* 端口修改；
* 启动脚本；
* 停止方法；
* 升级方法；
* 回滚方法；
* 数据备份；
* 故障排查。

---

## 16.16 用户操作手册

文件：

```text
docs/07-user-guide/user-manual.md
```

至少包含：

* 系统首页；
* 创建主题；
* 创建采集源；
* 配置采集规则；
* 测试采集源；
* 手动执行任务；
* 查看任务；
* 阅读文章；
* 搜索文章；
* 收藏文章；
* 标记已读；
* 添加笔记；
* 常见操作。

---

## 16.17 管理手册

文件：

```text
docs/08-operations/administration-manual.md
```

至少包含：

* 系统配置；
* 定时任务；
* 来源维护；
* 失败任务处理；
* 数据清理；
* 日志查看；
* 数据备份；
* 数据恢复；
* 性能观察；
* 合规管理；
* 来源停用。

---

## 16.18 安装手册

文件：

```text
docs/08-operations/installation-manual.md
```

面向非开发人员，提供：

* Windows 安装；
* Linux 安装；
* JDK 检查；
* 启动脚本；
* 浏览器访问；
* 数据目录；
* 升级；
* 卸载；
* 数据保留。

---

## 16.19 常见问题文档

文件：

```text
docs/09-faq/faq.md
```

至少回答：

* 系统无法启动；
* 端口被占用；
* 数据库文件损坏；
* RSS 解析失败；
* 网页正文为空；
* 网站返回 403；
* 网站返回 429；
* 页面乱码；
* 文章重复；
* 定时任务没有执行；
* 文章时间不正确；
* 如何备份；
* 如何恢复；
* 如何清空测试数据；
* 动态网页无法采集；
* robots.txt 禁止采集；
* 如何修改请求频率。

---

# 十七、阶段迭代规则

项目必须按照阶段逐步开发。

每个阶段都必须创建阶段报告。

阶段报告命名必须严格使用：

```text
stage-{id}-report.md
```

保存目录：

```text
docs/stages/
```

例如：

```text
docs/stages/stage-1-report.md
docs/stages/stage-2-report.md
docs/stages/stage-3-report.md
```

阶段编号从 `1` 开始，使用阿拉伯数字。

不得使用：

```text
stage-01-report.md
stage-one-report.md
stage1-report.md
```

必须严格使用：

```text
stage-{id}-report.md
```

---

# 十八、阶段报告模板

每一个 `stage-{id}-report.md` 必须包含：

```markdown
# Stage {id} 阶段报告

## 1. 阶段目标

## 2. 本阶段范围

## 3. 已完成事项

## 4. 未完成事项

## 5. 新增文件

## 6. 修改文件

## 7. 核心设计说明

## 8. 数据库变更

## 9. 接口变更

## 10. 页面变更

## 11. 测试情况

## 12. 构建与运行结果

## 13. Git 提交与标签

## 14. 已知问题

## 15. 风险与技术债务

## 16. 验收结果

## 17. 下一阶段计划
```

必须填写实际内容。

不得写入虚假的：

* 编译成功；
* 测试成功；
* Git 提交成功；
* 页面验证成功。

执行失败时，应记录真实错误和修复建议。

---

# 十九、推荐开发阶段

## Stage 1：项目立项与需求基线

完成：

* Git 初始化；
* `.gitignore`；
* 项目目录；
* 项目立项说明书；
* 用户需求文档；
* 产品需求文档；
* 需求规格说明书；
* 第一版范围；
* 风险清单；
* `stage-1-report.md`。

本阶段暂不开发具体业务代码。

---

## Stage 2：架构、原型与设计基线

完成：

* 原型设计；
* UI 设计规范；
* 系统概要设计；
* 详细设计；
* 数据库设计；
* 接口设计；
* Mermaid 架构图；
* Maven 项目骨架；
* `stage-2-report.md`。

---

## Stage 3：基础工程与本地运行

完成：

* Spring Boot 启动；
* H2 文件数据库；
* Flyway；
* 日志；
* 全局异常；
* 统一响应；
* 本地文件目录；
* 启动脚本；
* README；
* 开发环境文档；
* 第三方依赖文档；
* `stage-3-report.md`。

---

## Stage 4：主题和采集源管理

完成：

* 主题管理；
* 采集源管理；
* 主题与采集源关系；
* 管理页面；
* REST API；
* 单元测试；
* 集成测试；
* `stage-4-report.md`。

---

## Stage 5：RSS 与 Atom 采集

完成：

* RSS Provider；
* Atom Provider；
* 手动执行任务；
* 任务日志；
* URL 标准化；
* 基础去重；
* 文章入库；
* `stage-5-report.md`。

---

## Stage 6：HTML 网页采集

完成：

* HTML 列表采集；
* CSS Selector 规则；
* 规则测试页面；
* 正文提取；
* 内容清洗；
* HTML 安全处理；
* `stage-6-report.md`。

---

## Stage 7：主题匹配与质量控制

完成：

* 关键词分类；
* 排除关键词；
* 多主题；
* 质量评分；
* 低质量待审核；
* 内容指纹；
* `stage-7-report.md`。

---

## Stage 8：资料库与阅读功能

完成：

* 文章列表；
* 筛选；
* 搜索；
* 阅读详情；
* 收藏；
* 已读；
* 归档；
* 标签；
* 笔记；
* `stage-8-report.md`。

---

## Stage 9：定时任务与运维管理

完成：

* 定时采集；
* 失败重试；
* 采集频率控制；
* 来源健康状态；
* 仪表盘；
* 管理手册；
* `stage-9-report.md`。

---

## Stage 10：测试、部署与项目验收

完成：

* 完整测试计划；
* 测试用例；
* 测试报告；
* 部署文档；
* 用户操作手册；
* 安装手册；
* FAQ；
* 打包验证；
* 数据备份恢复验证；
* `stage-10-report.md`。

---

# 二十、每个阶段执行规则

每个阶段必须按照以下顺序执行：

1. 阅读当前代码和已有文档；
2. 检查上一阶段报告；
3. 明确本阶段验收标准；
4. 更新需求追踪关系；
5. 实现本阶段内容；
6. 编写测试；
7. 执行测试；
8. 执行构建；
9. 更新对应文档；
10. 创建阶段报告；
11. 检查 Git 变更；
12. 提交 Git；
13. 创建 Git 标签；
14. 汇报真实结果。

未经本阶段验收，不要直接跳到后续阶段。

如果当前阶段出现阻塞：

* 记录阻塞原因；
* 完成所有不受影响的工作；
* 不删除已完成内容；
* 不伪造成功结果；
* 在阶段报告中记录；
* 提供下一步修复方法。

---

# 二十一、代码设计原则

必须遵循：

1. Controller 不编写业务逻辑。
2. Service 不直接拼接网页 HTML。
3. Repository 不承担业务规则。
4. HTTP 请求、RSS 解析、HTML 解析必须解耦。
5. 采集源类型采用 Provider 模式。
6. 正文提取采用可替换 Extractor。
7. 主题分类采用可替换 Classifier。
8. 搜索采用可替换 SearchService。
9. 文件存储采用可替换 StorageService。
10. 所有金额或评分精度使用合适类型。
11. 所有时间使用 Java Time API。
12. 所有数据库变更使用 Flyway。
13. 禁止写死采集网站。
14. 禁止写死绝对路径。
15. 禁止把所有代码放入一个 Service。
16. 禁止创建超过合理职责范围的超大型类。
17. 关键规则必须有测试。
18. 配置值必须进入配置文件。
19. 外部请求必须设置超时。
20. 外部请求必须限制响应大小。
21. 外部请求必须控制频率。
22. 日志不得记录敏感 Token 和完整 Cookie。
23. 不使用 `ddl-auto=create`。
24. 不使用 `double` 表示质量评分中的精确业务值时，应优先使用整数或 BigDecimal。
25. 不确定规则记录到：

```text
docs/01-project/assumptions.md
```

---

# 二十二、核心接口建议

## 采集 Provider

```java
public interface CrawlProvider {

    SourceType supportedType();

    SourceFetchResult fetch(CrawlSource source, CrawlContext context);
}
```

实现：

```text
RssCrawlProvider
AtomCrawlProvider
HtmlListCrawlProvider
JsonApiCrawlProvider
ManualUrlCrawlProvider
```

## HTTP 客户端

```java
public interface WebContentClient {

    WebResponse get(WebRequest request);
}
```

## URL 标准化

```java
public interface UrlNormalizer {

    NormalizedUrl normalize(String url, UrlNormalizationContext context);
}
```

## 正文提取

```java
public interface ArticleContentExtractor {

    boolean supports(CrawlSource source);

    ExtractedArticle extract(Document document, ExtractionRule rule);
}
```

## 内容清洗

```java
public interface ContentSanitizer {

    SanitizedContent sanitize(String html);
}
```

## 去重

```java
public interface ArticleDuplicateDetector {

    DuplicateCheckResult check(ArticleCandidate candidate);
}
```

## 主题分类

```java
public interface TopicClassifier {

    TopicClassificationResult classify(ArticleContent content);
}
```

## 质量评分

```java
public interface ArticleQualityScorer {

    QualityScoreResult score(ArticleCandidate article);
}
```

## 搜索

```java
public interface ArticleSearchService {

    SearchResult search(ArticleSearchQuery query);
}
```

---

# 二十三、测试要求

## 23.1 单元测试

至少包括：

* URL 标准化；
* URL 哈希；
* RSS 解析；
* Atom 解析；
* HTML 选择器；
* 标题清洗；
* 正文清洗；
* XSS 清理；
* 内容哈希；
* 重复检测；
* 关键词匹配；
* 排除关键词；
* 阅读时长；
* 质量评分；
* 日期解析；
* 字符编码；
* 重试判断；
* robots.txt 规则解析。

## 23.2 集成测试

至少包括：

* 创建主题；
* 创建采集源；
* 测试采集源；
* 创建采集任务；
* RSS 入库；
* HTML 入库；
* 重复文章跳过；
* 文章查询；
* 收藏；
* 已读；
* 笔记；
* 数据持久化。

## 23.3 测试资源

不要让自动化测试依赖实时外部网站。

在：

```text
src/test/resources
```

中提供：

* RSS 样例；
* Atom 样例；
* HTML 列表样例；
* HTML 文章样例；
* 错误页面样例；
* 编码异常样例；
* 重复文章样例。

对真实网络的测试应标记为手动测试或可跳过测试。

---

# 二十四、部署要求

项目最终可以通过以下方式运行：

```bash
mvn clean package
java -jar knowledge-collector-boot/target/knowledge-collector.jar
```

或者单模块情况下：

```bash
mvn clean package
java -jar target/knowledge-collector-1.0.0.jar
```

访问：

```text
http://localhost:8080
```

提供：

```text
start.bat
start.sh
```

启动脚本需要：

* 检查 Java 版本；
* 创建数据目录；
* 创建日志目录；
* 启动应用；
* 输出访问地址；
* 输出数据目录；
* 输出日志目录；
* 给出停止方式。

---

# 二十五、第一版本验收标准

项目至少满足：

1. Git 仓库初始化完成。
2. 文档目录完整。
3. 所有要求的 Markdown 文档已完成。
4. 每个阶段有对应 `stage-{id}-report.md`。
5. Spring Boot 项目可以启动。
6. H2 文件数据库可以持久化。
7. 应用重启后数据不丢失。
8. 可以创建、修改和停用主题。
9. 可以创建、修改和测试采集源。
10. 支持 RSS。
11. 支持 Atom。
12. 支持配置化 HTML 列表采集。
13. 支持手动 URL 采集。
14. 可以手动执行采集任务。
15. 可以定时执行采集任务。
16. 可以查看任务执行情况。
17. 可以查看失败原因。
18. 可以限制请求频率。
19. 可以设置超时。
20. 可以进行 URL 去重。
21. 可以进行内容哈希去重。
22. 可以清洗正文。
23. 可以安全展示正文。
24. 可以按照主题分类。
25. 可以搜索文章。
26. 可以筛选文章。
27. 可以收藏文章。
28. 可以标记已读。
29. 可以归档文章。
30. 可以添加个人笔记。
31. 可以查看仪表盘。
32. 可以备份数据库和本地内容。
33. 可以通过启动脚本运行。
34. 核心模块有自动化测试。
35. `mvn clean test` 可以成功执行。
36. `mvn clean package` 可以成功执行。
37. 项目文档与实际代码保持一致。
38. 不存在绕过登录、验证码和付费墙的功能。
39. 不存在无限重试和无限采集。
40. 不存在虚假的阶段验收记录。

---

# 二十六、当前默认假设

在没有更多说明时，采用以下假设：

1. 第一版本为本地单用户管理系统。
2. 第一版本不实现登录。
3. 默认界面使用中文。
4. 默认时区使用系统本地时区。
5. 采集公开网页、RSS、Atom 和公开 API。
6. 不采集需要登录的内容。
7. 不绕过付费墙。
8. 不处理验证码。
9. 不使用代理池。
10. 不处理必须执行复杂 JavaScript 的页面。
11. 动态页面标记为不支持。
12. 默认遵守 robots.txt。
13. 默认同一来源请求间隔不少于两秒。
14. 默认保存清洗后的正文和原文链接。
15. 默认不保存原始 HTML。
16. 默认不下载第三方图片到本地。
17. 第一版本使用 H2 文件数据库。
18. 第一版本使用 Thymeleaf。
19. 第一版本使用 Spring `@Scheduled`。
20. 第一版本搜索采用数据库查询。
21. 默认文章按照发布时间倒序排列。
22. 无法识别发布时间时使用采集时间，并明确标记。
23. 同一文章可以属于多个主题。
24. 所有采集规则都必须通过管理后台配置。
25. 初始演示数据只能使用公开、稳定、适合测试的示例源。

将这些假设写入：

```text
docs/01-project/assumptions.md
```

---

# 二十七、Codex 当前执行任务

现在从 Stage 1 开始执行，不要直接完成所有阶段。

请依次完成：

1. 检查当前工作目录。
2. 检查当前目录是否已经是 Git 仓库。
3. 初始化 Git。
4. 创建 `.gitignore`。
5. 创建项目基础目录。
6. 创建文档目录。
7. 编写项目立项说明书。
8. 编写用户需求文档。
9. 编写产品需求文档。
10. 编写需求规格说明书。
11. 编写假设文档。
12. 创建项目总体迭代计划。
13. 创建：

```text
docs/stages/stage-1-report.md
```

14. 检查所有 Markdown 文档内容。
15. 执行 Git 状态检查。
16. 在环境允许的情况下提交：

```bash
git add .
git commit -m "stage-1: 完成项目立项与需求文档"
git tag stage-1
```

17. 汇报真实执行结果。

本次只完成 Stage 1。

不要提前实现 Stage 2 及后续代码。

---

# 二十八、本阶段输出格式

完成 Stage 1 后，按照以下格式输出：

## 1. 阶段目标

## 2. 完成内容

## 3. 新增文件

## 4. 关键需求结论

## 5. 默认假设

## 6. Git 状态

## 7. 测试或检查结果

## 8. 已知问题

## 9. 下一阶段计划

如果 Git 提交或标签创建失败，必须明确说明真实原因，不得声称已经完成。
