# AIDEV 资源目录

本目录维护作业平台需要自动初始化到 AIDEV 平台的 AI 资源，由部署阶段的 `job-sync-bk-aidev` 任务统一同步，
通过 Helm 的 `bkai.enabled` 开关控制是否开启。镜像中资源被放在 `/bk-job` 目录下，清单路径为
`/bk-job/bkai.yaml`，由 `bkai.packagePath` 指定。

## 目录结构

```plain
support-files/bk-aidev/bk-job/
├── bkai.yaml                 # Agent Package 清单，声明本次要同步的资源
├── agents/
│   └── bk_job_ai.yaml        # Agent 定义（引用 MCP / Skill / 知识库）
├── skills/                   # Skill 资源，目录名即 Skill code
├── knowledgebases/           # 知识库资源，每个子目录一个知识库
└── bin/
    ├── sync-bkaidev.sh       # 同步脚本，执行占位符渲染与 bkai-init validate / plan / sync
    └── render_placeholders.py # 占位符渲染脚本
```

## 资源 code 命名规则

code 是平台定位资源的稳定标识，**一律小写**，与展示名称无关；不符合规则会被 `bkai-init` 直接拒绝，
且平台不做大小写转换或迁移。

| 资源 | 规则 | 本仓库取值 |
| --- | --- | --- |
| 智能体 / 子智能体 | `^ai-[a-z][a-z0-9-]{1,12}$`，总长 5–16 位，不支持下划线 | `ai-bkjob-web` |
| 角色 / MCP / Skill | `^[a-z][a-z0-9_-]{0,63}$` | `bk-job-prod-mcp-task-context` |
| 知识库 | `^[a-z][a-z0-9_]{0,63}$`，**不支持中划线** | `bk_job_user_guide` |

知识库 code 直接用作 Milvus 集合名，Milvus 不接受中划线，故只能用下划线。

## 与 MCP 的关系

Agent 只引用 MCP，不创建 MCP。MCP Server 由 API 网关侧同步产生：

- 定义位置：`support-files/bk-api-gateway/v3/definition.yaml` 的 `stages[].mcp_servers`；
- 同步动作：`support-files/bk-api-gateway/v3/bin/sync-bkapigateway.sh` 中的 `sync_apigw_stage_mcp_servers`，
  由 `bkApiGatewayConfig.syncMcpServers` 开关控制，且必须排在网关资源发布之后；
- 引用位置：`agents/bk_job_ai.yaml` 的 `spec.mcps[].code`，取值与 `mcp_servers[].name` 一一对应。

开启网关同步（`bkApiGatewayConfig.sync=true`）时，`job-sync-bk-aidev` 会通过 `initContainer`（`wait-sync-api-gateway`）
等待网关同步 Job 执行完成后再启动，此外 `bin/sync-bkaidev.sh` 也保留了有限次重试以应对异步延迟。

## 占位符渲染

资源文件中与部署环境相关的地址不写死，而是用 `${占位符}` 表示，同步前由 `bin/render_placeholders.py`
替换为真实值。占位符取值来自同名环境变量，环境变量在 Helm 的同步 Job 中注入。

| 占位符 | 含义 | 取值来源 |
| --- | --- | --- |
| `${JOB_URL_BASE}` | 作业平台访问地址，如 `http://job.example.com` | Chart helper `job.url.base` |
| `${JOB_AI_GENERATE_TYPE}` | Agent 的 `spec.generate_type`，多租户为 `global`、非多租户为 `public` | Chart `tenant.enabled` |

新增占位符时需要同时改两处：`render_placeholders.py` 中的 `PLACEHOLDER_NAMES` 名单，以及
`support-files/kubernetes/charts/bk-job/templates/job-migration/sync-bkaidev-job.yaml` 中对应的环境变量。

渲染刻意按名单逐个做字面量替换，而不是全量替换所有 `${xxx}`：知识库文档中大量出现作业平台魔法变量的
字面写法（如 `${JOB_NAMESPACE_ALL}`、`${svr_addr}`），全量替换会把这些说明文档中的示例清成空串。
渲染范围为 `bkai.yaml` 与 `agents`/`skills`/`knowledgebases` 三个目录下的文本文件，不含 `bin` 目录。
若某个占位符在资源文件中被使用但对应环境变量为空，脚本会直接报错退出，避免把空地址同步到平台。

渲染用 Python 而非 shell 的 `sed`：字面量替换不涉及 `sed` 中 `&`、`\` 与分隔符的转义规则，
文件编码固定 UTF-8 且不改动原有换行风格，行为不受基础镜像 GNU coreutils / BusyBox 差异影响。
因此基础镜像中需要有 Python 3，`sync-bkaidev.sh` 会依次探测 `python3`、`python`，
也支持通过 `PYTHON_BIN` 指定解释器，并在缺失或版本不符时明确报错。

## 同步方式

镜像中已内置 `bkai-init`（基础镜像 `bk-aidev-init:0.1.5`），同步等价于：

```bash
bkai-init validate -f /bk-job/bkai.yaml --space "$SPACE_ID"
bkai-init plan     -f /bk-job/bkai.yaml --tenant-id "$TENANT_ID" --space "$SPACE_ID" \
  --publish --publish_config_only=0 --admins user_a user_b
bkai-init sync     -f /bk-job/bkai.yaml --tenant-id "$TENANT_ID" --space "$SPACE_ID" \
  --confirm --publish --publish_config_only=0 --admins user_a user_b
```

几个容易踩的点：

- **`sync` 不带 `--confirm` 只是只读预览**，命令照样返回 0，但平台上什么都不会写；
  `plan` 只预览同步动作与差异，不保证后续写权限或发布成功；
- **`--space` 传的是目标空间完整 ID**，`bkai-init` 不会自动拼接租户前缀，资源文件里不再声明 `space`。
  `bkai.space` 留空时，开启多租户取 `system-bkaidev`，未开启取 `bkaidev`；
- **多租户由 `BKPAAS_MULTI_TENANT_MODE` 控制**，Chart 按 `tenant.enabled` 注入。`--tenant-id` 取值须为
  作业平台应用实际所在的租户，由 `bkai.tenantId` 配置，留空时多租户取 `system`、非多租户取 `default`；
  租户不一致会报 `CROSS_TENANT_FORBIDDEN`。多租户下 Agent 的 `spec.generate_type` 渲染为 `global`（全租户，
  仅 `system-bkaidev` 可配置，创建后不可切回 `space`/`public`），非多租户下为 `public`（所有空间可见，供其他智能体调用）；知识库等其它资源固定为 `space`；
- **不带 `--publish` 只同步草稿**，页面上的智能体不会更新，由 `bkai.publish` 控制；
  发布方式由 `bkai.publishConfigOnly` 控制，默认 `false` 走平台常规发布流程，置 `true` 则只发布配置；
- 网关地址与应用身份由 `BK_API_URL_TMPL` / `BKAI_APP_CODE` / `BKAI_APP_SECRET` 提供，变量名由
  `bkai-init` 约定，不可改名；模板中的 `{api_name}` 会被替换成 `bk-aidev`。多租户且未指定初始化用户时，
  还会替换成 `bk-user` 查询默认 `bk_admin` 对应的 `bk_username`，此时用户管理网关也要能通；
- 初始化用户与管理员见下一节。

资源被用户在平台上手工改动、不希望再次覆盖时，可通过 `bkai.excludeResources` 传入 `kind/code`
（kind 小写，取值 `agent` / `collection` / `skill` / `knowledgebase`，如 `["agent/ai-bkjob-web"]`）跳过该资源。
同步中途失败时已写入的资源不会自动回滚，重跑是按 code 覆盖式重入；若耗尽重试仍失败，默认（`bkai.ignoreSyncError=false`）
脚本以非 0 退出码终止容器，K8S Job（`backoffLimit: 0`）随之进入 `Failed` 状态，在开启 `waitForJobs` 时
Helm 部署也会直接判定为失败；若将 `bkai.ignoreSyncError` 设为 `true`，则脚本打印告警并以 0 退出，忽略错误让 Helm 部署成功。

## 初始化用户与管理员

**这两处填的都是 `bk_username`，不是登录名（`login_name`）**，`bkai-init` 原样透传，不做任何转换。
单租户下两者相同；多租户下两者不同，填成登录名会报 `bk_username is not verified`。

初始化用户，即同步资源时的操作人，按 `bkai.accessToken` > `bkai.username` > 镜像默认 `bk_admin`
取值。两个 values 都留空时不注入对应环境变量，以免空值覆盖镜像默认值；此时若开启多租户，
`bkai-init` 会查询默认 `bk_admin` 对应的 `bk_username`，需要应用具备用户管理的虚拟用户查询权限。

额外管理员通过 `bkai.admins` 配置，由 `bin/sync-bkaidev.sh` 转成 `--admins user_a user_b` 传给 `bkai-init`：

```yaml
bkai:
  admins: ["user_a", "user_b"]
```

**Agent YAML 中禁止配置 `spec.admins`**（包括 `null` 与空列表），否则 `bkai-init validate` 会失败。
`--admins` 应用于本次同步的所有主、子智能体，在 Agent 配置写入后、发布前追加，仅支持插件智能体。

管理员是**只追加**语义：不会删除平台上已有的管理员，空列表时不传 `--admins`、不做任何处理。
追加失败或结果未确认时只输出异常，原因保留在最终报告中，不会中断当前 Agent 的发布和后续资源同步，
因此部署任务成功不代表管理员一定追加成功，需要查看日志确认。

## 维护约定

- `bkai.yaml` 的 `resources` 中列出的路径必须真实存在，否则 `bkai-init validate` 会失败；
- `resources` 中被引用的依赖（Skill、知识库、子智能体）要排在引用它们的 Agent 之前；
- 新增 Skill / 知识库后，需同时更新 `bkai.yaml` 与 `agents/bk_job_ai.yaml` 中的引用；
- Agent 不能配置 `metadata.version`（含 null）与 `metadata.space`，两者分别由平台分配和由 `--space` 传入；
- 自定义指令的 `agent_code` 省略即表示当前智能体；填其它智能体的 code 就变成「引用来源指令」，
  此时该 code 必须出现在 `subagents` 中，且来源指令必须已发布；
- 平台当前暂不支持自定义 Tools，`spec.tools` 保持空数组；
- 引用的 MCP **无论是否公开都要事先授权**，`bkai-init` 不会自动创建 MCP，也不会申请权限；
- 任何资源文件中都不要写入 Token、Secret 等敏感凭证，凭证统一通过部署时的环境变量注入。
