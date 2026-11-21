# CLI 与变量替换

本页说明 `bkai-init 0.1.6` 的安装、运行参数、资源选择和 Python 调用。Demo 用户可直接使用 [Quick Start](quick_start.md)，无需在宿主机安装 Python 包。

## 安装固定版本

| 交付项                      | 约定                                                                                          |
| --------------------------- | --------------------------------------------------------------------------------------------- |
| Python 包                   | `bkai-init==0.1.6`，要求 Python 3.11+；包源由平台提供，不假设已发布到公共 PyPI。              |
| `BKAI_PIP_INDEX_URL`        | 提供 bkai-init 的包源 `/simple` 地址，仅用于安装示例，不是运行参数。                          |
| `BKAI_DEPENDENCY_INDEX_URL` | 提供 Jinja2、PyYAML、requests 等依赖的可访问包源，仅用于安装示例。                            |
| `BKAI_INIT_IMAGE`           | 平台提供的可拉取基础镜像完整地址；应使用 0.1.6 配套镜像，确认包含常规发布及 admins 透传能力。 |

```bash
export BKAI_PIP_INDEX_URL='<bkai-init 包源的 /simple 地址>'
export BKAI_DEPENDENCY_INDEX_URL='<公共依赖源的 /simple 地址>'

python3.11 -m venv .venv-bkai-init
. .venv-bkai-init/bin/activate

python -m pip install \
  --index-url "$BKAI_PIP_INDEX_URL" \
  --extra-index-url "$BKAI_DEPENDENCY_INDEX_URL" \
  "bkai-init==0.1.6"

python -m pip show bkai-init
bkai-init --help
```

如一个源包含全部依赖，可省略 `--extra-index-url`。两个源均参与候选解析，不保证优先级，只使用平台确认的可信源。仓库访问权限由平台说明，不在文档中填写令牌。

## 生成本地接入模板

以下目标目录必须不存在；命令仅生成本地文件，不访问平台、不需要应用凭据，不同步或发布：

```bash
bkai-init init ./bk-job --agent-code ai-job --name "作业助手"

export BKAI_SPACE_ID='<目标空间ID>'
bkai-init validate -f ./bk-job/bkai.yaml --space "$BKAI_SPACE_ID"
```

按需添加 `--with-skill job_skill`、`--with-knowledgebase bk_job_docs` 或 `--with-subagent ai-job-exec`，知识库文档由接入方维护。

## 连接信息与身份

命令行连接参数优先于对应环境变量。连接信息不写入资源 YAML。

| 配置                                                           | 语义与优先级                                                                                                                                                                           |
| -------------------------------------------------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `BK_API_URL_TMPL`                                              | 与直接地址二选一，例如 `https://gateway.example.com/api/{api_name}/{stage}`。`api_name` 分别为 `bk-aidev`、`bk-user`；没有 stage 时补 `prod`。CLI 自动解析，无需再配 `BKAI_BASE_URL`。 |
| `BKAI_BASE_URL` / `--base-url`                                 | AIDEV 网关完整环境入口，包含 stage；命令行优先，未提供才使用模板。专用网关域名不能按通用模板猜测。                                                                                     |
| `BKAI_APP_CODE` / `BKAI_APP_SECRET`                            | 必填应用编码和密钥；未配置时分别读取 `BKPAAS_APP_ID` / `BKPAAS_APP_SECRET`，命令参数优先。                                                                                             |
| `BKPAAS_MULTI_TENANT_MODE`                                     | 默认 `false`；多租户设为 `true`。Python 不自动读取，须显式传 `multi_tenant_mode=True`。                                                                                                |
| `BKAI_TENANT_ID` / `--tenant-id`                               | 仅开启多租户时使用，写入 `X-Bk-Tenant-Id`；未设默认 `system`，应填写应用实际租户，不自动推断。单租户忽略。                                                                             |
| `BK_USER_BASE_URL` / `--bk-user-base-url`                      | 仅多租户且未提供执行用户名时需要，可由模板推导。显式执行者和 admins 使用 `bk_username`，不做转换。                                                                                     |
| `BKAI_USERNAME` / `--username` / Python `username`             | 可选，显式值按 `bk_username` 原样透传。未传默认 `bk_admin`，仅多租户下查询转换；结果不唯一或查询失败时停止，调用应用须具备 BK User 虚拟用户 lookup 权限。                              |
| `BKAI_ACCESS_TOKEN` / `--access-token` / Python `access_token` | 可选，通常无需配置；环境变量兼容 `ACCESS_TOKEN`。不替代应用、空间及 MCP 授权。                                                                                                         |
| `--space` / `BKAI_SPACE_ID`                                    | 显式参数优先，空命令行值不接受。未传参数时读取环境变量，环境变量空值回退默认 `bkaidev`。不自动拼接租户前缀，多租户显式填完整 ID，如 `system-bkaidev`。                                 |

## 命令参数

| 参数                       | 约定                                                                                                                  |
| -------------------------- | --------------------------------------------------------------------------------------------------------------------- |
| `-f`                       | Package 清单路径；Demo 镜像内为 `/app/package/bkai.yaml`，工作目录 `/app` 下可用 `./package/bkai.yaml`。              |
| `--resource`               | 选择清单中资源，可重复，格式 `kind/code`；类型为 `agent`、`collection`、`skill`、`knowledgebase`，省略为全包。        |
| `--exclude-resource`       | 按资源排除，可重复，优先于选择列表；适用于 `plan` / `sync`，`show` 不支持黑名单。                                     |
| `--admins` / Make `ADMINS` | 空格分隔的 `bk_username`，应用于所有选中的主子 Agent；不传则不处理管理员。只追加，不删除，不支持 YAML `spec.admins`。 |
| `--confirm`                | 确认执行 `sync`；不传仅只读预览，不上传、不写入、不发布。                                                             |
| `--publish`                | 显式开启 Agent 发布，先子后主；不传只同步草稿。                                                                       |
| `--publish_config_only`    | `0` 或 `1`，默认 `0`，须同时指定 `--publish`。`0` 显式请求发布到开发者中心，`1` 仅发布配置。                          |
| `--var KEY=VALUE`          | 可重复传部署变量，规则见下文。                                                                                        |

资源选择只接受清单内对象，不自动同步未选中的依赖；MCP 是 Agent 关联，不是独立同步资源。即使只选部分资源，所有本地配置仍须通过校验。`plan` 就绪不代表后续有写权限或服务端执行必然成功。

## 执行校验、预览和同步

在已安装 bkai-init 的环境中执行。以下路径按 Demo 容器工作目录 `/app` 编写，独立调用时替换为实际清单路径：

```bash
# .env 中变量必须 export，source 后子进程才能读取
export BK_API_URL_TMPL='https://gateway.example.com/api/{api_name}/{stage}'
export BKAI_APP_CODE='<应用编码>'
export BKAI_APP_SECRET='<应用密钥>'
export BKPAAS_MULTI_TENANT_MODE=true
export BKAI_TENANT_ID='<应用所在租户>'
export BKAI_SPACE_ID='<目标空间ID>'

source .env
bkai-init validate -f ./package/bkai.yaml --space "$BKAI_SPACE_ID"
bkai-init show -f ./package/bkai.yaml --space "$BKAI_SPACE_ID"
bkai-init plan -f ./package/bkai.yaml --space "$BKAI_SPACE_ID" --publish
bkai-init sync -f ./package/bkai.yaml --space "$BKAI_SPACE_ID" \
  --confirm --publish --publish_config_only=0
```

`show` 只查看线上配置，不查询权限。`plan --format json` 或 `--format yaml` 可获取结构化计划；不带 `--confirm` 的 `sync` 同样只预览。Skill 文件、envs 值、角色标签及知识库文档等无法回读的内容，不能据此认定一致。

### 发布规则

- 常规发布显式传递 `is_publish_to_paas_v3=true`。平台可按代码版本一致策略跳过重新部署；工具不改变这一优化。
- 工具只调用发布接口，不轮询或等待部署。`pending` / `running` 标记“已提交，后台处理中”，`successful` 按接口结果记录发布完成；实际部署仍须到平台确认。
- 本轮子智能体发布已提交后，主智能体可直接关联，来源指令使用本轮已同步配置。包外或本轮未发布的来源仍需有可访问的发布版本。
- 发布请求失败不自动重试。仅明确返回 HTTP 400 且原因是“智能体状态异常，无法发布，当前状态为：publishing”时，记录原因并继续，不计为失败。
- Skill 已有版本正在发布的明确响应会跳过本次更新并继续；其它上传或发布错误仍停止。

### 更新时避免覆盖

1. 提供完整配置：同步 Agent 可能重建资源关联，省略字段不等于保留线上原值。
2. 按资源保护：平台手工维护的 Agent 应排除整个 Agent。仅排除 Skill，不能阻止 Agent 中的 Skill 关联或 envs 更新。
3. 区分 envs 输入：省略、`[]`、缺少 `value`、`value: ""` 语义不同；CLI 不将省略的 value 转为空字符串。`[]` 不代表清空全部变量，最终值须满足 Skill 的依赖声明。
4. 更新角色后同步引用它的 Agent；Agent 关联 Skill 使用当前 Skill 版本。资源版本号不代表 Agent 已发布。
5. 知识库有文档时，必须提供本地完整目录，会删除线上多余内容；不要用不完整目录更新。

## 变量替换

本节描述 bkai-init 自身的替换能力：支持受限的 Jinja 变量语法 `{{ KEY }}`、`{{ KEY | default("默认值") }}`、`{{ KEY | required }}`，不自动读取环境变量。

本仓库在调用工具前另有 `${JOB_URL_BASE}`、`${JOB_AI_GENERATE_TYPE}` 的名单式预渲染，见 [现有资源目录说明](../../../../support-files/bk-aidev/bk-job/README.md#占位符渲染)；这是项目脚本行为，不受本节 Jinja 文件白名单约束，也不能改为全量替换 `${...}`，以免破坏知识库中的作业魔法变量示例。

| 文件白名单                                                          | 处理方式                                       |
| ------------------------------------------------------------------- | ---------------------------------------------- |
| 入口 `bkai.yaml`、清单引用的 Agent / Collection YAML                | 解析后仅替换字符串值，不替换键名、结构或类型。 |
| 已声明 Skill 根目录 `skill.yaml`、知识库根目录 `knowledgebase.yaml` | 同上，不扫描所有 YAML。                        |
| 已声明 Skill 根目录已有 Dockerfile                                  | 仅替换文本，写入上传包，不修改本地源文件。     |

```bash
bkai-init sync -f /app/package/bkai.yaml --space "$BKAI_SPACE_ID" --confirm \
  --var BKAI_AGENT_IMAGE_REPO=registry.example.com/team \
  --var SKILL_NAME=job_skill
```

- 变量名为 1–64 位英文、数字或下划线，不能以数字开头；同一 key 重复传入报错，按第一个等号分隔 key/value。
- 仅支持简单变量、单个字符串参数的 `default`、不带参数的 `required`。不支持表达式、属性访问、函数调用、其它过滤器、循环或条件；结果不再次渲染。
- `default` 仅在未传变量时生效，显式空字符串保持为空。`required` 遇到未传、空字符串或纯空白时报错。
- YAML 独立占位符必须加引号，例如 `name: "{{ SKILL_NAME | default('作业查询 Skill') }}"`。
- 未传且无默认值的 YAML 占位符保留，供运行时组件引用；部署变量不要与组件 key 重名。
- Dockerfile 不允许遗留未替换占位符；替换值不允许换行或 NUL。变量来源须可信，不通过 CLI 变量传密钥。
- `SKILL.md`（含 frontmatter）、脚本、知识库文档、普通 YAML、嵌套 Dockerfile 均不处理。镜像开白与构建仍由平台校验。

## Python 调用

CLI 与 Python 使用同一个包、同一个虚拟环境，无需另装 SDK；从包顶层导入 `BkaiInit`。先加载 Quick Start 中的环境变量，再运行：

```python
import logging
import os
import sys

from bkai_init import BkaiInit

logging.basicConfig(level=logging.INFO, stream=sys.stdout)
# Python 示例使用含 {api_name}/{stage} 的完整模板
api_url_template = os.environ.get("BK_API_URL_TMPL", "")
initializer = BkaiInit(
    base_url=os.environ.get("BKAI_BASE_URL") or api_url_template.format(api_name="bk-aidev", stage="prod"),
    app_code=os.environ["BKAI_APP_CODE"],
    app_secret=os.environ["BKAI_APP_SECRET"],
    bk_user_base_url=os.environ.get("BK_USER_BASE_URL") or api_url_template.format(api_name="bk-user", stage="prod"),
    username=os.environ.get("BKAI_USERNAME"),
    access_token=os.environ.get("BKAI_ACCESS_TOKEN"),
    multi_tenant_mode=os.environ.get("BKPAAS_MULTI_TENANT_MODE", "false").lower() == "true",
    tenant_id=os.environ.get("BKAI_TENANT_ID", "system"),
    space=os.environ["BKAI_SPACE_ID"],
    variables={"BKAI_AGENT_IMAGE_REPO": os.environ.get("BKAI_AGENT_IMAGE_REPO", "")},
)
package = "/app/package/bkai.yaml"
initializer.validate(package)
online = initializer.show(package)
preview = initializer.plan(package, publish=True, publish_config_only=False)

# 写入操作：仅在调用方确认计划后执行
report = initializer.sync(package, publish=True, publish_config_only=False)
```

- 示例模板为 `https://gateway.example.com/api/{api_name}/{stage}`；直接提供 `BKAI_BASE_URL` / `BK_USER_BASE_URL` 时分别优先。
- CLI 自动解析模板；Python 须显式传地址及多租户开关。`username`、`access_token` 参数可省略，未传 username 时使用默认 `bk_admin`。
- Python `sync()` 直接写入，没有 `confirm` 参数；只读预览使用 `plan()`，发布规则与 CLI 相同。
- 类调用的 logging 由应用配置，CLI 默认输出到 stdout。
