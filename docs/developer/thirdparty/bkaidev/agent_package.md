# Package 与 Agent 协议

Agent Package 用 `bkai.yaml` 声明同步清单。先确定稳定的 code、目标空间和依赖资源，再维护 Agent 配置；连接信息统一由调用方注入，不写入资源文件。协议目标版本为 `bkai-init 0.1.6`。

## 资源 code 命名

code 是创建、更新和关联的稳定标识，不是展示名称；所有资源 code 使用小写，`name` 可以使用中文。

| 资源               | 完整匹配正则                | 约束与示例                                                                                                     |
| ------------------ | --------------------------- | -------------------------------------------------------------------------------------------------------------- |
| 主 / 子智能体      | `^ai-[a-z][a-z0-9-]{1,12}$` | `ai-` 开头，前缀后首位小写英文，其余小写英文、数字或中划线，总长 5–16 位；如 `ai-job`、`ai-job-exec`。         |
| 角色 / MCP / Skill | `^[a-z][a-z0-9_-]{0,63}$`   | 小写英文开头，其余小写英文、数字、下划线或中划线，1–64 位；如 `bk_job_role`、`bk-job-apigw-mcp`、`job_skill`。 |
| 知识库             | `^[a-z][a-z0-9_]{0,63}$`    | 小写英文开头，其余小写英文、数字或下划线，1–64 位；如 `bk_job_docs`，不支持中划线。                            |

智能体错误示例：`job-agent`、`ai-Job`、`ai-job_exec`、`ai-a`。角色 / MCP / Skill 错误示例：`Job_role`、`1job`。知识库错误示例：`bk-job-docs`、`Job_docs`。知识库 code 直接用于 Milvus 集合名，因此不能使用中划线。

资源定义、关联引用、`commands[].agent_code`、CLI 选择和黑名单均按对应规则校验，变量替换后再校验。不兼容不符合新规则的旧 code，不自动转小写、重命名或迁移；平台保留旧数据不代表 CLI 接受旧编码。

模型编码、外部应用编码、指令 ID、组件 key 和环境变量名不适用此表，例如 `BK_JOB_MODULE` 可以大写。YAML kind 为 `Agent` / `Collection` / `Skill` / `KnowledgeBase`，CLI 类型使用小写，例如 `agent/ai-job`；文件名不代替资源 code。

## 接入目录与清单

Demo 目录及镜像工作目录：

```text
demo/
├── .env                 # 从 .env.example 复制，不提交
├── .env.example
├── Makefile
├── Dockerfile
└── package/
    ├── bkai.yaml
    ├── agents/
    ├── skills/
    └── knowledgebases/

/app/
├── .env                 # 运行时挂载
└── package/
    └── bkai.yaml
```

角色按需放在 `collections/`。知识库下每个目录对应一个知识库，`knowledgebase.yaml` 定义 code，未指定时使用目录名；Markdown 和图片保留目录层级。未使用的资源无需创建目录。

`bkai.yaml` 只声明本次需要同步的资源，路径相对于清单所在目录。以下为自有 Package 示例；只用 inline Prompt 时可移除角色项：

```yaml
apiVersion: bkai.tencent.com/v1
kind: AgentPackage
metadata:
    name: bk-job
    version: "1.0.0"
resources:
    - collections/job_role.yaml
    - skills/job_skill
    - knowledgebases/bk_job_docs
    - agents/bk_job_exec.yaml
    - agents/bk_job_ai.yaml
```

资源文件不指定空间，统一通过 `--space` / `BKAI_SPACE_ID` 传入；默认 `bkaidev`，多租户需完整 ID。Package `metadata.version` 不用于 Agent 发布，也不作为 Skill 版本兜底。完整依赖定义见 [依赖资源协议](resources.md)。

## Agent 完整示例

以下为 `agents/bk_job_ai.yaml`。后续片段应合并到同一个 `spec`，不要重复声明或覆盖其它配置。先替换名称、code、Prompt 和模型，再按实际依赖调整关联。

```yaml
# AIDEV Agent CLI Manifest v1
apiVersion: bkai.tencent.com/aidev-agent/v1
kind: Agent
metadata:
    code: ai-job
    name: 蓝鲸作业智能助手
    description: 面向蓝鲸作业平台的智能助手
spec:
    user_scope: public
    prompt:
        type: inline
        content: |
            ## Role

            蓝鲸作业智能助手

            ## Constraints

            - 仅回答与蓝鲸作业平台相关的问题。
    model:
        llm_code: aidev-chat-auto
        fallback_model: null
        temperature: 0.7
        context_window: 16
        llm_token_limit: 28000
        max_tokens: 4096
        tool_output_compress_thrd: 4096
    conversation_settings:
        opening_remark: 你好，我可以帮助你查询作业执行情况。
        predefined_questions:
            - 如何查询作业执行状态？
        enable_chat_session: true
        enable_word_selection_popup: true
        commands:
            - id: query_job_status
              name: 查询作业状态
              icon: null
              agent_code: ai-job
              alias: null
              content: |
                  查询作业 {{ job_id }} 在 {{ environment }} 环境中的执行状态。
                  最多返回 {{ limit }} 条记录。
                  补充信息：{{ context }}
              components:
                  - type: text
                    key: job_id
                    name: 作业 ID
                    placeholder: 请输入作业 ID
                    default: ""
                    required: true
                    hide: false
                  - type: select
                    key: environment
                    name: 环境
                    placeholder: 请选择环境
                    default: test
                    required: true
                    hide: false
                    options:
                        - label: 测试
                          value: test
                        - label: 生产
                          value: production
                  - type: number
                    key: limit
                    name: 返回数量
                    default: 10
                    min: 1
                    max: 100
                    required: false
                    hide: false
                  - type: textarea
                    key: context
                    name: 补充信息
                    placeholder: 输入或引用需要分析的文本
                    default: ""
                    rows: 4
                    required: false
                    hide: false
              enable_fill_back: true
              fill_back_component_key: context
              fill_regx: null
              support_upload:
                  vision: false
                  file: false
            - id: execute_job
              agent_code: ai-job-exec
              alias: 作业执行助手
    subagents:
        - code: ai-job-exec
    mcps:
        - type: apigw
          code: bk-job-apigw-mcp
          is_selected_all: false
          selected_tools:
              - tool_name: get_job_status
    skills:
        - code: job_skill
          envs:
              - key: BK_JOB_MODULE
                value: bk-job
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
    tools: []
```

## 基础与模型字段

| 字段                              | 类型 / 约束              | 含义                                                                                        |
| --------------------------------- | ------------------------ | ------------------------------------------------------------------------------------------- |
| `apiVersion` / `kind`             | 固定字符串               | `bkai.tencent.com/aidev-agent/v1` / `Agent`。                                               |
| `metadata.code`                   | 必填 string              | 按 code 定位智能体，规则见上文。                                                            |
| `metadata.name` / `description`   | string                   | 名称必填，描述可选；名称由平台校验。                                                        |
| `spec.user_scope`                 | 可选 `public` / `custom` | 所有人 / 指定人使用，不接受 null 或其它值。                                                 |
| `prompt.type`                     | `inline` / `collection`  | 两种模式互斥，collection 为平台角色资源。                                                   |
| `prompt.content`                  | string                   | inline 必填正文；collection 不填写。                                                        |
| `prompt.code`                     | string                   | collection 必填角色 `metadata.code`，不填平台 ID；inline 不填写。                           |
| `model.llm_code`                  | string                   | 协议默认 `aidev-chat-auto`；省略时由 CLI 补齐后提交。                                       |
| `model.fallback_model`            | string / null            | null 表示不指定备用模型。                                                                   |
| `model.temperature`               | number / null            | 生成随机性，平台范围 0–2。                                                                  |
| `model.context_window`            | integer / null           | 仅接受 1–30 整数，单位为会话轮次，不是 token 或 K；256、字符串 `"16"`、小数、布尔值均报错。 |
| `model.llm_token_limit`           | integer / null           | 上下文 token 上限，平台最小值 1024。                                                        |
| `model.max_tokens`                | integer / null           | 回复 token 上限，平台范围 1–20480。                                                         |
| `model.tool_output_compress_thrd` | integer / null           | 工具输出压缩阈值，平台最小值 1024。                                                         |
| `tools`                           | 空数组                   | 本期不支持 Tools 配置。                                                                     |

模型还须满足目标模型自身能力限制。Agent 禁止配置 `version`，包括 null；发布版本由平台分配。角色引用方式见 [角色](resources.md#角色collection)。

## 使用范围与管理员

```yaml
spec:
    user_scope: public # 指定人使用填 custom
```

- 主子智能体分别设置，不继承。省略时，新建沿用平台默认范围，已有智能体保持原范围。
- `custom` 的人员名单在平台管理；`public` 不免除入口鉴权、租户隔离和依赖授权，也不等于资源公开属性。
- 使用范围修改会影响已有发布版本，不依赖本次 `--publish`。先通过 `plan` 确认，再同步；目标 CLI 和平台须支持，设置失败或回读不一致时停止，不静默忽略。
- 管理员仅通过 `--admins alice bob` 或 Make `ADMINS` 传入实际 `bk_username`，适用于所选主子 Agent，仅支持插件智能体。YAML 禁止 `spec.admins`，包括 null 和空列表。
- 在 Agent 配置写入后、发布前追加管理员。单租户、多租户均直接透传，不查询用户管理；调用方自行转换 `login_name`。
- 追加失败或结果未确认时输出异常，在最终报告保留原因，继续当前 Agent 发布及后续资源同步；已写入配置或权限不回滚。
- 不传管理员参数则不处理；只追加不删除已有成员。`plan` 只展示管理员及追加意图，不校验用户映射，也不将无法回读的名单判为一致。
- 仅未提供执行用户名且开启多租户时，调用应用才需 BK User lookup 权限，用于默认 `bk_admin`。执行者与管理员是不同输入，追加管理员不能替代发布身份校验。

## 对话设置

以下字段位于 `spec.conversation_settings`：

| 字段                          | 类型 / 默认值    | 约束                                                 |
| ----------------------------- | ---------------- | ---------------------------------------------------- |
| `opening_remark`              | string，空字符串 | 开场白最长 1024 字符。                               |
| `predefined_questions`        | string[]，`[]`   | 预设问题。                                           |
| `commands`                    | Command[]，`[]`  | 本地定义和来源引用使用同一数组，展开后满足指令结构。 |
| `enable_chat_session`         | boolean，`true`  | 是否启用多会话。                                     |
| `enable_word_selection_popup` | boolean，`false` | 是否启用划词弹出菜单。                               |

### 指令 commands

| 字段                      | 类型 / 默认值               | 约束                                                                           |
| ------------------------- | --------------------------- | ------------------------------------------------------------------------------ |
| `id`                      | 必填 string                 | 指令标识，同一 Agent 内唯一，避免与默认指令 ID 冲突；来源引用填写来源指令 ID。 |
| `name`                    | string                      | 自定义指令必填，引用简写由 CLI 从来源补齐。                                    |
| `icon`                    | string / null，无平台默认值 | 完整提交必须提供，无图标显式填 null。                                          |
| `agent_code`              | string / null               | 来源引用填来源 Agent code；本地定义省略表示当前 Agent。                        |
| `alias`                   | string / null，null         | 目标智能体展示别名。                                                           |
| `content`                 | string / null，null         | 正文使用 `{{ key }}` 引用组件，不是 prompt 字段。                              |
| `components`              | Component[]，`[]`           | 组件结构见下文。                                                               |
| `enable_fill_back`        | boolean，`false`            | 是否启用引用文本回填。                                                         |
| `fill_back_component_key` | string / null               | 开启回填时必填，须匹配某个组件 key。                                           |
| `fill_regx`               | string / null，null         | 保留平台字段拼写 `regx`。                                                      |
| `support_upload`          | 对象，`{}`                  | 仅 `vision`、`file` 两个 boolean 键；声明不代表执行链路支持上传。              |

### 组件 components

| 字段                   | 类型 / 默认值                            | 约束                                          |
| ---------------------- | ---------------------------------------- | --------------------------------------------- |
| `type`                 | 必填 string                              | 仅 `text`、`textarea`、`number`、`select`。   |
| `key`                  | 必填 string                              | 同一指令内唯一，用于正文引用。                |
| `name` / `placeholder` | string / null，null                      | 展示名称和输入提示。                          |
| `default`              | string / integer / boolean / list / null | 按控件类型填写；select 默认值须匹配 options。 |
| `required`             | boolean / null，`false`                  | 建议使用明确布尔值。                          |
| `hide`                 | boolean，`false`                         | 是否隐藏。                                    |
| `rows`                 | integer / null，null                     | textarea 行数。                               |
| `min` / `max`          | integer / null，null                     | 数值边界，整数默认值须在范围内。              |
| `options`              | 对象数组 / null，null                    | `{label: string, value: string或number}`。    |

只填写表内支持的组件字段，不配置 `multiple`；文本回填在指令层设置，不在组件内重复配置。CLI 校验 key 唯一、引用存在、默认值类型与选项匹配、数值范围有效。

## 引用与展开规则

1. **本地指令**填写完整定义，`agent_code` 可省略，不手填平台 ID；平台可能自动生成当前智能体默认指令，无需复制到接入文件中。
2. **来源指令**通过 `agent_code + id` 定位，`agent_code` 必须出现在 `subagents`。来源引用仅接受 `id`、`agent_code`、`alias`；不新增 `reference` 层。要修改正文或组件时，另建本地指令。
3. **展开内容**包括来源 `name`、`icon`、`content`、`components`、回填和上传设置；`alias` 允许本地覆盖。同包本轮发布读取已同步配置，其它情况读取来源最新发布版本。
4. **失败即停止**：来源不存在、无权访问、缺少本轮同步配置或可用发布版本、指令已删除、code 无法唯一定位时，不能用空配置替代。
5. **回填**开启时目标 key 必须存在；平台标记目标组件 `fill_back` 并同步 `fill_regx`，关闭时重置组件回填字段。
6. **资源引用**仅填写 code，不填平台资源 ID；指令 `id` 是指令标识，仍需保留。资源须在指定租户及目标空间可访问，无法唯一定位时先修正资源或空间，不换成其它环境的 ID。

首次主子初始化建议整包 `sync --confirm --publish --publish_config_only=0`；详情见 [子智能体](resources.md#子智能体) 与 [发布规则](cli.md#发布规则)。
