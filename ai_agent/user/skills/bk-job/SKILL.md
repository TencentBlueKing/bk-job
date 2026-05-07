---
name: bk-job
metadata: {"version":"0.1.0","space_id":"","bk_skill_code":"bk-job"}
description: 经 APIGW 调用蓝鲸作业平台（BK-Job）开放接口，支持检索定时任务与最近定时执行的状态/日志、执行方案搜索与带参启动、按实例 ID 查询任务状态与执行日志。列举类查询默认先最近 20 条；大列表在本地用 jq 或短脚本过滤后再摘要。含 plan-execute 操作确认门禁（G1–G4）、对用户输出规范与可选业务记忆 memory/businesses。技能包以含 SKILL.md 的目录为根，可部署在 Cursor、CodeBuddy、OpenClaw 等任意技能加载路径下，脚本与文档路径均相对该根目录。当用户提及作业平台、蓝鲸作业、定时任务、cron、执行方案、job_plan、job_instance、APIGW 调用作业接口、执行作业方案时使用。
compatibility: 依赖 Python 3（标准库即可）与环境变量 BK_JOB_ACCESS_TOKEN、BK_JOB_APIGW_BASE_URL。技能根目录定义为与本 SKILL.md 同级的文件夹（内含 scripts、reference、memory）；与宿主产品无关，复制整包到目标智能体的技能目录即可使用。
---

# 蓝鲸作业平台运维操作

## 概述

本技能指导AI Agent通过技能包内捆绑的 **Python 3 标准库** 脚本 [`scripts/job_apigw_client.py`](scripts/job_apigw_client.py) 调用蓝鲸 **API 网关** 上的作业平台接口，实现在作业平台内的各类运维操作。

### 技能根目录（多智能体通用）

**技能根目录** = **与本 `SKILL.md` 位于同一目录** 的文件夹（即同时包含 `scripts/`、`reference/`、`memory/` 的技能包，目录名与上文 `name` 一致，如 `bk-job`）。下文所有相对路径（如 `scripts/...`、`reference/...`）均相对于该目录解析。

部署时可将**整包**同步到任意宿主，例如：Cursor 的 `.cursor/skills/bk-job/`、CodeBuddy 的 `.codebuddy/skills/bk-job/`、OpenClaw 技能目录下的 `bk-job/`，或 AI-Agent 仓库 `common/bk-job/` 经 sync 后的目标路径。**不要求**固定放在 `.cursor` 下；宿主只需能把 `SKILL.md` 与捆绑文件一并加载，并在执行命令时使用**该目录**作为当前工作目录或脚本路径前缀。

## 何时应加载本技能

- 用户要通过 **APIGW** 查定时任务、最近一次定时执行、实例状态或日志。
- 用户要 **搜索 / 查看详情 / 启动** 执行方案（含变量与 dry-run）。
- 用户提到 **job_instance**、**cron**、**执行方案**、**bk_scope**、**access_token** 等与开放 API 相关的操作。

## 1. 脚本约定（跨平台与展示）

- **中文输出与 Windows**：脚本启动时将 **stdout/stderr 设为 UTF-8**，减轻控制台乱码；macOS/Linux 在已为 UTF-8 时兼容。仍异常时可用 `python -X utf8` 或将终端设为 UTF-8。
- **执行历史回溯**：`cron-last-run` 调用 `v4/get_job_instance_list` 时，`--lookback-days` **硬上限 31 天**（超出会截断并在 stderr 提示；JSON 中可见 `lookback_days_effective` / `lookback_days_max`）。这样避免无意拉取过长区间导致网关超时或响应过大。
- **定时任务启停**：`cron-search`、`cron-last-run` 展示的定时任务均带 **启停状态** 文案（网关约定：1 已启动、2 已暂停；其它数值需结合控制台核对）。

## 2. 列表策略与 Token（必读）

1. **「有哪些」类问题**：若用户未要求全量、导出或统计总数，**先只查一页**，使用 `cron-search` / `plan-search` 的默认 `--length`（20）或显式 `--length 20`，并用 `--keyword` 缩小范围。若 `total > length`，应说明「本页仅 N 条，共 M 条」，再询问是否翻页或增大 `length`。原因：避免默认把大列表塞进对话，浪费上下文且难读。
2. **大列表分析**：**不要把完整 JSON 贴进对话再分析**。应在本地用 **jq**、短 **`python -c`**，或在子进程中调脚本后只取必要字段（见 [listing-and-token-efficient-analysis.md](reference/manuals/listing-and-token-efficient-analysis.md)），回复中只放筛选结果、统计或少量行。

## 3. 渐进式披露（默认只读本文件）

详细流程、示例与排障按能力拆在 [`reference/manuals/`](reference/manuals/README.md)。**按任务只打开对应手册**，降低上下文占用。

| 能力 | 何时读 | 手册 |
|------|--------|------|
| 网关地址、令牌、`bk_scope` | 任何调用前 | [environment-and-auth.md](reference/manuals/environment-and-auth.md) |
| `cron-search` / `cron-last-run` | 定时任务、最近定时执行、日志 | [cron-tasks-and-last-execution.md](reference/manuals/cron-tasks-and-last-execution.md) |
| `plan-search` / `plan-detail` / `plan-execute` | 执行方案搜索与启动 | [job-plans-search-and-execute.md](reference/manuals/job-plans-search-and-execute.md) |
| `instance-status` | 已知作业实例 ID 查状态 | [job-instance-status.md](reference/manuals/job-instance-status.md) |
| 鉴权失败、无历史、状态码 | 报错或解释状态时 | [troubleshooting-and-status-codes.md](reference/manuals/troubleshooting-and-status-codes.md) |
| 列举默认条数、jq/脚本分析 | 「有哪些」或大 JSON | [listing-and-token-efficient-analysis.md](reference/manuals/listing-and-token-efficient-analysis.md) |
| **启动确认门禁、对用户输出格式** | 任何 `plan-execute`、交付列表/状态/日志 | [confirmation-and-output-protocol.md](reference/manuals/confirmation-and-output-protocol.md) |
| **业务/业务集记忆** | 预填 `bk_scope`、常用方案与参数、沉淀 | [business-memory.md](reference/manuals/business-memory.md) |

**索引**：[reference/manuals/README.md](reference/manuals/README.md)

## 4. 环境变量（摘要）

| 变量 | 说明 |
|------|------|
| `BK_JOB_ACCESS_TOKEN` | 用户态 access_token |
| `BK_JOB_APIGW_BASE_URL` | 网关根 URL，可拼 `/api/v3/...`、`/api/v4/...` |

详见 [environment-and-auth.md](reference/manuals/environment-and-auth.md)。可用 `--access-token`、`--base-url` 覆盖命令行。

## 5. 全局规则（摘要）

- **`plan-execute`（非 `--dry-run`）**：须满足 [confirmation-and-output-protocol.md](reference/manuals/confirmation-and-output-protocol.md) **第 1 节** — **G1–G4 门禁**，尤其 **G2**：先按 **1.4** 展示确认摘要，**再等待用户下一条独立回复**后才可执行。「立即启动」等只表达意图，**不算**确认。原因：避免误触生产执行。
- **对用户的输出**：同文件 **第 2 节** — 不叙述调脚本/调 API 的过程；表格化交付；启动链路不在同一轮内既「摘要」又「真实执行」。原因：用户需要结论与可核对信息，而非实现细节。
- **关键词歧义**：多匹配时脚本退出并列出候选项；需 `--cron-id` / `--job-plan-id` 或知情下 `--pick-first`。详见各手册。
- **子命令与示例**：[job-plans-search-and-execute.md](reference/manuals/job-plans-search-and-execute.md)；`--dry-run` 可与摘要配合校验请求体。
- **业务记忆**：[business-memory.md](reference/manuals/business-memory.md) — 加载 `memory/businesses/` 下对应范围文件以预填上下文；写入须用户确认，敏感信息勿入库。

## 6. 子命令一览

| 子命令 | 手册 |
|--------|------|
| `cron-search`、`cron-last-run` | [cron-tasks-and-last-execution.md](reference/manuals/cron-tasks-and-last-execution.md) |
| `plan-search`、`plan-detail`、`plan-execute` | [job-plans-search-and-execute.md](reference/manuals/job-plans-search-and-execute.md) |
| `instance-status` | [job-instance-status.md](reference/manuals/job-instance-status.md) |

## 7. 网关字段级 API 说明

OpenAPI 级参数与响应见 [`reference/apidocs/`](reference/apidocs/) 各文件；手册侧重**脚本组合与注意点**，不重复全量字段表。

## 8. 捆绑脚本说明（Python）

本技能的业务客户端为 **Python 3 标准库**，无额外 pip 依赖，与蓝鲸侧接口版本已对齐。`bk-skill-creator` 对「新建技能内自动化脚本优先 Node」的通则，适用于从零编写的通用脚本；此处保留 Python 是为了 **完整延续既有 `job_apigw_client` 行为与运维习惯**，避免重复实现与行为漂移。

运行示例（将终端当前目录设为技能根目录，或使用脚本绝对路径）：

```bash
cd /path/to/bk-job
python scripts/job_apigw_client.py --help
```

```bash
python /path/to/bk-job/scripts/job_apigw_client.py cron-search --bk-scope-id 309 --length 20
```

## AIDev 与校验

上传或打包前，在已克隆的 `bk-skill-creator` 仓库中执行（将参数换成本机 **bk-job 技能根目录** 的绝对路径）：

```bash
cd /path/to/bk-skill-creator
python -m scripts.quick_validate "/path/to/bk-job"
```

`metadata.version` / `space_id` 可在首次 `bkai upload` 后由 CLI 写回。
