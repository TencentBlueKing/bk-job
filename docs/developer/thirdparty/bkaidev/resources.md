# 依赖资源协议

按需在 Package 中维护角色、Skill、知识库和子智能体。MCP 由接入方提前准备，只在 Agent 中引用；Tools 暂不支持。以下 Agent 配置片段须合并到同一个 `spec`，不要覆盖其它字段。

资源 code 规则见 [Package 与 Agent 协议](agent_package.md#资源-code-命名)，变量替换与覆盖保护见 [CLI 与变量替换](cli.md)。

## 资源定位与全租户范围

- 在目标租户、空间内按 code 定位：不存在则创建，存在则更新。遇到其它空间同 code、软删除占用等冲突时报错，不擅自迁移、恢复或覆盖。
- 资源引用须可访问且可唯一定位，不填环境相关平台 ID。资源文件不指定空间，调用方空间由 `--space` 统一传入，`metadata.space` 不能覆盖调用方空间。
- Agent / Collection / Skill / KnowledgeBase 的 `spec.generate_type: global` 表示全租户资源。`validate`、`plan`、`sync` 在发送资源请求前校验调用方空间，仅 `system-bkaidev` 可配置。
- Agent 兼容 `metadata.generate_type`，优先于 `spec`。平台仍要求多租户模式及 `system` 租户；资源创建后不能切换全租户范围。

## 角色（Collection）

通过 `collections/` 目录中的 Collection 资源维护角色，并加入 `bkai.yaml.resources`。先创建或更新角色，再同步 Agent。

### 资源定义

`collections/job_role.yaml`：

```yaml
apiVersion: bkai.tencent.com/v1
kind: Collection
metadata:
    code: bk_job_role
    name: 蓝鲸作业专家
spec:
    collection_type: role
    generate_type: space
    is_public: false
    tag_names: []
    content:
        - role: system
          content: |
              你是蓝鲸作业专家，仅回答与作业平台相关的问题。
        - role: user
          content: 如何查询作业执行状态？
        - role: assistant
          content: 我可以帮助你查询作业执行状态，请提供作业 ID。
```

| 字段                     | 类型 / 约束                          | 说明                                                                     |
| ------------------------ | ------------------------------------ | ------------------------------------------------------------------------ |
| `apiVersion` / `kind`    | `bkai.tencent.com/v1` / `Collection` | 纳入 Package 清单。                                                      |
| `metadata.code` / `name` | 必填 string                          | 按 code 定位已有角色；小写、字母开头、最长 64 位，允许下划线和中划线。   |
| `spec.icon`              | 可选 string / null，最长 1024 字符   | 省略时遵循平台默认或更新规则。                                           |
| `spec.collection_type`   | 仅 `role`，默认 `role`               | 仅管理角色资源。                                                         |
| `spec.content`           | 非空消息数组                         | 保留消息顺序，不合并为单段字符串。                                       |
| `spec.content[].role`    | `system` / `user` / `assistant`      | 首条必须为 system，且只能一条 system。                                   |
| `spec.content[].content` | 非空 string                          | 固定正文，不支持角色运行时变量；部署变量按文件替换规则处理。             |
| `spec.generate_type`     | `space` / `public` / `global`        | 默认选 space；public 强制公开，space 使用 is_public；global 约束见上文。 |
| `spec.is_public`         | boolean                              | 配合 generate_type 设置可见性。                                          |
| `spec.tag_names`         | 二维 string 数组，默认 `[]`          | 每项为一条标签路径。                                                     |

角色不定义变量和默认模型，模型统一配置在 Agent `model` 中。

### Agent 角色引用

将 Agent 的 inline Prompt 替换为：

```yaml
spec:
    prompt:
        type: collection
        code: bk_job_role
```

- collection 模式仅填 `type`、`code`；inline 模式仅填 `type`、`content`，两种互斥。不支持引用含模板变量的角色。
- 先处理清单内 Collection，再解析 `prompt.code`；未在清单中的角色只作已有资源引用，不需平台 ID。
- 切换到 inline 时平台清空角色关联和快照。
- 更新角色后，须再次同步引用它的 Agent；已有 Agent 不自动使用新角色正文，对外生效还需发布。
- `--exclude-resource collection/bk_job_role` 只跳过角色写入，Agent 仍可引用线上角色。也不希望刷新 Agent 快照时，同时排除 Agent。
- 使用 `validate` / `show` / `plan` / `sync` 管理角色，检查正文、消息顺序、角色类型、标签和公开配置。失败先修正配置或权限，再重试依赖 Agent，不能假设此前成功资源已回滚。

## MCP

仅支持 APIGW MCP。接入方先创建、发布 MCP Server，并提前完成接入应用及目标智能体所需授权，再在 `spec.mcps` 引用。公开可见、已发布都不代表允许 bkai-init 或目标智能体使用。

```yaml
spec:
    mcps:
        - type: apigw
          code: bk-job-apigw-mcp
          is_selected_all: false
          selected_tools:
              - tool_name: get_job_status
```

| 字段                     | 类型 / 约束  | 说明                                                                                                                         |
| ------------------------ | ------------ | ---------------------------------------------------------------------------------------------------------------------------- |
| `mcps[].type`            | 固定 `apigw` | 无需填地址。                                                                                                                 |
| `mcps[].code`            | string       | 网关中完整、准确的 MCP Server 编码，小写、字母开头、最长 64 位，允许下划线和中划线。                                         |
| `mcps[].is_selected_all` | boolean      | 显式 true / false 优先；省略且 selected_tools 缺失或为空时为 true，仅提供非空 selected_tools 时为 false。plan 与 sync 一致。 |
| `mcps[].selected_tools`  | 对象数组     | 每项 `tool_name: string`；全选时建议省略或 `[]`。                                                                            |
| `mcps[].id`              | 禁止         | 包括 null，均报错；仅通过 code 引用。                                                                                        |

默认启用全部工具，仅填 `type`、`code` 即可，也可显式 `is_selected_all: true`。全选会包含 MCP 后续新增工具；要固定范围则显式填 false 和工具清单。

MCP 不属于独立同步资源。工具不自动创建、不申请或授予权限；权限不足或查询不到时停止，不绕过授权。接入方可使用目标版本 APIGW MCP Server 初始化接口创建或更新 MCP，通过 `target_app_codes` 主动授权所需应用，具体支持项以网关官方文档为准。

### 初始化与空间校验

bkai-init 使用 Agent `metadata.code` 作为 `agent_code` 查询 MCP；租户、空间来自 `--tenant-id`、`--space`。不新增配置开关，不要求智能体编码与调用应用编码相同。

| 当前租户中的 Agent | 规则                                                                        |
| ------------------ | --------------------------------------------------------------------------- |
| 尚未创建           | 校验调用应用对目标空间的权限后，按 APIGW 数据查询 MCP，不要求先创建 Agent。 |
| 已经创建           | 应用须有 Agent 所在空间权限，且该空间须与 `--space` 一致，不匹配则停止。    |

只按当前租户判断是否创建，其它租户同名 Agent 不影响本次初始化。目标平台须支持此规则，不能创建空壳资源绕过检查。查询成功不代表运行调用已验证，授权前提不变。

## Skill

接入方维护 Skill 包，通过 Package 清单同步，再由 Agent 引用。根目录必须包含 `SKILL.md`，Skill 内容及环境变量依赖声明由接入方维护。

### 资源与镜像

`skills/job_skill/skill.yaml`：

```yaml
apiVersion: bkai.tencent.com/v1
kind: Skill
metadata:
    code: job_skill
    name: 作业查询 Skill
    # 不填 version，由平台根据 ZIP 内容自动确定版本
    description: 查询作业执行信息
spec:
    generate_type: space
    is_public: false
    tag_names: []
```

code 小写、字母开头、最长 64 位，允许下划线和中划线；Agent 引用同一 code。

版本可省略，不以 Package 版本兜底。平台比较 ZIP hash：首次创建 `0.0.1`；内容变化时在最新版本上增加补丁号，未变化则覆盖当前版本。无法取得旧 ZIP hash 时按版本覆盖处理；无法读取新上传 ZIP 时停止。显式版本须为 `x.x.x`，支持同版本覆盖，缺失版本快照时由平台创建。

仅在已有根目录 `skills/job_skill/Dockerfile` 中引用镜像：

```dockerfile
FROM {{ BKAI_AGENT_IMAGE_REPO | required }}/skill:1.0
```

自定义镜像先同步到目标环境镜像仓库，由模块 Helm Chart 上报、平台开白，再引用。核对渲染后的完整镜像名及目标环境有效允许列表。仓库前缀传入不带末尾 `/` 的值，原文冲突见 [接入入口](README.md#原文冲突镜像仓库前缀)。

只支持根目录已有 Dockerfile，不提供 `spec.base_image`，不自动生成文件。部署变量替换后写入上传包，不修改本地源文件；嵌套 Dockerfile、`SKILL.md` 和脚本不参与替换。

### Agent 关联与环境变量

以下是关联配置，不是 Skill 资源定义：

```yaml
spec:
    skills:
        - code: job_skill
          envs:
              - key: BK_JOB_MODULE
                value: bk-job
```

| 字段                    | 类型 / 约束               | 说明                                                                                                    |
| ----------------------- | ------------------------- | ------------------------------------------------------------------------------------------------------- |
| `skills[].code`         | 必填 string               | Skill 已同步，且目标租户、空间可访问。                                                                  |
| `skills[].envs`         | 可选对象数组，允许 `[]`   | 省略、空数组与显式 value 含义不同，不能假设清空或保留全部线上值。                                       |
| `skills[].envs[].key`   | 必填 string，最长 64 字符 | 同一 Skill 下不重复，须在 Skill 环境变量依赖声明中存在。                                                |
| `skills[].envs[].value` | 可选 string，允许空字符串 | 保留首尾空白；CLI 不把省略 value 转为空字符串。                                                         |
| `skills[].version`      | 不纳入关联输入            | 不在 Agent YAML 指定版本，只要求 Skill 存在且可访问，不依赖 SkillVersion 快照；需目标平台部署对应修复。 |

示例 `BK_JOB_MODULE` 需先在 Skill 环境变量依赖中声明；仓库只保存非敏感值。`envs: []` 不表示清空全部变量，最终值仍须满足依赖声明；未填写 value、空字符串和默认值不能混为一谈。

Agent 关联使用当前 Skill 版本。仅排除 Skill 同步不阻止 Agent 的关联或 envs 更新；保护平台手工维护的 Agent 时排除整个 Agent。

## 子智能体

同包使用 `--publish` 时先同步子智能体，提交发布后继续主智能体，不等待部署；来源指令用本轮已同步配置。包外或本轮未发布的子智能体必须可访问，且已有可用发布版本，所引用指令须在该版本中存在。

`agents/bk_job_exec.yaml`：

```yaml
apiVersion: bkai.tencent.com/aidev-agent/v1
kind: Agent
metadata:
    code: ai-job-exec
    name: 作业执行助手
spec:
    prompt:
        type: inline
        content: 你是作业执行助手，帮助用户理解任务信息。
    model:
        llm_code: aidev-chat-auto
        context_window: 16
    conversation_settings:
        commands:
            - id: execute_job
              name: 分析作业执行
              icon: null
              content: 请分析任务 {{ task_id }} 的执行信息。
              components:
                  - type: text
                    key: task_id
                    name: 任务 ID
                    required: true
    tools: []
```

主 Agent 的关联片段：

```yaml
spec:
    subagents:
        - code: ai-job-exec
    # 可选：引用来源指令；仅关联子智能体时无需此 commands 项
    conversation_settings:
        commands:
            - id: execute_job
              agent_code: ai-job-exec
              alias: 作业执行助手
```

- `subagents[].code` 必填，规则与主 Agent 一致：`ai-` 开头，总长 5–16 位；名称、平台 ID 和关联信息由工具获取。
- 来源 commands 的 `id`、`name`、`agent_id` 及可选 `status` 由工具读取，不需要重复填写；来源须包含 `execute_job`。
- 首次初始化推荐整包 `sync --confirm --publish --publish_config_only=0`。单独同步主 Agent 时，未选中的子 Agent 不会自动同步，仍需可用发布版本。
- 指令引用只接受 `id`、`agent_code`、`alias`，展开规则及失败边界见 [引用与展开规则](agent_package.md#引用与展开规则)。

## 知识库

每个知识库使用独立目录，维护配置、Markdown 和图片，Agent 通过 code 引用。code 仅小写英文、数字或下划线，字母开头、最长 64 位，不支持中划线。

### 资源定义

`knowledgebases/bk_job_docs/knowledgebase.yaml`：

```yaml
apiVersion: bkai.tencent.com/v1
kind: KnowledgeBase
metadata:
    code: bk_job_docs
    name: 作业平台知识库
    description: 作业平台使用文档
spec:
    generate_type: space
    is_public: false
    config: {}
    tag_names: []
```

`metadata` 定义标识、名称、描述，`spec` 定义可见性、知识库 config 和标签；具体 config 按目标平台能力填写，不与 Agent 检索配置混用。未指定 code 时使用知识库目录名。

### Agent 检索配置

```yaml
spec:
    knowledgebases:
        items:
            - code: bk_job_docs
        retriever_code: ""
        query_function: mixed
        recall_channels: [dense, sparse]
        rrf_weights:
            dense: 0.7
            sparse: 0.3
        document_fragment_count: 20
        polish: true
        origin: false
        is_response_when_no_knowledgebase_match: true
        rejection_message: 未找到相关作业文档，请补充问题信息。
```

以下字段直接位于 `knowledgebases` 下；`items` 是关联列表，其余统一作用于全部关联知识库，不增加 `retrieval` 层。

| 字段                                      | 类型 / 约束                  | 说明                                                                 |
| ----------------------------------------- | ---------------------------- | -------------------------------------------------------------------- |
| `items[].code`                            | string                       | 已同步、可访问的知识库编码，遵循知识库命名规则。                     |
| `retriever_code`                          | 可选 string                  | 检索器编码。                                                         |
| `query_function`                          | `semantic` / `mixed` / `sql` | 兼容检索方式字段，不代表所有知识库支持 SQL；与召回通道一致。         |
| `recall_channels`                         | `dense` / `sparse` 数组      | 与 query_function 一致。                                             |
| `rrf_weights`                             | string → number 对象         | 每项 0–1；示例 dense、sparse，不要求总和为 1。                       |
| `document_fragment_count`                 | integer                      | 返回文档片段数，示例 20。                                            |
| `polish` / `origin`                       | boolean / boolean            | 检索结果处理开关；首次按示例填写，与平台确认目标版本返回效果后调整。 |
| `is_response_when_no_knowledgebase_match` | boolean                      | 未命中时是否允许根据通识回答。                                       |
| `rejection_message`                       | string，可空                 | 拒答文案。                                                           |

明确填写所需检索配置，更新前查看差异，不通过省略字段假定保留线上原值。

### 文档 ZIP 同步

1. 先同步知识库配置，再打包目录内 Markdown 和图片，保留相对路径。
2. 向平台申请临时上传 URL，直接上传到 BKRepo，由平台核对上传状态、大小及摘要。
3. 提交 ZIP 导入，取得任务 ID 后继续后续同步，不等待后台任务完成。“已提交，后台处理中”不是导入成功。
4. 在平台按任务 ID 确认导入状态、目录和文档；没有查询入口时，提供任务 ID 给平台维护方核实。失败、部分成功、结果未知均不视为成功，不以 CLI 正常退出替代任务验收。

**包含文档时按完整知识库根目录镜像同步，会删除线上多余内容。** 必须确认本地目录完整；无文档时只同步配置，不清空线上文档。导入完成也不等于向量处理、检索或问答验证完成。

文件约束：

- 仅 Markdown 和图片；排除根目录 `knowledgebase.yaml`、`.DS_Store`、`.git`、`__pycache__`。
- 拒绝符号链接和其它文件类型。
- ZIP 最大 500 MiB、文件总大小最大 2 GiB、最多 10000 个文件；平台继续校验路径和解压安全。
- `show` / `plan` 不比较文档内容；验收需另行确认实际导入及检索效果。
