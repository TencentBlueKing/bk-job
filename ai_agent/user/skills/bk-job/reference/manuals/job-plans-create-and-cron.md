# 执行方案创建与定时任务编排

在已有「搜索 / 详情 / 启动」能力基础上，本手册覆盖 **模板检索与详情** → **创建执行方案** → **新建/保存定时任务** → **更新定时任务启停状态** 的组合工作流。

涉及 **`bk_scope` 预填** 时见 [business-memory.md](business-memory.md)；写操作确认门禁见 [confirmation-and-output-protocol.md](confirmation-and-output-protocol.md)。

## 1. 子命令

| 子命令 | 接口 | 说明 |
|--------|------|------|
| `template-search` | `GET /api/v3/get_job_template_list` | 按名称模糊匹配定位作业模板（`--keyword`），可选 `--creator` 过滤 |
| `template-detail` | `GET /api/v4/get_job_template_detail` | 拉取模板**全部步骤**与**全局变量**，作为 `plan-create` 参数源 |
| `plan-create` | `POST /api/v4/create_job_plan` | 基于作业模板创建执行方案 |
| `cron-save` | `POST /api/v3/save_cron` | 新建或更新定时任务（绑定 `job_plan_id`） |
| `cron-update-status` | `POST /api/v3/update_cron_status` | 启动（1）或暂停（2）定时任务 |

## 2. 推荐工作流

### 2.1 创建执行方案（完整流程）

```
[A] 定位目标作业模板
     └─ template-search --keyword "..."     （多条时选定或追加 --pick 指定）
              ↓
[B] 拉取模板详情，得到步骤列表与全局变量
     └─ template-detail --job-template-id <模板ID>
              ↓
[C] 与用户确认：
     - 启用哪些步骤 → enable_steps（来源：step_list[].id）
     - 全局变量覆盖 → variables   （来源：global_var_list[].name；含必填变量的取值或 follow_template）
              ↓
[D] plan-create --dry-run 展示请求体 + 操作摘要
              ↓
⛔ 等待用户独立确认（confirmation-and-output-protocol G2）
              ↓
[E] plan-create（真实执行）→ 得到 job_plan_id
```

**为什么必须先 template-detail**：
- `enable_steps` 是模板**已存在**步骤 ID 的子集，列表中任意 ID 不属于该模板即报错。
- `variables` 按 `name` 与模板全局变量匹配，名称错误或类型不匹配即报错；密文与执行目标变量的填写方式不同。
- 不预先看详情就尝试写 `enable_steps` / `variables` 等同于盲填，几乎必然失败或写出语义错误的方案。

### 2.2 创建定时任务（完整流程）

```
[A] 定位执行方案
     └─ plan-search --keyword "..."         （或直接给 --job-plan-id）
              ↓
[B] 与用户确认：cron 表达式 / 单次执行时间、时区、是否覆盖默认全局变量
              ↓
[C] cron-save --dry-run 展示请求体 + 操作摘要
              ↓
⛔ 等待用户独立确认
              ↓
[D] cron-save → 得到 cron_id（**新建默认 status=2 暂停**）
              ↓
⛔ 必须再次询问用户：「是否立即启用该定时任务？」
     未获明确确认前禁止 cron-update-status --status 1
              ↓
[E] 用户确认启用 → cron-update-status --status 1
```

### 2.3 与只读能力的衔接

- 创建方案前可用 `plan-search` 确认是否已有同名方案，避免重复创建。
- 定时变量结构可参考 `plan-detail` 的 `global_var_list`（执行方案侧）或 `template-detail` 的 `global_var_list`（模板侧），二者结构基本一致。
- 创建定时任务后可用 `cron-search` 核对列表，或用 `cron-last-run` 观察首次定时执行（须任务已启用且到达触发时间）。

## 3. 写操作确认协议（必须）

### 3.1 `plan-create`（非 `--dry-run`）

须向用户展示确认摘要，至少包含：

- 资源范围（`bk_scope_type` / `bk_scope_id`）
- 作业模板（`job_template_id` + 名称，名称建议直接来自 `template-detail` 返回）
- 执行方案名称（`name`）
- 启用步骤：以「ID — 名称（type 中文）」逐行列出（基于 `template-detail` 的 `step_list`），未传 `enable_steps` 时声明「使用模板全部步骤」
- 全局变量覆盖：表格列出 `name` / 来源（用户提供 / 模板默认 / `follow_template`），敏感值脱敏

**等待用户下一条独立确认回复** 后才可真实调用（门禁精神同 `plan-execute` G2）。

### 3.2 `cron-save`（非 `--dry-run`）

创建成功后脚本会在 JSON 中附带 `_note`：**新建默认为暂停（status=2）**。

智能体 **必须** 在交付创建结果后询问用户：「是否立即启用该定时任务？」

- **未获用户明确确认前，禁止** 调用 `cron-update-status --status 1`。
- 用户确认启用后，再调用 `cron-update-status`；仍建议先 `--dry-run` 核对请求体。

### 3.3 `cron-update-status`（非 `--dry-run`）

| status | 含义 |
|--------|------|
| 1 | 启动 |
| 2 | 暂停 |

启用（status=1）属于写操作：须在用户**明确同意启用**后才可调用。暂停操作若由用户主动要求（如「先停掉这个定时任务」），可按用户意图执行，但仍建议摘要 cron ID 与目标状态。

## 4. 参数要点

### `template-search`

| 参数 | 说明 |
|------|------|
| `--keyword` | 模板名称模糊匹配；强烈建议带上以避免拉回过长列表（默认 `--length 20`） |
| `--creator` | 可选，按创建人精确过滤 |
| `--start` / `--length` | 分页参数；遵循「先一页」策略，必要时再翻页 |

### `template-detail`

| 参数 | 说明 |
|------|------|
| `--job-template-id` | 必填；通常由 `template-search` 选定后传入 |

**关注字段（用于下游 `plan-create`）**：
- `step_list[].id`、`step_list[].name`、`step_list[].type`：用于交互确认启用步骤
- `global_var_list[].name`、`global_var_list[].type`、`global_var_list[].required`、`global_var_list[].value`、`global_var_list[].execute_target`：用于交互确认变量取值与 `follow_template` 策略

### `plan-create`

| 参数 | 说明 |
|------|------|
| `--job-template-id` | 必填，须已存在的模板 |
| `--name` | 必填，长度 1～60；同范围、同模板下唯一 |
| `--enable-steps` | 可选 JSON 数组；不传时使用模板全部步骤；不可传空数组；ID 须均属于该模板（取自 `template-detail` 的 `step_list[].id`） |
| `--variables` | 可选 JSON 数组；按 `name` 与模板变量匹配；执行目标类变量（type=3）用 `execute_target.host_list` 结构，详见 [`../apidocs/v4_create_job_plan.md`](../apidocs/v4_create_job_plan.md) |
| `--variables-file` | 推荐：从文件读取 `--variables` 的 JSON（避免命令行转义问题，PowerShell 尤其需要），如 `variables.json` |

> **⚠️ PowerShell 用户注意**：传递 JSON 参数时，建议始终使用 `--variables-file` 或 `--global-vars-file`，避免命令行转义失败。
>
> **⚠️ 字段名注意（重要！）**：
> - `template-detail` 返回 + `plan-create` 传入：type=3 变量使用 `execute_target.host_list`
> - `plan-execute` 传入：type=3 变量使用 `server.ip_list`（与 plan-create 不同！）
> - 主机信息必须同时提供 `bk_cloud_id`（云区域ID）和 `ip`，否则报错"目标执行对象为空"

响应 `data.job_plan_id` 为新建方案 ID。**创建成功后**脚本会补充 `job_plan_url`（`{job_base_url}/api_plan/{job_plan_id}`，见 [`config.yaml`](../../config.yaml) 的 `job_base_url`）；智能体须以可点击链接交付给用户。

### `cron-save`

| 参数 | 说明 |
|------|------|
| `--job-plan-id` | 必填 |
| `--name` | 新建时必填 |
| `--expression` / `--execute-time` | 新建时二选一；`expression` 为 crontab（分 时 日 月 周）；`execute-time` 为 Unix 时间戳（秒） |
| `--execute-time-zone` | 可选，IANA 时区，如 `Asia/Shanghai` |
| `--cron-id` | 更新已有任务时传入 |
| `--global-vars` | 可选 JSON 数组，结构与 `execute_job_plan` 的 `global_var_list` 类似 |
| `--global-vars-file` | 推荐：从文件读取 `--global-vars` 的 JSON（避免命令行转义问题） |

> **⚠️ 字段名注意（重要！）**：
> - `plan-create` 传入：type=3 变量使用 `execute_target.host_list`
> - `cron-save` 传入：type=3 变量使用 `server.ip_list` 或 `server.host_id_list`（与 plan-create 不同！）
> - `plan-execute` 传入：type=3 变量使用 `server.ip_list` 或 `server.host_id_list`（与 cron-save 一致）
> - 主机信息必须同时提供 `bk_cloud_id`（云区域ID）和 `ip`，否则报错"目标执行对象为空"
>
> **字段名差异总结**：
> | API | 类型3变量字段名 | 主机列表字段名 |
> |-----|----------------|---------------|
> | `plan-create` | `execute_target` | `host_list` |
> | `plan-execute` | `server` | `ip_list` 或 `host_id_list` |
> | `cron-save` | `server` | `ip_list` 或 `host_id_list` |

### `cron-update-status`

| 参数 | 说明 |
|------|------|
| `--cron-id` | 必填 |
| `--status` | 必填，`1` 启动、`2` 暂停 |

## 5. 命令示例

### 5.1 创建执行方案

```bash
# A. 按关键词搜模板
python scripts/job_apigw_client.py template-search \
  --bk-scope-id <业务ID> --keyword "deploy"

# B. 拉取模板详情（看 step_list 与 global_var_list）
python scripts/job_apigw_client.py template-detail \
  --bk-scope-id <业务ID> --job-template-id 1000

# C. 将变量 JSON 写入文件（避免命令行转义问题）
cat > /tmp/variables.json << 'EOF'
[
  {"name":"HOST_TARGET","execute_target":{"host_list":[{"bk_cloud_id":0,"ip":"127.0.0.1"}]}},
  {"name":"TARGET_DIR","value":"/data/release","follow_template":false}
]
EOF

> **⚠️ 字段名关键区别（重要！）**：
> - `template-detail` 返回 + `plan-create` 传入：type=3 变量使用 `execute_target.host_list`
> - `plan-execute` 传入：type=3 变量使用 `server.ip_list`（与 plan-create 不同！）
> - 主机必须同时提供 `bk_cloud_id`（云区域ID）和 `ip`

# D. dry-run 校验请求体（--enable-steps 可选，不传则启用模板全部步骤）
python scripts/job_apigw_client.py plan-create \
  --bk-scope-id <业务ID> \
  --job-template-id 1000 \
  --name "api-plan-demo" \
  --enable-steps '[101,102]' \
  --variables-file /tmp/variables.json \
  --dry-run

# E. 用户独立确认后真实执行
python scripts/job_apigw_client.py plan-create \
  --bk-scope-id <业务ID> \
  --job-template-id 1000 \
  --name "api-plan-demo" \
  --enable-steps '[101,102]' \
  --variables-file /tmp/variables.json
# 成功返回含 job_plan_id 与 job_plan_url，须以可点击链接交付用户
```

> **⚠️ 字段名注意（重要！）**：
> - `template-detail` 返回 + `plan-create` 传入：type=3 变量使用 `execute_target.host_list`
> - `plan-execute` 传入：type=3 变量使用 `server.ip_list`（与 plan-create 不同！）
> - 主机必须同时提供 `bk_cloud_id`（云区域ID）和 `ip`，否则报错"目标执行对象为空"

### 5.2 创建并启用定时任务

```bash
# 保存定时任务（新建，默认暂停）
python scripts/job_apigw_client.py cron-save \
  --bk-scope-id <业务ID> \
  --job-plan-id 50001 \
  --name "每日巡检" \
  --expression "0 8 * * *" \
  --execute-time-zone "Asia/Shanghai"

# 用户明确确认启用后
python scripts/job_apigw_client.py cron-update-status \
  --bk-scope-id <业务ID> \
  --cron-id 1000067 \
  --status 1
```

#### 示例：创建定时任务并传入全局变量

```bash
# A. 将变量 JSON 写入文件（避免命令行转义问题）
cat > /tmp/cron_vars.json << 'EOF'
[
  {"name":"hosts","type":3,"server":{"ip_list":[{"bk_cloud_id":0,"ip":"127.0.0.1"}]}},
  {"name":"var1","type":1,"value":"var1111x"},
  {"name":"var2","type":1,"value":"var2222x"}
]
EOF

> **⚠️ 字段名关键区别（重要！）**：
> - `plan-create` 传入：type=3 变量使用 `execute_target.host_list`
> - `cron-save` 传入：type=3 变量使用 `server.ip_list`（与 plan-create 不同！）
> - 主机必须同时提供 `bk_cloud_id`（云区域ID）和 `ip`

# B. dry-run 校验请求体
python scripts/job_apigw_client.py cron-save \
  --bk-scope-id <业务ID> \
  --job-plan-id 50001 \
  --name "定时任务示例" \
  --expression "0 10 * * *" \
  --execute-time-zone "Asia/Shanghai" \
  --global-vars-file /tmp/cron_vars.json \
  --dry-run

# C. 用户独立确认后真实执行
python scripts/job_apigw_client.py cron-save \
  --bk-scope-id <业务ID> \
  --job-plan-id 50001 \
  --name "定时任务示例" \
  --expression "0 10 * * *" \
  --execute-time-zone "Asia/Shanghai" \
  --global-vars-file /tmp/cron_vars.json
```

## 6. 相关接口文档

- [`../apidocs/get_job_template_list.md`](../apidocs/get_job_template_list.md)
- [`../apidocs/v4_get_job_template_detail.md`](../apidocs/v4_get_job_template_detail.md)
- [`../apidocs/v4_create_job_plan.md`](../apidocs/v4_create_job_plan.md)
- [`../apidocs/save_cron.md`](../apidocs/save_cron.md)
- [`../apidocs/update_cron_status.md`](../apidocs/update_cron_status.md)
