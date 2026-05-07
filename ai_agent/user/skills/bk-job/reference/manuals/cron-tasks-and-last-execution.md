# 定时任务检索与最近一次定时执行（状态 + 日志）

列举定时任务时默认 **`--length 20`**；全量、分页与节省 Token 的分析方式见 [listing-and-token-efficient-analysis.md](listing-and-token-efficient-analysis.md)。

## 1. 子命令

| 子命令 | 作用 |
|--------|------|
| `cron-search` | 仅列出定时任务（`GET /api/v3/get_cron_list`） |
| `cron-last-run` | 关键词或 `--cron-id` → 最近一次**定时触发**的执行实例 → 状态 + 各步骤执行日志 |

## 2. `cron-last-run` 内部流程（脚本已实现）

1. **定位定时任务**  
   - 若提供 `--keyword`：调用 `get_cron_list`，`name` 模糊匹配。  
   - 若提供 `--cron-id`：仍可对列表接口带 `id` 查询元数据；若无返回，脚本仍用该 `cron_id` 查执行历史（见 `_note` 字段）。  
   - **多条匹配**且未指定 `--cron-id`：默认只输出列表并退出；需用户指定 ID 或 `--pick-first`（慎用）。

2. **最近一次定时执行**  
   - `GET /api/v4/get_job_instance_list`  
   - 条件：`job_cron_id` = 定时任务 ID，`launch_mode=3`（定时执行），`create_time_start` / `create_time_end` 为**毫秒**时间戳；回溯天数由 `--lookback-days` 指定，**脚本硬上限 31 天**（超出截断，stderr 有提示，JSON 中有 `query.lookback_days_effective` 等字段）。  
   - `offset=0`，`length=1`：列表按**从新到老**，取第一条即最近一次。

3. **状态与执行对象**  
   - `GET /api/v4/get_job_instance_status`，`return_execute_object_result=true`，用于拿到每步的 `step_execute_object_result_list`。

4. **日志**  
   - 对每步调用 `POST /api/v4/batch_get_job_instance_execute_object_log`。  
   - 优先组 `host_id_list`；否则 `ip_list`（`bk_cloud_id` + `ip`）；容器用 `container_id_list`。  
   - **单请求最多 50 个执行对象**；超出时脚本只请求前 50，并在该步结果中带 `log_warning`。  
   - 无主机/容器对象的步骤（如纯人工确认）会标注 `log_note`，不调用日志接口。

## 3. 详细说明与注意事项

- **仅统计「定时触发」**：脚本固定 `launch_mode=3`。页面执行、API 触发不会在 `cron-last-run` 里作为「定时任务最近执行」返回；若需其它触发方式，须改脚本或直接用 `v4_get_job_instance_list` 自行组合参数（见 [`../apidocs/v4_get_job_instance_list.md`](../apidocs/v4_get_job_instance_list.md)）。
- **时间窗口**：若长期未跑定时任务，会显示无执行记录；回溯**最多 31 天**（产品硬限制，不可通过参数突破）。
- **启停状态**：脚本在每条定时任务上输出 **`启停状态`**（由网关返回的 `status` 映射：1 已启动、其他值表示已暂停）。向用户展示定时任务时必须带上该字段。
- **关键词**：与产品侧「名称模糊匹配」行为一致；匹配过多时务必让用户确认 `--cron-id`。

## 4. 命令示例

```bash
python scripts/job_apigw_client.py cron-search \
  --bk-scope-id <业务ID> --keyword "清理"

python scripts/job_apigw_client.py cron-last-run \
  --bk-scope-id <业务ID> --keyword "清理" --lookback-days 30

python scripts/job_apigw_client.py cron-last-run \
  --bk-scope-id <业务ID> --cron-id 1000031
```

业务集：增加 `--bk-scope-type biz_set`。

## 5. 相关接口文档

- [`../apidocs/get_cron_list.md`](../apidocs/get_cron_list.md)  
- [`../apidocs/v4_get_job_instance_list.md`](../apidocs/v4_get_job_instance_list.md)  
- [`../apidocs/v4_get_job_instance_status.md`](../apidocs/v4_get_job_instance_status.md)  
- [`../apidocs/v4_batch_get_job_instance_execute_object_log.md`](../apidocs/v4_batch_get_job_instance_execute_object_log.md)  
