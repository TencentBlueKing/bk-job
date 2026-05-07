# 故障排查与状态码

## 1. 常见故障

| 现象 | 可能原因 | 处理方向 |
|------|----------|----------|
| 终端中文乱码 | 控制台非 UTF-8 或未走脚本包装后的 stdout | 直接运行官方脚本（已强制 UTF-8）；或 `chcp 65001`（Windows）、`python -X utf8` |
| HTTP 401 | `access_token` 过期或无效 | 重新申请令牌，更新环境变量 |
| HTTP 403 | 无业务/作业权限 | 权限中心申请；确认 `bk_scope_id` 正确 |
| 网关 404 / 非 JSON | `BK_JOB_APIGW_BASE_URL` 错误 | 与 API 网关控制台作业平台路径对齐 |
| `cron-last-run` 无执行历史 | 时间窗口内无定时触发、或并非 `launch_mode=3` | 增大 `--lookback-days`；确认任务是否由定时触发 |
| `plan-execute` 参数错误 | `global_var_list` 与方案变量不一致 | `plan-detail` 核对 `name`/`id`/`server` 结构 |
| 日志不完整 | 单步超过 50 台主机 | 脚本已截断并 `log_warning`；分批换用 API 或缩小范围 |

## 2. 作业/任务状态码（常见）

与作业实例、步骤上展示的整型状态一致（完整定义见 v4 文档中的 `task_status` / `run_status` / `target_object_status`）。

| 值 | 含义 |
|----|------|
| 1 | 等待执行 |
| 2 | 正在执行 |
| 3 | 执行成功 |
| 4 | 执行失败 |
| 5 | 跳过 |
| 6 | 忽略错误 |
| 7 | 等待用户 |
| 8 | 手动结束 |
| 9 | 状态异常 |
| 10 | 强制终止中 |
| 11 | 强制终止成功 |
| 13 | 确认终止 |
| 14 | 被丢弃 |
| 15 | 滚动等待 |

单主机脚本结果等另有 `target_object_status`（如 9 表示执行成功），与上表不同维度，以 [`../apidocs/v4_get_job_instance_status.md`](../apidocs/v4_get_job_instance_status.md) 为准。
