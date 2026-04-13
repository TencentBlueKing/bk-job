# 执行方案：搜索、详情与启动

搜索执行方案时默认 **`--length 20`**；全量、分页与节省 Token 的分析方式见 [listing-and-token-efficient-analysis.md](listing-and-token-efficient-analysis.md)。

涉及 **`bk_scope` 预填、常用方案与参数沉淀** 时，见 [business-memory.md](business-memory.md)。

## 1. 子命令

| 子命令 | 接口 | 说明 |
|--------|------|------|
| `plan-search` | `GET /api/v3/get_job_plan_list` | `name` 模糊匹配 |
| `plan-detail` | `GET /api/v3/get_job_plan_detail` | 步骤列表、`global_var_list` |
| `plan-execute` | `POST /api/v3/execute_job_plan` | 启动执行，可选 `global_var_list` |

## 2. 推荐顺序

1. `plan-search` 缩小范围。  
2. 启动前用 `plan-detail` 核对必填变量、主机变量、密文变量等。  
3. 组装 `global_var_list`（字段定义见 [`../apidocs/execute_job_plan.md`](../apidocs/execute_job_plan.md) 与 [`../apidocs/get_job_plan_detail.md`](../apidocs/get_job_plan_detail.md)）。  
4. 使用 **`plan-execute --dry-run`** 打印将提交的 JSON；**真实启动**须严格遵守 [confirmation-and-output-protocol.md](confirmation-and-output-protocol.md) **第 1 节**（含 G1–G4 门禁与 G2 两轮确认）。

## 3. 写操作确认协议（必须）

**完整规则（门禁、摘要格式、反面案例）见 [confirmation-and-output-protocol.md](confirmation-and-output-protocol.md) 第 1 节。** 摘要：**未经 1.4 确认摘要 + 用户下一条独立确认，禁止真实 `plan-execute`（非 `--dry-run`）。**

## 4. 关键词与歧义

- 仅传 `--keyword` 时：唯一匹配则使用该方案；**多条匹配**时脚本只输出列表并退出，需 `--job-plan-id` 或 `--pick-first`（后者需用户知情）。  
- `--global-vars` 为 **JSON 数组**字符串，例如：`'[{"name":"x","value":"y"}]'`。主机类变量需 `server` 结构，与 OpenAPI 一致。

## 5. 详细说明与注意事项

- 执行方案与作业模板可能不同步（`need_update` 等字段见列表接口）；启动前是否同步由用户在作业平台界面或另行处理，脚本不自动同步模板。  
- `callback_url` 等高级参数当前脚本未暴露，需要时可改 `job_apigw_client.py` 或自行调用网关。

## 6. 命令示例

```bash
python scripts/job_apigw_client.py plan-search \
  --bk-scope-id <业务ID> --keyword "重启"

python scripts/job_apigw_client.py plan-detail \
  --bk-scope-id <业务ID> --job-plan-id 1000193

python scripts/job_apigw_client.py plan-execute \
  --bk-scope-id <业务ID> --job-plan-id 1000193 \
  --global-vars '[{"name":"param_name","value":"param_value"}]' \
  --dry-run
```

## 7. 相关接口文档

- [`../apidocs/get_job_plan_list.md`](../apidocs/get_job_plan_list.md)  
- [`../apidocs/get_job_plan_detail.md`](../apidocs/get_job_plan_detail.md)  
- [`../apidocs/execute_job_plan.md`](../apidocs/execute_job_plan.md)  
