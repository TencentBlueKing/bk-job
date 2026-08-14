# 环境与鉴权

## 1. 脚本

[`scripts/job_apigw_client.py`](../../scripts/job_apigw_client.py)（相对技能根目录）。

## 2. 技能配置（config.yaml）

网关与页面 URL **写在技能根目录 [`config.yaml`](../../config.yaml)** 中，脚本启动时自动读取，**不读环境变量**。

| 配置项 | 含义 |
|--------|------|
| `apigw_base_url` | 作业平台在 **API 网关**上的根地址，须能直接拼接 `/api/v3/...`、`/api/v4/...`（含 `https://`、环境/阶段前缀，以实际网关发布为准） |
| `job_base_url` | 作业平台 **Web 控制台**根地址（非 APIGW），用于 `plan-execute` 成功后拼接：`{job_base_url}/api_execute/{job_instance_id}` |

部署到新环境时，修改 [`config.yaml`](../../config.yaml) 中上述两个字段（**代码库内仅为占位示例，须替换为真实地址**），与技能包一并同步到目标宿主即可。

占位示例（**非真实地址**）：

```yaml
# access_token 由 ai-hub 命令或环境变量 BK_JOB_ACCESS_TOKEN 提供，勿写入本文件
apigw_base_url: https://bkapi.example.com/api/bk-job/prod
job_base_url: https://job.example.com
```

## 3. 访问令牌

令牌为蓝鲸用户态 **access_token**，脚本放入请求头 `X-Bkapi-Authorization: {"access_token":"<token>"}`。

### 3.1 获取顺序（脚本自动完成，无需智能体介入）

| 顺序 | 来源 | 说明 |
|------|------|------|
| 1 | `--access-token` | 显式传入，优先级最高，用于临时覆盖 |
| 2 | **`ai-hub` 命令** | PATH 中存在 `ai-hub` 时（即运行在 **imate 数字分身**上），脚本执行 `ai-hub access-token get`，从输出 JSON 中取 `access_token` 字段 |
| 3 | `BK_JOB_ACCESS_TOKEN` | 环境变量，`ai-hub` 不存在或取令牌失败时回退 |

`ai-hub` 输出形如 `{"access_token":"xxxx", ...}`；解析与回退**全部在脚本内完成**，智能体不要自行调用 `ai-hub`，也不要解析其输出或把令牌贴进对话。

`ai-hub` 调用超时为 10 秒，返回非零退出码、输出非 JSON、缺少 `access_token` 字段时均视为失败并回退到环境变量。

### 3.2 取不到令牌时的报错

脚本按是否检测到 `ai-hub` 给出不同引导：

| 环境 | 提示方向 |
|------|---------|
| 无 `ai-hub`（非 imate 环境） | 只提示设置环境变量 `BK_JOB_ACCESS_TOKEN` |
| 有 `ai-hub`（imate 环境） | 提示检查 imate 数字分身上的 ai-hub 服务是否正常（可手动执行 `ai-hub access-token get` 验证），或手动设置 `BK_JOB_ACCESS_TOKEN` |

向用户转述时按脚本给出的方向引导，**不要**在非 imate 环境让用户去查 ai-hub 服务。

**不要把 access_token 写进 config.yaml、SKILL、手册或仓库**，也不要在对话中回显；用 `ai-hub`、环境变量或本地私密配置。

## 4. 资源范围

绝大多数子命令需要：

- `--bk-scope-type`：`biz`（业务）或 `biz_set`（业务集），默认 `biz`
- `--bk-scope-id`：与类型对应的 ID（字符串）

无权限时接口返回 403，需在权限中心申请作业平台/业务相关权限。

## 5. 注意事项

- **跨平台中文**：客户端脚本会将标准输出/错误设为 **UTF-8**（Windows 常见 GBK 控制台可减轻乱码）。若 IDE/管道仍异常，可尝试终端 UTF-8、`python -X utf8`。
- 网关根 URL 若配置错误，会出现 404、HTML 错误页或路由到错误服务；应与 API 网关控制台中「作业平台」组件对外路径一致。
- `access_token` 通常有有效期，过期后表现为 401，需按蓝鲸流程重新申请。
