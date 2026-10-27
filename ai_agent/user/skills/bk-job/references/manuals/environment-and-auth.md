# 环境与鉴权

## 1. 脚本

[`scripts/job_apigw_client.py`](../../scripts/job_apigw_client.py)（相对技能根目录）。

## 2. 技能配置（config.yaml）

网关与页面 URL、多租户默认 ID **写在技能根目录 [`config.yaml`](../../config.yaml)** 中，脚本启动时自动读取，**不读环境变量**。

| 配置项 | 含义                                                                                                    |
|--------|-------------------------------------------------------------------------------------------------------|
| `apigw_base_url` | 作业平台在 **API 网关**上的根地址，须能直接拼接 `/api/v3/...`、`/api/v4/...`（含 `https://`、环境/阶段前缀，以实际网关发布为准）              |
| `job_base_url` | 作业平台 **Web 控制台**根地址（非 APIGW），用于 `plan-execute` 成功后拼接：`{job_base_url}/api_execute/{job_instance_id}`   |
| `bk_tenant_id` | **可选**。当前环境下的默认租户 ID，作为header `X-Bk-Tenant-Id` 传给 APIGW。**同环境跨租户时用 `--bk-tenant-id` 入参按次覆盖，无需修改本文件**。 |

部署到新环境时，修改 [`config.yaml`](../../config.yaml) 中上述字段（**代码库内仅为占位示例，须替换为真实地址**），与技能包一并同步到目标宿主即可。

占位示例（**非真实地址**）：

```yaml
# access_token 由 ai-hub 命令或环境变量 BK_JOB_ACCESS_TOKEN 提供，勿写入本文件
apigw_base_url: https://bkapi.example.com/api/bk-job/prod
job_base_url: https://job.example.com
# 可选：当前环境默认租户；跨租户时用 --bk-tenant-id 覆盖，未配置则默认 default
bk_tenant_id: default
```

## 3. 多租户（X-Bk-Tenant-Id）

同一作业平台环境可能同时服务多个租户，因此租户 ID 可能需要**按次切换**。脚本按下列优先级解析本次请求使用的租户 ID，并自动写入请求头 `X-Bk-Tenant-Id`：

| 优先级 | 来源 | 适用场景 |
|------|------|----------|
| 1 | `--bk-tenant-id` CLI 入参 | **用户当轮指定**的租户，智能体归集到本次调用中强制指定；仅对本次进程生效 |
| 2 | `config.yaml` 中的 `bk_tenant_id` | 当前环境的**默认租户**（如部署宿主主要服务 `system` 租户） |
| 3 | `"default"` | 兼容非多租户环境或默认不知时使用 |

### 3.1 智能体行为准则（必看）

- **先从上下文/记忆取**：如当前对话或业务记忆中已明确租户 ID（例如用户刚说“帮我查 tenant-a 业务 X 的任务”），直接传 `--bk-tenant-id tenant-a`。
- **上下文未明确时先问用户**：多租户环境下若不知道要操作哪个租户，**先归集确认租户 ID**，可用 `list-authorized-scopes` 前向用户发送选项卡（也可拉 `--bk-tenant-id` 候选列表）；**不得凭空猜租户**，否则可能误操作到其他租户的资源。
- **不得修改 config.yaml**：切换租户仅当轮生效，无需也不允许自行改写部署时的 `config.yaml`。
- **不知道时优先依赖 config.yaml**：若用户仅给了业务信息而未提租户，且本地部署已在 `config.yaml` 里写了 `bk_tenant_id`，则不需强行追问。
- **回答用户时声明本次租户**：无论查询还是写操作，输出中都要说清本次请求实际生效的租户 ID 及其来源（`--bk-tenant-id` / `config.yaml` / `default`），便于用户核对是否符合预期。接口返回 4xx、资源不存在或列表为空时，除给出常规排查建议外，**再附带一句**「本次请求的是租户 `<X>` 下的资源，如与预期不符可通过 `--bk-tenant-id` 指定」——作为并列信息提供，不预设租户错配就是根因。
  - 话术示例：「在租户 `system`（来源：`config.yaml`）下未查到编号为 12345 的定时任务；如您想查的是其它租户的资源，请告知租户 ID，我改用 `--bk-tenant-id <ID>` 重试。」
- **多租户报错信号**：APIGW 返回 401/403 时，令牌、权限与租户 ID 都可能相关，按实际错误信息与业务上下文并列排查，不预先偏向某一项。

### 3.2 命令行写法

`--bk-tenant-id` 为全局参数，需**放在子命令之前**：

```bash
# 例：当轮切到 tenant-a 查受权业务
python3 scripts/job_apigw_client.py --bk-tenant-id tenant-a list-authorized-scopes

# 例：不传则回退 config.yaml 的默认租户
python3 scripts/job_apigw_client.py cron-search --bk-scope-id 1001
```

## 4. 访问令牌

令牌为蓝鲸用户态 **access_token**，脚本放入请求头 `X-Bkapi-Authorization: {"access_token":"<token>"}`。

多租户环境下同时需传 `X-Bk-Tenant-Id: <租户 ID>`（按上节三级优先级解析），两个头部由脚本自动拼装，智能体无需干预。

### 4.1 获取顺序（脚本自动完成，无需智能体介入）

| 顺序 | 来源 | 说明 |
|------|------|------|
| 1 | `--access-token` | 显式传入，优先级最高，用于临时覆盖 |
| 2 | **`ai-hub` 命令** | PATH 中存在 `ai-hub` 时（即运行在 **imate 数字分身**上），脚本执行 `ai-hub access-token get`，从输出 JSON 中取 `access_token` 字段 |
| 3 | `BK_JOB_ACCESS_TOKEN` | 环境变量，`ai-hub` 不存在或取令牌失败时回退 |

`ai-hub` 输出形如 `{"access_token":"xxxx", ...}`；解析与回退**全部在脚本内完成**，智能体不要自行调用 `ai-hub`，也不要解析其输出或把令牌贴进对话。

`ai-hub` 调用超时为 10 秒，返回非零退出码、输出非 JSON、缺少 `access_token` 字段时均视为失败并回退到环境变量。

### 4.2 取不到令牌时的报错

脚本按是否检测到 `ai-hub` 给出不同引导：

| 环境 | 提示方向 |
|------|---------|
| 无 `ai-hub`（非 imate 环境） | 只提示设置环境变量 `BK_JOB_ACCESS_TOKEN` |
| 有 `ai-hub`（imate 环境） | 提示检查 imate 数字分身上的 ai-hub 服务是否正常（可手动执行 `ai-hub access-token get` 验证），或手动设置 `BK_JOB_ACCESS_TOKEN` |

向用户转述时按脚本给出的方向引导，**不要**在非 imate 环境让用户去查 ai-hub 服务。

**不要把 access_token 写进 config.yaml、SKILL、手册或仓库**，也不要在对话中回显；用 `ai-hub`、环境变量或本地私密配置。

## 5. 资源范围

绝大多数子命令需要：

- `--bk-scope-type`：`biz`（业务）或 `biz_set`（业务集），默认 `biz`
- `--bk-scope-id`：与类型对应的 ID（字符串）

无权限时接口返回 403，需在权限中心申请作业平台/业务相关权限。

## 6. 注意事项

- **多租户错配**：若当轮传入的 `--bk-tenant-id` 与目标资源归属租户不一致，或 config.yaml 中 `bk_tenant_id` 写错，APIGW 会回 401/403 或路由至错误租户；不确定时先向用户确认租户 ID。
- **跨平台中文**：客户端脚本会将标准输出/错误设为 **UTF-8**（Windows 常见 GBK 控制台可减轻乱码）。若 IDE/管道仍异常，可尝试终端 UTF-8、`python -X utf8`。
- 网关根 URL 若配置错误，会出现 404、HTML 错误页或路由到错误服务；应与 API 网关控制台中「作业平台」组件对外路径一致。
- `access_token` 通常有有效期，过期后表现为 401，需按蓝鲸流程重新申请。
