# 作业实例状态查询（辅助）

## 1. 子命令

`instance-status` → `GET /api/v4/get_job_instance_status`

## 2. 用途

在已知道 **`job_instance_id`**（例如 `plan-execute` 返回，或其它系统通知）时，查询整体状态与各步骤状态。

## 3. 参数说明

- `--job-instance-id`：必填。  
- `--with-objects`：为真时设置 `return_execute_object_result=true`，返回每主机/容器上的步骤结果（体积更大）。

## 4. 注意事项

- 本命令**不拉取完整执行日志**；日志请用 `batch_get_job_instance_execute_object_log`（已由 `cron-last-run` 对定时场景封装）。  
- v4 响应为 `{ "data": { ... } }` 形态；异常时为 HTTP 4xx/5xx 及 `error` 体（见接口文档）。

## 5. 示例

```bash
python scripts/job_apigw_client.py instance-status \
  --bk-scope-id <业务ID> \
  --job-instance-id 2000501 \
  --with-objects
```

## 6. 相关接口文档

[`../apidocs/v4_get_job_instance_status.md`](../apidocs/v4_get_job_instance_status.md)
