---
name: bk-job
description: 经 APIGW 调用蓝鲸作业平台（BK-Job）开放接口：查搜定时任务、作业模板、执行方案与作业执行历史、实例状态日志，创建执行方案、新建与启停定时任务，到指定机器快速执行脚本，分发文件到目标机器（仅服务器/本地文件），查主机拓扑与执行账号。含写操作确认门禁、先选业务范围、先查主机与账号等规范。当用户提及作业平台、业务、业务集、蓝鲸作业、定时任务、cron、执行方案、作业模板、job_plan、job_instance、执行历史、快速执行脚本、fast_execute_script、文件分发、fast_transfer_file、主机、搜索主机、执行账号、bk_scope、APIGW 调用作业接口时使用。不适用于 Web 界面操作、非 APIGW 调用及 CMDB、监控等其它蓝鲸产品。
compatibility: 依赖 Python 3（标准库即可）；访问令牌优先经 imate 的 ai-hub 命令获取，回退环境变量 BK_JOB_ACCESS_TOKEN；API 网关与页面根 URL 在技能根目录 config.yaml 中配置，部署时修改该文件即可。
metadata: {"version":"1.0.0","bk_skill_code":"bk-job","openclaw":{"displayName":"蓝鲸作业平台","requires":{"env":["BK_JOB_ACCESS_TOKEN"]},"primaryEnv":"BK_JOB_ACCESS_TOKEN"}}
---

# 蓝鲸作业平台运维操作

通过技能包内脚本 [`scripts/job_apigw_client.py`](scripts/job_apigw_client.py) 调用蓝鲸 **API 网关** 上的作业平台接口完成运维操作。

## 核心概念

- **资源范围**：一切操作的前提，由 `bk_scope_type`（`biz` 业务 / `biz_set` 业务集）与 `bk_scope_id` 组成。
- **作业对象关系**：模板派生执行方案；方案可直接启动，也可由定时任务周期触发；每次执行产生作业实例，状态与日志按实例 ID 查。
- **渐进式披露**：本文件常驻上下文，细节按任务再读手册；包结构与手册索引见 [手册 README](references/manuals/README.md)。

## 前置检查

1. **URL 配置**：脚本从 [`config.yaml`](config.yaml) 读 `apigw_base_url` 与 `job_base_url`，**不读环境变量**。
2. **访问令牌**：脚本按 `--access-token` → `ai-hub`（imate）→ `BK_JOB_ACCESS_TOKEN` 自动获取，**智能体勿自行取令牌或回显**。见 [鉴权手册](references/manuals/environment-and-auth.md)。
3. **资源范围**：无 `bk_scope` 上下文且无业务记忆时，先用 `list-authorized-scopes` 列出有权限的业务/业务集供选择，**勿擅自猜 `bk_scope_id`**；选定后可沉淀业务记忆（写入须确认）。

## 核心规则（必读）

- **写操作须过 G1–G4 门禁**：`plan-execute`、`fast-execute-script`、`fast-transfer-file`、`plan-create`、`cron-save`、`cron-update-status`（非 `--dry-run`）须先展示确认摘要，**再等用户下一条独立回复**才执行；「立即执行」只表达意图，**不算**确认。**一次确认只授权一次执行**，重复执行（含「相同参数再执行一次」）须重新走门禁，不得跳过。格式与反例见 [确认门禁](references/manuals/confirmation-and-output-protocol.md)。
- **填主机先查再填**：需要目标机（含分发源机）而用户未给 `bk_host_id` 或 `bk_cloud_id:ip` 时，先用 `host-topo-tree`、`host-search` 定位，列候选经用户确认，**不要凭空猜主机 ID**。
- **填账号先查再填**：需要执行账号而用户未指定时，先用 `account-list` 列出该范围可用账号供选择，**不要凭空猜账号别名**。
- **文件分发仅两种源**：只支持「服务器文件」与「本地文件」；第三方文件源（如 COS）未提供接口，**不要**给该选项，脚本会拒绝。
- **列表先查一页**：默认 `--length 20` 并用 `--keyword` 缩小，`total > length` 时先说明「本页 N 条，共 M 条」再问翻页；大列表用 jq 过滤，**勿把整页 JSON 贴进对话**。见 [列举与分析](references/manuals/listing-and-token-efficient-analysis.md)。
- **对用户输出**：不叙述调脚本/调 API 过程，表格化交付结论；同一轮内不得既给摘要又真实执行。
- **临时文件只放技能 `tmp/`**：内联 JSON 在 PowerShell 易转义失败，改用 `--*-file` 入参；这类中间文件一律写 `tmp/`，用完即清，且**只许清 `tmp/` 内容**，严禁删其它路径。见 [临时文件](references/manuals/temp-files.md)。
- **主机变量结构因接口而异**：`plan-create` 与 `plan-execute`/`cron-save` 字段名不同，组装前先查手册差异表。
- **切换业务后重查资源**：切换业务后必须重新查询拓扑、主机、账号、方案、定时任务，禁止复用上一业务的资源。
- **必给结果链接**：无论成败，触发后须以可点击链接交付 `job_instance_url`（执行类）或 `job_plan_url`（建方案）。

## 支持的原子能力

| 能力 | 子命令 | 手册 |
|------|--------|------|
| 范围选择 | `list-authorized-scopes` | [手册](references/manuals/scope-selection-and-onboarding.md) |
| 主机查询 | `host-topo-tree`、`host-search` | [手册](references/manuals/host-query-and-selection.md) |
| 账号查询 | `account-list` | [手册](references/manuals/account-query-and-selection.md) |
| 定时任务 | `cron-search`、`cron-last-run` | [手册](references/manuals/cron-tasks-and-last-execution.md) |
| 模板与创建 | `template-search`、`template-detail`、`plan-create`、`cron-save`、`cron-update-status` | [手册](references/manuals/job-plans-create-and-cron.md) |
| 方案与启动 | `plan-search`、`plan-detail`、`plan-execute` | [手册](references/manuals/job-plans-search-and-execute.md) |
| 快速执行脚本 | `fast-execute-script` | [手册](references/manuals/fast-execute-script.md) |
| 文件分发 | `fast-transfer-file`、`gen-local-upload-url`、`upload-local-file` | [手册](references/manuals/file-transfer.md) |
| 执行历史与日志 | `instance-list`、`instance-status`、`get-instance-log` | [手册](references/manuals/job-instance-status.md) |
| 业务记忆 | `memory-load` | [手册](references/manuals/business-memory.md) |

字段级参数见 [`references/apidocs/`](references/apidocs/)，全部参数用 `--help` 查看。

## 常用组合工作流程

下列**只是常见示例，非能力边界**：可按需用上表原子能力自由组装；但写操作一律走 G1–G4 门禁，不得绕过。「确认」均指门禁两轮确认；参数见对应手册。

- **快速执行脚本**：`host-search` 定位目标机 → `account-list` 选账号 → `--dry-run` 出摘要 → 确认 → 执行。
- **分发本地文件**：`gen-local-upload-url` → `upload-local-file` 上传 → `fast-transfer-file` 引用返回的 `path` 分发；前两步不过门禁。
- **分发服务器文件**：定位源机与源账号 → 定位目标机、目标账号与目标目录 → `fast-transfer-file --server-file-list <源机上的绝对路径>` `--dry-run` → 确认 → 分发。
- **查模板建方案**：`template-search` → `template-detail` 看步骤与全局变量 → 与用户确认启用步骤及变量 → `plan-create --dry-run` → 确认 → 创建。
- **搜方案并启动**：`plan-search` → `plan-detail` 核对必填与主机变量 → 组装 `global_var_list` → `plan-execute --dry-run` → 确认 → 启动。
- **建定时任务并启用**：`plan-search` 定位方案 → 确认 cron 表达式与时区 → `cron-save --dry-run` → 确认 → 保存（默认暂停）→ **再问是否启用** → `cron-update-status --status 1`。
- **查定时任务与执行历史**：`cron-search` 找任务（须展示启停状态）→ `cron-last-run` 取最近一次定时执行的状态与各步骤日志。
- **查执行历史**：`instance-list` 按时间窗口与状态/类型/执行人筛出实例 → `instance-status` 看状态 → `get-instance-log` 取步骤日志。

## 异常处理

- **关键词歧义**：多条匹配时脚本列候选并退出，需补 `--cron-id`/`--job-plan-id`，或知情下用 `--pick-first`。
- **鉴权失败、无历史、状态码含义**：见 [排障手册](references/manuals/troubleshooting-and-status-codes.md)。
- **回溯上限**：`cron-last-run`、`instance-list` 最多回溯 **31 天**，超出会截断并提示。
