# bk-job 技能手册索引（渐进式披露）

主入口为仓库内 [`../../SKILL.md`](../../SKILL.md)。**仅在用户任务涉及对应能力时**再读取下列手册，避免一次性加载全部细节。

| 手册 | 适用场景 |
|------|----------|
| [environment-and-auth.md](environment-and-auth.md) | 配置网关地址、令牌、资源范围 `bk_scope` |
| [listing-and-token-efficient-analysis.md](listing-and-token-efficient-analysis.md) | **列举默认先 20 条**、大列表用 jq/脚本本地过滤、节省 Token |
| [confirmation-and-output-protocol.md](confirmation-and-output-protocol.md) | **plan-execute 确认门禁（G1–G4）**、摘要格式、**输出规范**（结构化交付、禁止过程叙述） |
| [business-memory.md](business-memory.md) | **业务/业务集记忆**：路径、`memory/businesses/`、加载流程、沉淀触发与写入规则 |
| [cron-tasks-and-last-execution.md](cron-tasks-and-last-execution.md) | 定时任务关键词检索、`cron-last-run` 最近执行与日志 |
| [job-plans-search-and-execute.md](job-plans-search-and-execute.md) | 执行方案搜索、详情、`plan-execute` 与确认协议 |
| [job-instance-status.md](job-instance-status.md) | 已知 `job_instance_id` 时查状态 |
| [troubleshooting-and-status-codes.md](troubleshooting-and-status-codes.md) | 鉴权失败、无历史、状态码对照 |

网关字段级说明仍以 [`../apidocs/`](../apidocs/) 下各接口文档为准；手册描述**脚本行为、组合流程与注意事项**，不重复 OpenAPI 全表。
