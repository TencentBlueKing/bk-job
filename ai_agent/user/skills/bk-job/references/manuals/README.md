# bk-job 技能手册索引（渐进式披露）

主入口为仓库内 [`../../SKILL.md`](../../SKILL.md)。**仅在用户任务涉及对应能力时**再读取下列手册，避免一次性加载全部细节。

## 技能包结构

技能根目录为与 `SKILL.md` 同级的目录，包内所有相对路径均相对它解析；整包可放入 `.cursor/skills/`、`.codebuddy/skills/`、OpenClaw 等任意宿主。

| 路径 | 用途 |
|------|------|
| `SKILL.md` | 主入口：核心概念、前置检查、核心规则、原子能力与工作流 |
| `scripts/job_apigw_client.py` | 唯一可执行脚本，负责 API 调用与参数校验 |
| `config.yaml` | 部署配置：`apigw_base_url` 与 `job_base_url`（仓库内为占位，部署时替换） |
| `references/manuals/` | 本目录：按任务按需加载的手册 |
| `references/apidocs/` | 字段级 API 文档 |
| `tmp/` | **运行期临时文件的唯一存放位置**，用完即清；见 [temp-files.md](temp-files.md) |
| `memory/businesses/` | 可选业务记忆；带 `--bk-scope-id` 的命令自动附加 `_business_memory`，`--no-business-memory` 可关闭 |

打包上传前可在 bk-skill-creator 仓库内执行 `python -m scripts.quick_validate "<bk-job 根目录绝对路径>"` 校验技能包。

## 手册索引

| 手册 | 适用场景 |
|------|----------|
| [environment-and-auth.md](environment-and-auth.md) | 配置网关地址、令牌、资源范围 `bk_scope` |
| [scope-selection-and-onboarding.md](scope-selection-and-onboarding.md) | **首次引导选择业务/业务集**：`list-authorized-scopes`、无 `bk_scope` 上下文时的选择流程 |
| [host-query-and-selection.md](host-query-and-selection.md) | **执行类操作填写主机**：`host-topo-tree` 看拓扑、`host-search` 按 IP/主机名/操作系统/拓扑节点搜索主机 |
| [account-query-and-selection.md](account-query-and-selection.md) | **执行类操作填写账号**：`account-list` 查询业务下可用执行账号（系统/DB），引导选择 alias/id |
| [listing-and-token-efficient-analysis.md](listing-and-token-efficient-analysis.md) | **列举默认先 20 条**、大列表用 jq/脚本本地过滤、节省 Token |
| [temp-files.md](temp-files.md) | **临时文件只放技能 `tmp/`**、命名约定、清理命令与清理红线（只清 `tmp/`） |
| [confirmation-and-output-protocol.md](confirmation-and-output-protocol.md) | **plan-execute / fast-execute-script 确认门禁（G1–G4）**、摘要格式、**输出规范**（结构化交付、禁止过程叙述） |
| [business-memory.md](business-memory.md) | **业务/业务集记忆**：路径、`memory/businesses/`、加载流程、沉淀触发与写入规则 |
| [cron-tasks-and-last-execution.md](cron-tasks-and-last-execution.md) | 定时任务关键词检索、`cron-last-run` 最近执行与日志 |
| [job-plans-create-and-cron.md](job-plans-create-and-cron.md) | **`template-search`** / **`template-detail`** / **`plan-create`** / **`cron-save`** / **`cron-update-status`** 及创建后启用询问 |
| [job-plans-search-and-execute.md](job-plans-search-and-execute.md) | 执行方案搜索、详情、`plan-execute` 与确认协议 |
| [fast-execute-script.md](fast-execute-script.md) | **`fast-execute-script`** 到指定机器快速执行脚本（写操作，须过确认门禁） |
| [file-transfer.md](file-transfer.md) | **`fast-transfer-file`** / `gen-local-upload-url` / `upload-local-file` 分发文件（仅服务器文件与本地文件，写操作，须过确认门禁） |
| [job-instance-status.md](job-instance-status.md) | **执行历史**（`instance-list`）、实例状态（`instance-status`）与步骤执行日志（`get-instance-log`） |
| [troubleshooting-and-status-codes.md](troubleshooting-and-status-codes.md) | 鉴权失败、无历史、状态码对照 |

网关字段级说明仍以 [`../apidocs/`](../apidocs/) 下各接口文档为准；手册描述**脚本行为、组合流程与注意事项**，不重复 OpenAPI 全表。
