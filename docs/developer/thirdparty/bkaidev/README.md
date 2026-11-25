# AIDEV 智能体接入参考

面向需要为作业平台准备 AIDEV 智能体接入目录、同步资源和编排部署任务的开发者。本目录整理 Agent Package v1 的资源协议及 `bkai-init 0.1.6` 使用约定；其中的 `ai-job`、`job_skill` 等为上游接入示例，不用于替代本仓库已有资源包或 Chart 配置。

## 阅读入口

| 要解决的问题                       | 文档                                      | 内容范围                                                 |
| ---------------------------------- | ----------------------------------------- | -------------------------------------------------------- |
| 如何先跑通 Demo？                  | [Quick Start](quick_start.md)             | `.env`、构建、校验、预览、同步与管理员追加               |
| 如何独立调用同步工具？             | [CLI 与变量替换](cli.md)                  | 包安装、运行参数、资源选择、覆盖保护、变量和 Python 调用 |
| 如何嵌入部署流程、确认结果和排障？ | [部署与验收](deployment.md)               | 模块镜像、Helm Job、日志、异步结果与常见问题             |
| 如何编写清单、Agent、指令和组件？  | [Package 与 Agent 协议](agent_package.md) | 目录、code、模型、使用范围、快捷指令和引用展开           |
| 如何准备和关联依赖资源？           | [依赖资源协议](resources.md)              | 角色、MCP、Skill、子智能体和知识库                       |

首次接入先按 Quick Start 完成资源同步，再按需查阅 CLI、部署方式和资源协议。正式维护配置前，应先阅读下面的接入边界。

## 当前项目入口与差异

本仓库已包含 AIDEV 资源包及部署任务，`bkai.enabled` 默认 `false`。维护现有接入时，先阅读 [资源目录说明](../../../../support-files/bk-aidev/bk-job/README.md)，不要按通用示例另建一套。

| 项目           | 当前仓库实现                                                                                                                                   | 与本目录上游方案的关系                                                                                                                                 |
| -------------- | ---------------------------------------------------------------------------------------------------------------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| 资源与同步脚本 | [Package 入口](../../../../support-files/bk-aidev/bk-job/bkai.yaml)、[同步脚本](../../../../support-files/bk-aidev/bk-job/bin/sync-bkaidev.sh) | 镜像内清单为 `/bk-job/bkai.yaml`，不是 Demo 的 `/app/package/bkai.yaml`；项目命名按资源目录说明。                                                      |
| 基础镜像       | [bkAidev.Dockerfile](../../../../support-files/kubernetes/images/migration/bkAidev.Dockerfile) 引用 `bk-aidev-init:0.1.5`                      | 本目录目标是 0.1.6，不能仅凭本文认定当前镜像具备全部新能力。                                                                                           |
| Chart 参数     | [values.yaml](../../../../support-files/kubernetes/charts/bk-job/values.yaml) 的 `bkai.*`                                                      | 镜像字段为 `bkai.image.*`，默认 `publish: true`；`space` 留空按多租户取 `system-bkaidev` / `bkaidev`。不能直接套用上游顶层 image 示例。                |
| Job 编排       | [同步 Job](../../../../support-files/kubernetes/charts/bk-job/templates/job-migration/sync-bkaidev-job.yaml)                                   | 使用普通 Job，不是 post-install / post-upgrade hook；开启网关同步时通过 initContainer 等待网关 Job。MCP 注册还需 `bkApiGatewayConfig.syncMcpServers`。 |
| 重试与错误     | `bkai.syncMaxRetry`、`syncRetryInterval`、`ignoreSyncError`                                                                                    | 项目脚本有同步级重试；不能与 bkai-init 内部只重试查询的策略混为一谈。忽略错误后的 Job 成功不等于同步成功。                                             |
| 项目预渲染     | [render_placeholders.py](../../../../support-files/bk-aidev/bk-job/bin/render_placeholders.py)                                                 | 调用工具前按名单替换 `${JOB_URL_BASE}`、`${JOB_AI_GENERATE_TYPE}`；多租户 Agent 使用 global，单租户使用 public，不能替换知识库其它魔法变量。           |
| Skill 版本     | [现有 Skill 说明](../../../../support-files/bk-aidev/bk-job/skills/README.md) 仍描述 Package 版本兜底                                          | 与本目录 0.1.6 的“省略版本时按 ZIP 内容决定版本”不同。维护时须按实际工具版本判断，不混用两套规则。                                                     |

## 接入边界

- 接入方维护接入目录、Agent 配置和依赖资源，并在部署流程中注入连接信息、执行同步。真实凭据不进入资源文件、镜像、values 示例或 Git。
- 平台提供内置 `bkai-init` 的基础镜像和应用态接口。常规接入通过命令调用，不需要自行对接内部服务。
- 同步范围包括 Agent、Collection（角色）、Skill、KnowledgeBase 的创建与更新，以及按 `admins` 追加开发者中心管理员。知识库文档仅接受 Markdown 与图片。
- MCP 只支持 APIGW MCP，由接入方提前创建、发布和授权；公开 MCP 也不免授权。工具不创建 MCP，不自动申请或审批授权。
- 不提供 Tools 同步、角色运行时变量、空间 IAM 授权或自动回滚；Agent 的 `tools` 保持空数组。接口不支持或权限不足时，不通过 private 接口或提前创建空壳资源绕过检查。

## 同步前必须明确的事项

1. **code 与空间是定位依据。** 所有资源 code 使用小写，知识库不支持中划线。资源文件不指定空间；多租户必须显式提供完整空间 ID，例如 `system-bkaidev`，CLI 不自动拼接租户前缀。
2. **MCP 授权和镜像开白是前提。** 完成接入应用及目标智能体所需 MCP 授权；Skill 自定义镜像先同步到目标环境镜像仓库，由模块 Helm Chart 上报、平台开白，再在 Skill 根目录 Dockerfile 中引用。
3. **配置同步可能覆盖线上修改。** 先 `validate`、`plan`，再 `sync --confirm`；保护平台手工维护的 Agent 时排除整个 Agent，不能仅排除它依赖的 Skill。
4. **知识库文档同步有删除行为。** 有文档时，以本地完整目录镜像同步并删除线上多余文档；没有文档时仅同步配置，不清空线上文档。
5. **提交成功不等于部署或导入完成。** `--publish` 才发起 Agent 发布，常规发布使用 `--publish --publish_config_only=0`。CLI 不等待后台；同包子智能体提交发布后即可继续关联主智能体，无需等待子智能体部署完成。
6. **失败不回滚。** 中途失败时，先用 `show` / `plan` 确认线上状态，再修正并限定范围重跑；未选中的依赖不会自动同步。

## 资料来源与版本

| 来源                                                    | 版本 / 日期                                         | 本目录使用范围                                                      |
| ------------------------------------------------------- | --------------------------------------------------- | ------------------------------------------------------------------- |
| 《AIDEV 智能体接入与资源初始化方案 · Agent Package v1》 | 目标版本 `bkai-init 0.1.6`；原文更新于 `2026-10-08` | Demo、CLI、Python、镜像与 Helm 接入、资源协议、发布规则、验收及排障 |

目标平台需具备文中对应能力，尤其是首次初始化 MCP 查询、`user_scope`、管理员透传、常规发布及 Skill 关联不依赖版本快照等能力。示例地址均为占位地址，包源和可拉取的配套基础镜像由平台提供，不假设包已发布到公共 PyPI。

原文提到的 APIGW MCP Server 创建 / 更新、初始化接口及 `target_app_codes` 主动授权能力，以目标网关版本的官方文档为准；`spec.mcps[].code` 必须填写网关返回的完整 MCP Server 编码。

### 原文冲突：镜像仓库前缀

原文 `.env` 注释要求 `BKAI_AGENT_IMAGE_REPO` 非空时以 `/` 结尾，但构建说明要求不带末尾 `/`，Dockerfile 模板也已包含分隔符：

```dockerfile
FROM {{ BKAI_AGENT_IMAGE_REPO | required }}/skill:1.0
```

本目录示例按该模板传入**不带末尾 `/`** 的仓库前缀，例如 `registry.example.com/team`。Demo Skill 使用 `required`，因此运行 Demo 时该变量不能留空；应核对渲染后的完整镜像名及目标平台有效允许列表。

## 扩展同步工具时的接口边界

仅扩展工具时需要关注：资源接口统一使用 AIDEV 源码中 `src/aidev/aidev/resource/openapi/services/app` 下的应用态接口，不直接调用平台内部服务。显式执行者使用 `bk_username`；只有未传执行者且开启多租户时，才查询默认 `bk_admin`，不增加平台身份查询接口。管理员追加通过平台 app 接口执行。
