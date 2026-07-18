# Changelog

本项目采用阶段标签记录可运行里程碑。正式版本发布后将遵循 [Semantic Versioning](https://semver.org/)。

## [Unreleased]

- 准备 GitHub 开源发布结构、文档导航、License、安全策略和贡献指南。

## [1.0.0] - 2026-07-18

### Added

- 主题、公开采集源、CSS 规则、定时调度和采集任务管理。
- RSS、Atom、HTML List、JSON API、Manual URL Provider 与安全正文清洗。
- URL 去重、主题匹配、质量评估、审核、全文搜索和阅读管理。
- 采集源健康检查、AI 自动发现来源、任务过滤和规则化归档。
- Ollama 文章分析、多轮聊天和 AI 材料待审核入库。
- H2/Flyway、本地备份恢复、Docker Compose 和 Android 离线资料包。

### Security

- 默认仅监听回环地址。
- URL 安全校验、响应大小/超时限制、TLS 证书验证和安全 HTML 清洗。
- 不提供绕过登录、验证码、robots.txt、付费墙或证书验证的能力。

各阶段明细见[阶段报告索引](docs/stages/README.md)。
