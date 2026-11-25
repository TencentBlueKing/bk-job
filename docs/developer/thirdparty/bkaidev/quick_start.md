# Quick Start

使用 AIDEV 提供的 Demo 跑通资源同步，目标版本为 `bkai-init 0.1.6`。Demo 包含 Skill、知识库、子智能体 `ai-bkai-child` 和主智能体 `ai-bkai-demo`，工具自动按依赖顺序同步。

## 前提

- 宿主机已准备可用的 Podman 或 Docker。
- 平台已提供可拉取的基础镜像，以及可安装 `bkai-init==0.1.6` 的可信包源。
- 目标环境已开放应用接口、目标空间权限和 Skill 基础镜像白名单；使用 MCP 时，提前完成创建、发布及授权。
- 以下 `src/bkai-init/demo` 路径属于 **AIDEV 仓库**，不是本仓库现有目录。也可将完整 Demo 目录复制到自己的项目中维护。

修改包版本或 `package/` 内的 YAML、文档、Skill Dockerfile 后，都要重新 `make build`。仅修改 `.env` 运行配置时，下次同步即读取。

## 1. 准备 Demo 与 .env

在 AIDEV 仓库根目录执行：

```bash
cd src/bkai-init/demo
# 已有 .env 则保留，首次运行才复制
test -f .env || cp .env.example .env
```

编辑 `package/bkai.yaml` 及其引用文件。修改资源 code 时同步修改关联，规则见 [Package 与 Agent 协议](agent_package.md#资源-code-命名)。

在 `.env` 中填写实际连接信息，每项使用 `export`：

```bash
export BKAI_INIT_VERSION=0.1.6
export BKAI_APP_CODE='<应用编码>'
export BKAI_APP_SECRET='<应用密钥>'
export BK_API_URL_TMPL='https://gateway.example.com/api/{api_name}/{stage}'
export BKAI_SPACE_ID='<目标空间完整 ID>'

# 单租户默认 false；多租户环境改为 true 并填写应用实际租户
export BKPAAS_MULTI_TENANT_MODE=false
export BKAI_TENANT_ID=

# 显式填写实际 bk_username，不填写待转换的 login_name
export BKAI_USERNAME='<实际 bk_username>'

# Demo Skill 必填；仓库前缀不带末尾 /
export BKAI_AGENT_IMAGE_REPO=registry.example.com/team
```

真实 `.env` 不入库。镜像仓库前缀的原文冲突见 [接入入口](README.md#原文冲突镜像仓库前缀)。

专用网关无法套用模板时，另设 `BKAI_BASE_URL` 为平台提供的完整环境入口，包含 stage（例如 `/prod`）；其优先于模板。仅多租户且 `BKAI_USERNAME` 留空时查询默认 `bk_admin`，此时需要可用的 BK User 地址与虚拟用户查询权限。显式填写 `bk_admin` 也会原样透传。

## 2. 构建、校验与预览

在 Demo 目录执行：

```bash
source .env
make build
make validate
make plan
```

- `make build` 从 `.env` 读取 `BKAI_INIT_VERSION`。基础镜像默认 `bkai-init:local`；需要其它地址时，在 `.env` 中设置 `export BKAI_INIT_IMAGE=...`，`source` 后生效，也可通过 `make build` 的 `BKAI_INIT_IMAGE` 参数传入。
- 构建产物是本地 `bkai-init-demo:local`，不会发布 pip 包或镜像。
- 使用 Docker 时，每条 make 命令添加 `CONTAINER_TOOL=docker`。
- `make validate` 不自行加载 `.env`，因此先 `source`，随后读取已导出的 `BKAI_SPACE_ID`。
- `plan` 只预览，不保证后续写权限或发布成功。

Demo Skill 根目录 Dockerfile 使用：

```dockerfile
FROM {{ BKAI_AGENT_IMAGE_REPO | required }}/bkdbm-aidev-skills-env:0.0.1-alpha.16
```

仓库变量通过 make 的 `--var` 传入，最终完整镜像名必须在目标平台允许列表内。包版本 `0.1.6` 与 Skill 运行镜像版本相互独立。

## 3. 同步并追加管理员

确认预览后执行，以下操作会写入平台并提交发布：

```bash
source .env
# 将执行账号同时加为主、子应用管理员；参数名是 ADMINS
: "${BKAI_USERNAME:?请先填写实际 bk_username}"
make sync ADMINS="$BKAI_USERNAME"
```

不追加管理员时执行 `make sync`；多个管理员使用 `ADMINS="user_id_a user_id_b"`，值必须为实际 `bk_username`，调用方自行完成登录名转换。

`ADMINS` 应用于所有选中的主子 Agent；YAML 不支持 `spec.admins`。操作仅追加成员，不删除已有管理员，不改写 YAML。追加失败或结果未确认时保留异常原因，但继续当前 Agent 发布和后续资源同步，详见 [使用范围与管理员](agent_package.md#使用范围与管理员)。

`make sync` 默认带 `--confirm` 和 `--publish`：先同步子智能体并提交发布，再关联主智能体，不等待后台完成。仅同步草稿使用 `PUBLISH=false`；若首次初始化因子智能体没有可用发布版本而停止，应移除 `PUBLISH=false` 和限定子集的 `RESOURCES`，使用默认整包发布流程。

## 4. 判断结果

| 日志结果                      | 含义与下一步                                                                                |
| ----------------------------- | ------------------------------------------------------------------------------------------- |
| 管理员追加完成                | 应用管理员追加接口已成功，不代表后续发布身份校验必然通过。                                  |
| 已提交，后台处理中            | 知识导入或 Agent 发布已提交；到平台确认后台结果，CLI 不轮询。                               |
| 当前状态为 publishing         | 仅发布接口明确返回该原因的 HTTP 400 时记录并继续，不作为失败退出项。                        |
| Skill 已有版本正在发布        | 明确的版本构建中响应跳过本次更新并继续；其它上传或发布错误仍停止。                          |
| `bk_username is not verified` | 核对失败步骤、Trace ID 和下游实际传递的 `bk_username`；追加应用管理员不能替代上游身份校验。 |
| 基础镜像不在允许列表          | 核对渲染后的完整镜像名及目标租户有效白名单；修改 Dockerfile 后重新构建再同步。              |

已完成操作不自动回滚。限定重跑范围可使用 `RESOURCES="agent/ai-bkai-child"` 等清单内资源，未选中的依赖不会自动同步。知识库目录同步会移除线上多余文档，必须使用完整本地目录。

后续按 [部署与验收](deployment.md#验收清单) 确认后台任务和运行效果；独立调用见 [CLI 与变量替换](cli.md)，编写自有资源见 [Package 与 Agent 协议](agent_package.md) 及 [依赖资源协议](resources.md)。
