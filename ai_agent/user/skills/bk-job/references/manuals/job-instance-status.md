# 作业实例：执行历史、状态与日志

列举执行历史时默认 **`--length 20`**；分页与节省 Token 的分析方式见 [listing-and-token-efficient-analysis.md](listing-and-token-efficient-analysis.md)。

## 1. 子命令

| 子命令 | 接口 | 用途 |
|--------|------|------|
| `instance-list` | `GET /api/v4/get_job_instance_list` | **查询执行历史**：按时间窗口列出作业实例，可按名称、执行人、执行方式、任务类型、状态、目标 IP 等过滤 |
| `instance-status` | `GET /api/v4/get_job_instance_status` | 已知 `job_instance_id` 时查整体与各步骤状态 |
| `get-instance-log` | `POST /api/v4/batch_get_job_instance_execute_object_log` | 取某一步骤在各主机/容器上的执行日志 |

三者均为**只读**，无需过写操作确认门禁。

## 2. 推荐流程

### 2.1 查执行历史并下钻

```
[A] instance-list --lookback-days 7 [--keyword/--operator/--status/--type ...]
        └─ 列出近期实例，表格展示：实例ID、任务名、类型、执行方式、状态、执行人、创建时间
             ↓
[B] 用户选定某条记录，拿到 job_instance_id
             ↓
[C] instance-status --job-instance-id <ID> --with-objects
        └─ 看整体状态与各步骤状态，拿到失败步骤的 step_instance_id
             ↓
[D] get-instance-log --job-instance-id <ID> --step-instance-id <步骤ID> --host-id-list ...
        └─ 取该步骤的执行日志
```

- 用户问「最近执行了什么」「谁跑的」「有哪些失败的任务」时用 `instance-list`，**不要**让用户先提供实例 ID。
- 排查失败任务的常用组合：`--status 4`（执行失败）加 `--lookback-days`，定位后再按 [C][D] 下钻。
- 只想看**某个定时任务**的最近一次执行，用 `cron-last-run` 更直接（它已封装 [A]~[D] 全过程），见 [cron-tasks-and-last-execution.md](cron-tasks-and-last-execution.md)。

## 3. `instance-list` 参数

| 参数 | 说明 |
|------|------|
| `--lookback-days` | 回溯天数，默认 7；**硬上限 31 天**，超出自动截断并在 stderr 提示。接口要求必填时间窗口，脚本按此参数换算为毫秒时间戳 |
| `--keyword` | 任务名称模糊匹配 |
| `--operator` | 执行人，**精准**匹配（非模糊） |
| `--launch-mode` | 执行方式：1 页面执行、2 API调用、3 定时执行 |
| `--type` | 任务类型：0 作业执行、1 脚本执行、2 文件分发 |
| `--status` | 任务状态，如 3 执行成功、4 执行失败；取值见 [troubleshooting-and-status-codes.md](troubleshooting-and-status-codes.md) |
| `--ip` | 执行目标服务器 IP，精准匹配 |
| `--cron-id` | 按定时任务 ID 过滤（对应接口 `job_cron_id`） |
| `--job-instance-id` | 按实例 ID 精确查询；**传入后接口会忽略其余过滤条件** |
| `--offset` / `--length` | 翻页，`offset` 最大 10000，`length` 接口最大 200 |

脚本为每条记录补充可读字段：`任务状态`、`任务类型`、`执行方式`、`创建时间`、`启动时间`、`结束时间`、`耗时秒`，原始数值字段保持不变。

> **接口不返回总数**：若本页条数等于 `--length`，可能还有更早的记录，可增大 `--offset` 翻页或缩小时间窗口，**不要**据此向用户断言「共 N 条」。

## 4. `instance-status` 与 `get-instance-log` 参数

| 参数 | 说明 |
|------|------|
| `instance-status --job-instance-id` | 必填 |
| `instance-status --with-objects` | 设置 `return_execute_object_result=true`，返回每主机/容器上的步骤结果（体积更大） |
| `get-instance-log --job-instance-id` / `--step-instance-id` | 均必填，步骤 ID 取自 `instance-status` 的步骤列表 |
| `get-instance-log --host-id-list` / `--ip-list` / `--container-id-list` | 执行对象，三选一；`--ip-list` 格式为 `bk_cloud_id:ip`，**单次最多 50 个** |

## 5. 注意事项

- `instance-list` 只返回实例的**概要**，不含步骤详情与日志；需要细节时按 2.1 下钻。
- v4 响应为 `{ "data": { ... } }` 形态；异常时为 HTTP 4xx/5xx 及 `error` 体（见接口文档）。
- 时间字段均为 **Unix 毫秒时间戳**，脚本已同时给出可读时间。

## 6. 示例

```bash
# 近 7 天的执行历史（默认 20 条）
python scripts/job_apigw_client.py instance-list --bk-scope-id <业务ID>

# 近 30 天内失败的文件分发任务
python scripts/job_apigw_client.py instance-list \
  --bk-scope-id <业务ID> --lookback-days 30 --type 2 --status 4

# 某人通过 API 触发的任务
python scripts/job_apigw_client.py instance-list \
  --bk-scope-id <业务ID> --operator admin --launch-mode 2

# 下钻：状态 → 日志
python scripts/job_apigw_client.py instance-status \
  --bk-scope-id <业务ID> --job-instance-id 2000501 --with-objects

python scripts/job_apigw_client.py get-instance-log \
  --bk-scope-id <业务ID> --job-instance-id 2000501 \
  --step-instance-id 3000501 --host-id-list 101,102
```

业务集：增加 `--bk-scope-type biz_set`。

## 7. 相关接口文档

- [`../apidocs/v4_get_job_instance_list.md`](../apidocs/v4_get_job_instance_list.md)
- [`../apidocs/v4_get_job_instance_status.md`](../apidocs/v4_get_job_instance_status.md)
- [`../apidocs/v4_batch_get_job_instance_execute_object_log.md`](../apidocs/v4_batch_get_job_instance_execute_object_log.md)
