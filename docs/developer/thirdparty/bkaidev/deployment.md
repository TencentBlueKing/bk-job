# 部署与验收

本页整理上游 `bkai-init 0.1.6` 的通用部署方案，镜像路径和 Chart 字段均为上游示例，不是本仓库的直接配置模板。作业平台现有普通 Job、`/bk-job` 路径及默认发布行为见 [当前项目入口与差异](README.md#当前项目入口与差异)。执行顺序为“校验 → 计划预览 → 同步 → 确认后台结果”；配置写入、发布提交和实际运行验收不能相互替代。

## 基础镜像与模块镜像

平台侧先构建并发布 pip 包，再构建安装该包的基础镜像；接入模块使用平台提供的基础镜像，加入自己的接入目录。基础镜像 Dockerfile 通过 pip 安装已发布版本，不复制 bkai-init 源码。

模块镜像继承平台基础镜像中的 bkai-init 入口：

```dockerfile
ARG BKAI_INIT_IMAGE=bkai-init:local
FROM ${BKAI_INIT_IMAGE}

ARG BKAI_INIT_VERSION=0.1.6
USER root
RUN python -m pip install --no-cache-dir "bkai-init==${BKAI_INIT_VERSION}"
USER 10001:10001

WORKDIR /app
COPY --chown=10001:10001 package /app/package

CMD ["sync", "--confirm", "-f", "/app/package/bkai.yaml"]
```

构建前确认目标版本在所用包源中可用。默认基础镜像名仅为本地示例，实际地址由平台提供；Demo 的 `.env`、构建和同步方式见 [Quick Start](quick_start.md)。`user_scope` 在 Agent YAML 的 `spec` 中设置，不是镜像参数。

### 进入 Demo 容器

宿主机在 Demo 目录执行：

```bash
make exec
```

容器工作目录 `/app`，清单为 `./package/bkai.yaml`，`.env` 运行时挂载：

```bash
source .env
bkai-init validate -f ./package/bkai.yaml --space "$BKAI_SPACE_ID"
bkai-init sync -f ./package/bkai.yaml --space "$BKAI_SPACE_ID" --confirm --publish
```

`source .env` 依赖文件中的 `export`。仅开启 `BKPAAS_MULTI_TENANT_MODE` 时使用 `BKAI_TENANT_ID`，不要打印凭据。

## Helm 接入

以下为业务 Chart 的 values 示例，不是本仓库现有 Chart 配置：

```yaml
image:
    repository: registry.example.com/modules/bk-job-init
    tag: "0.1.6"
    pullPolicy: IfNotPresent

bkai:
    enabled: true
    tenantId: "<应用所在租户>"
    space: "<目标空间ID>" # 必填，替换为真实目标值
    packagePath: /app/package/bkai.yaml
    excludeResources: []
```

模块 Chart 的任务约定：

- 使用 `post-install` / `post-upgrade` Job，加载包含接入目录的模块镜像；`enabled` 控制是否运行。
- Job 参数必须带 `sync --confirm`，并传入 `packagePath`、`space`；`excludeResources` 转换为重复的 `--exclude-resource`。
- 多租户注入 `BKPAAS_MULTI_TENANT_MODE=true` 和实际 `tenantId`；单租户不要求 `tenantId`。
- 默认仅同步草稿。首次初始化主子智能体时，显式追加 `--publish --publish_config_only=0`，按先子后主顺序提交，不等待部署完成。
- 连接信息沿用业务 Chart 现有环境变量注入机制，不新增 `credentialsSecretName`；真实凭据不进入 values 示例、镜像或 Git。
- 需要部署变量时追加多个 `--var`，白名单和替换规则见 [CLI 与变量替换](cli.md#变量替换)。
- Job 失败先查看日志与线上状态，再决定重跑，不能假设所有资源已回滚。

## 日志与失败反馈

`plan` 和 `sync` 按 `bkai.yaml` 的资源事项展示进度、配置路径和当前步骤。正常接口仅保留缩进后的请求 URL，不展开输入、响应正文或接口处理结果；MCP 查询成功合并为一行。

以下为输出结构示例，计数和错误内容以实际执行为准：

```text
[3/8] 正在处理：Skill/job_skill
  配置：skills/job_skill/skill.yaml
  ✓ 打包完成
  ✓ 上传完成
  → 正在同步 Skill
      接口请求：POST https://bkapi.example.com/api/bk-aidev/prod/openapi/aidev/app/v1/skills/upsert/
  ✓ 同步完成

      【正在查询 MCP】bk-job-apigw-mcp agent_code: ai-job 成功
```

失败时展开格式化、脱敏的信息：

```text
[3/8] 正在处理：Skill/job_skill
  配置：skills/job_skill/skill.yaml
  ✓ 打包完成
  ✓ 上传完成
  → 正在同步 Skill
      接口请求：POST https://bkapi.example.com/api/bk-aidev/prod/openapi/aidev/app/v1/skills/upsert/
      请求参数：
          {
            "skill_code": "job_skill",
            "space_id": "<目标空间ID>"
          }
      接口响应：[HTTP 400]
          {
            "error": {
              "code": "INVALID_ARGUMENT",
              "message": "Dockerfile 基础镜像不在允许列表中: registry.example.com/team/skill:1.0"
            }
          }
  ✗ 同步 Skill失败：Dockerfile 基础镜像不在允许列表中

初始化已停止
  已完成 2 项，失败 1 项，未执行 5 项
  失败事项：Skill/job_skill
  失败步骤：同步 Skill
  排查建议：核对完整基础镜像名及目标环境允许列表
  反馈信息：执行时间、CLI 版本、trace_id 与服务端请求标识（如有）
```

先定位失败事项和步骤，不要仅截取最后一行错误。反馈保留资源、步骤、执行时间、CLI 版本、`trace_id` 和服务端请求标识，不粘贴原始凭据。

### 重试与异步状态

- 查询请求发生连接失败、超时、HTTP 429 或 5xx 时，分别等待 1、2、4 秒重试，最多请求 4 次。
- 写入请求及其它业务错误不会盲目重试；发布请求失败不自动重试。
- 知识库异步导入和 Agent `pending` / `running` 发布显示“已提交，后台处理中”，不代表全部处理完成。CLI 不轮询，到平台确认结果。
- 发布接口明确返回 HTTP 400，且原因为“智能体状态异常，无法发布，当前状态为：publishing”时，记录并继续，不计为失败。不能将其它 HTTP 400 视为成功。
- Skill 明确返回已有版本正在发布时跳过本次更新并继续；其它上传或发布错误仍停止。
- 已完成事项不自动回滚；重试前先用 `show` / `plan` 确认线上状态。

## 验收清单

- [ ] 命令执行结果符合预期，目标资源处理成功，黑名单中的资源未被覆盖。
- [ ] 再次执行 `show` / `plan`，核对名称、Prompt、模型、指令和资源关联。无法读取的字段不能据此认定一致。
- [ ] Skill 文件、envs 值、角色标签和知识库文档等无法回读内容，通过平台或对应资源结果另行确认；资源版本号不代表 Agent 已发布。
- [ ] 知识库 ZIP 导入任务已完成，目录与文档符合预期；再单独检查向量处理及检索效果。失败、部分成功、结果未知均不视为导入成功，不以 CLI 正常退出替代任务验收。
- [ ] CLI 未带 `--publish` 时确认 Agent 草稿。`make sync` 默认发布，需核对平台发布版本；发布提交成功不能替代实际部署结果。
- [ ] 验证对话、MCP 工具、Skill 和子智能体调用，不能将配置同步成功视为运行验证完成。

`show` 只查看线上配置，不查询权限；`plan` 预览同步动作和配置差异，不保证服务端写入或发布成功。知识导入任务没有查询入口时，将任务 ID 提供给平台维护方核实。

## 常见问题

| 现象                                        | 排查方向                                                                                                                                              |
| ------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------- |
| BK User HTTP 403 / `CROSS_TENANT_FORBIDDEN` | 请求租户与应用所在租户不一致；在 `.env` 设置 `BKAI_TENANT_ID` 为应用实际租户，不要沿用默认 `system`，重新 `source` 后执行。                           |
| 资源不存在或编码冲突                        | 确认资源已创建、code 正确且可唯一定位；同包本轮发布使用已同步配置，其它子智能体引用需有可用发布版本。                                                 |
| 智能体不存在或不在当前空间                  | 检查租户、空间及已有智能体归属；首次尚未创建时，确认平台支持 MCP 初始化查询规则，不创建空壳智能体绕过检查。                                           |
| 不支持的配置                                | 确认 CLI 和平台版本，不直接删除重要配置后覆盖已有 Agent；先与平台确认支持范围。                                                                       |
| `API_NOT_FOUND` / `1640401`                 | 网关路由未找到，不等于 MCP 不存在。检查 `api_name=bk-aidev`、网关环境、资源路径、接口发布状态；仅业务明确返回 MCP 不存在时检查 MCP 创建、发布和授权。 |
| `INITIALIZE_APP_MEMBERS_ERROR`              | 应用成员初始化调用 BK IAM 失败；将 `create_group` 路径、HTTP 状态、`request_id` 提供给权限中心或平台维护方，不是 Skill 或本地 YAML 错误。             |
| 关联 Skill 版本不存在                       | 确认 Skill 存在且可访问，平台已部署关联不依赖版本快照的修复；无需在 Agent 关联中补写版本。                                                            |
| `bk_username is not verified`               | 核对失败步骤、Trace ID 及下游实际传递的 `bk_username`；追加管理员不能替代上游身份校验。                                                               |
| 基础镜像不在允许列表                        | 核对渲染后的完整镜像名及目标租户有效白名单；修改 Dockerfile 后重新构建再同步。                                                                        |
| 同步中途失败                                | 先查看线上状态和差异，再修正重试；此前成功资源可能已写入，不会自动全部回滚。                                                                          |
