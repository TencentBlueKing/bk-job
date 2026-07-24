---
name: bk-job
description: 经 APIGW 调用蓝鲸作业平台（BK-Job）开放接口，支持检索定时任务与最近定时执行的状态/日志、作业模板搜索与详情查询、执行方案搜索/创建/带参启动、定时任务新建与启停、按实例 ID 查询任务状态与执行日志。列举类查询默认先最近 20 条；大列表在本地用 jq 或短脚本过滤后再摘要。含 plan-execute/plan-create/cron-save 操作确认门禁（G1–G4 及创建后启用询问）、对用户输出规范与可选业务记忆 memory/businesses。技能包以含 SKILL.md 的目录为根，可部署在 Cursor、CodeBuddy、OpenClaw 等任意技能加载路径下，脚本与文档路径均相对该根目录。当用户提及作业平台、蓝鲸作业、定时任务、cron、执行方案、作业模板、job_template、job_plan、job_instance、APIGW 调用作业接口、执行作业方案、创建执行方案、保存定时任务时使用。不适用于：作业平台 Web 界面/前端交互操作、非 APIGW（如直连微服务或其它接入方式）调用、以及作业平台之外的蓝鲸产品（CMDB、监控、节点管理等）。
compatibility: 依赖 Python 3（标准库即可）与环境变量 BK_JOB_ACCESS_TOKEN；API 网关与作业平台页面根 URL 在技能根目录 config.yaml 中配置，部署时修改该文件即可。
metadata: {"version":"1.0.0","bk_skill_code":"bk-job","openclaw":{"displayName":"蓝鲸作业平台","requires":{"env":["BK_JOB_ACCESS_TOKEN"]},"primaryEnv":"BK_JOB_ACCESS_TOKEN"}}
---

# 蓝鲸作业平台运维操作

通过技能包内捆绑的 **Python 3 标准库** 脚本 [`scripts/job_apigw_client.py`](scripts/job_apigw_client.py) 调用蓝鲸 **API 网关** 上的作业平台接口，完成查询、搜索、创建、启动、定时任务编排与状态/日志查看。

**技能根目录** = 与本 `SKILL.md` 同级的目录（名为 `bk-job`），下文相对路径均相对它解析；整包可同步到任意宿主（`.cursor/skills/`、`.codebuddy/skills/`、OpenClaw 技能目录等），不要求固定在 `.cursor` 下。

### 目录结构（顶层项用途）

| 路径 | 用途 |
|------|------|
| `scripts/job_apigw_client.py` | 唯一可执行脚本：API 调用与参数校验 |
| `config.yaml` | 部署配置：网关与页面根 URL（仅占位示例，部署时替换） |
| `reference/manuals/` | 渐进式披露手册（按任务按需加载） |
| `reference/apidocs/` | 网关字段级 API 文档 |
| `memory/businesses/` | 可选业务记忆（脚本自动附加 `_business_memory`） |

## 何时加载

通过 **APIGW** 查/搜/建/启 定时任务、执行方案、作业模板、作业实例状态或日志，或用户提到 **job_template / job_instance / cron / 执行方案 / bk_scope / access_token** 等开放 API 操作。

## 1. 核心规则（必读）

- **列表「有哪些」**：未要求全量/导出/统计时**先只查一页**（默认 `--length 20`）并用 `--keyword` 缩小；`total > length` 时说明「本页 N 条，共 M 条」再问翻页。大列表用 **jq / `python -c`** 本地过滤，**勿把整页 JSON 贴进对话**（见 [listing-and-token-efficient-analysis.md](reference/manuals/listing-and-token-efficient-analysis.md)）。
- **写操作**（`plan-execute` / `plan-create` / `cron-save` / `cron-update-status`，非 `--dry-run`）：须过 [confirmation-and-output-protocol.md](reference/manuals/confirmation-and-output-protocol.md) **第 1 节** 的 **G1–G4 门禁**，尤其 **G2**——先展示确认摘要，**再等用户下一条独立回复**才执行；「立即启动/启用」只是意图，**不算**确认。`cron-save` 创建后**必须询问是否启用**，未确认前**禁止** `cron-update-status --status 1`。
- **对用户输出**：不叙述调脚本/调 API 过程；表格化交付结论；启动链路不在同一轮内既「摘要」又「真实执行」。
- **关键词歧义**：多匹配时脚本退出并列候选，需 `--cron-id` / `--job-plan-id`，或知情下 `--pick-first`。
- **PowerShell**：传 JSON 参数易转义失败，统一改用 `--variables-file` / `--global-vars-file`。
- **主机变量字段名因接口而异**：`plan-create` 用 `execute_target.host_list`；`plan-execute` / `cron-save` 用 `server.ip_list`/`host_id_list`；均须同时给 `bk_cloud_id` 与 `ip`。完整差异表见 [job-plans-create-and-cron.md](reference/manuals/job-plans-create-and-cron.md)。
- **其它**：`cron-last-run` 回溯**硬上限 31 天**（超出截断并提示）；脚本启动将 stdout/stderr 设为 **UTF-8**。

## 2. 渐进式披露（默认只读本文件，按任务再开手册）

| 能力 / 子命令 | 手册 |
|------|------|
| 网关地址、令牌、`bk_scope`（任何调用前） | [environment-and-auth.md](reference/manuals/environment-and-auth.md) |
| `cron-search` / `cron-last-run` | [cron-tasks-and-last-execution.md](reference/manuals/cron-tasks-and-last-execution.md) |
| `template-search` / `template-detail` / `plan-create` / `cron-save` / `cron-update-status` | [job-plans-create-and-cron.md](reference/manuals/job-plans-create-and-cron.md) |
| `plan-search` / `plan-detail` / `plan-execute` | [job-plans-search-and-execute.md](reference/manuals/job-plans-search-and-execute.md) |
| `instance-status` | [job-instance-status.md](reference/manuals/job-instance-status.md) |
| `memory-load`、业务记忆预填与沉淀 | [business-memory.md](reference/manuals/business-memory.md) |
| 确认门禁、对用户输出格式 | [confirmation-and-output-protocol.md](reference/manuals/confirmation-and-output-protocol.md) |
| 列举默认条数、jq/脚本分析 | [listing-and-token-efficient-analysis.md](reference/manuals/listing-and-token-efficient-analysis.md) |
| 鉴权失败、无历史、状态码 | [troubleshooting-and-status-codes.md](reference/manuals/troubleshooting-and-status-codes.md) |
| 字段级 API 参数/响应 | [`reference/apidocs/`](reference/apidocs/) |

**索引**：[reference/manuals/README.md](reference/manuals/README.md)

## 3. 配置、鉴权与结果链接（摘要）

- **URL 配置**：脚本从技能根目录 [`config.yaml`](config.yaml) 读取 `apigw_base_url`、`job_base_url`（**不读环境变量**）；代码库内 URL **仅为占位**，部署时替换为真实地址。
- **访问令牌**：`BK_JOB_ACCESS_TOKEN`（环境变量）或 `--access-token`，**不落盘**。详见 [environment-and-auth.md](reference/manuals/environment-and-auth.md)。
- **成功结果链接**（脚本自动补充，须以可点击链接交付用户）：`plan-execute` → `job_instance_url` = `{job_base_url}/api_execute/{job_instance_id}`；`plan-create` → `job_plan_url` = `{job_base_url}/api_plan/{job_plan_id}`。
- **业务记忆**：带 `--bk-scope-id` 的子命令自动在 JSON 附加 `_business_memory`（取自 `memory/businesses/`，无则 `loaded:false`）；可用 `memory-load` 单独加载或 `--no-business-memory` 关闭。写入须用户确认。详见 [business-memory.md](reference/manuals/business-memory.md)。

## 4. 脚本与校验

Python 3 标准库（无 pip 依赖），脚本仅做 API 调用与参数校验。常用与校验命令：

```bash
python scripts/job_apigw_client.py --help
python scripts/job_apigw_client.py cron-search --bk-scope-id 2 --length 20
# 打包/上传前在 bk-skill-creator 仓库内校验（参数为本机 bk-job 根目录绝对路径）
python -m scripts.quick_validate "/path/to/bk-job"
```

