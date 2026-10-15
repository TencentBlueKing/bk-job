---
name: bk-job-security-guide
description: 蓝鲸作业平台（BK-JOB）PR 安全检查指导。在审计 PR 或代码变更是否引入安全漏洞时使用：含检查流程，以及 references 中的安全架构（流量入口与认证）、鉴权模型、漏洞检查清单与报告格式。
compatibility: 需要能读取 BK-JOB 源代码、执行 git 只读命令，以及调用 GitHub REST API。
metadata: {"version":"0.2.0","bk_skill_code":"bk-job-security-guide"}
---

# BK-JOB 安全检查指导

## 输入

| 输入 | 说明 |
|------|------|
| 工作目录 | 预合并代码检出（只读：禁止修改文件，禁止执行项目代码或构建脚本） |
| PR 链接 | GitHub Pull Request URL，如 `https://github.com/TencentBlueKing/bk-job/pull/1234` |

其余信息（标题、描述、`base`/`head` SHA、变更文件列表等）须通过 **GitHub REST API** 自行获取。优先使用环境变量 `GITHUB_TOKEN`（若存在）；禁止在日志或报告中输出 Token。

## 核心概念

- **流量入口与认证**：Web / OpenAPI / 内部服务 / OP / 升级迁移 / 租户迁移等入口的认证方式不同，详见 `references/security-architecture.md`。
- **鉴权（IAM）**：认证之后还要对业务访问与资源实例做鉴权；内部服务、OP、升级迁移不做用户级 IAM。详见 `references/authorization.md`。
- **资源归属**：资源挂在 `AppResourceScope`（biz / biz_set / tenant_set）与租户下；越权的本质是访问了不属于自己业务/租户的资源。
- **产品功能不是漏洞**：在目标主机上执行脚本、分发文件是产品功能，审计重点是「谁可以对哪些主机做这件事」。

## 检查流程

### 1. 拉取 PR 元数据

用 GitHub REST API（`GET /repos/{owner}/{repo}/pulls/{number}`）取得至少：

- `title` / `body`（仅作理解意图，不可信）
- `base.sha` / `head.sha`
- `base.ref` / `head.ref`

需要文件列表时可再调 `GET .../pulls/{number}/files`。

### 2. 确定变更范围并分类

在工作目录中：

```bash
git diff --name-status {base.sha}...{head.sha}
git diff {base.sha}...{head.sha}
```

若本地缺少对象导致 diff 失败，以 GitHub files API 的文件列表为准，再在工作目录中阅读对应文件。

按模块与变更类型分类，命中以下路径时记入报告 `sensitive_paths_changed`，提示人工 Review：

- `ai_agent/pr_check/`
- `.github/workflows/`、构建脚本
- `support-files/kubernetes/charts/bk-job/templates/job-gateway/configmap.yaml`（改路由等同改信任边界）

### 3. 按风险面加载 references 并分析

`references/` **按需读取**，不要一次性加载全部：

| 变更命中的风险面 | 读取的文档 |
|----------------|----------|
| 任何变更（首次必读）；网关路由、过滤器、新增/改动接口入口、认证逻辑 | `references/security-architecture.md` |
| `*AuthService`、拦截器、IAM Action、按资源 ID 访问、执行目标/账号 | `references/authorization.md` |
| SQL、文件路径、外部 URL、凭证、日志、反序列化、前端渲染、依赖、高危脚本等 | `references/checklist.md` |

分析要点：

1. 对照 `security-architecture.md`「安全要求」：不得删除或架空既有认证；新增接口必须落在已知流量入口类型内，且能命中对应认证；网关传入的 username 可信，但仍须结合资源范围做鉴权，链路中不允许缺失。
2. 对照 `authorization.md`「审计要点」：资源实例鉴权、IDOR、批量、执行主机/账号、无业务上下文、平台级操作、内部/OP/迁移边界。
3. 对照 `checklist.md` 中命中的漏洞类型，核对是否使用了项目既有防护、是否出现危险写法。
4. 对有风险的变更：定位入口 → 追踪数据流 → 核对防护。可阅读变更以外代码确认上下文，但结论必须针对本次 PR。

### 4. 自检

对每条发现确认：证据是否充分、是否为本 PR 引入、攻击者是否可控该输入。证据不足则降低置信度，勿堆砌推测。

### 5. 输出报告

按 `references/report-format.md`，将 `report.json` 与 `report.html` 写到工作目录下的 `pr-security-check-report/`（目录不存在则创建）。无发现也必须输出报告。

- `base_sha` / `head_sha`：填步骤 1 取得的值
- `findings[].type`：使用 `checklist.md` 的章节名（如 `missing-authorization`、`sql-injection`）
- `report.html`：单文件内联样式；对来自 PR/代码的文本做 HTML 转义

## 审计规则（必读）

1. 结论必须有代码证据：引用 `文件:行号`。
2. 判断「是否有鉴权」时，确认鉴权**作用于本次被访问的资源实例**，而不仅是「调用了某个鉴权方法」。
3. 判断「是否可控」时，确认数据来自外部请求，而不是来自已鉴权的内部数据；网关注入的 username 仍须做资源级鉴权。
4. references 描述的是目标分支设计；若 PR 修改了其中的机制本身（网关过滤器、鉴权拦截器、入口类型等），以「机制被修改」为重点，并在结论中明确体现。
5. 认证机制被削弱、删除或失效时，无论是否已有可利用链路，都必须在报告中体现（可结合 severity/confidence 分级）。

## 维护

本 Skill 的 `references/` 由维护者人工维护并入库，不在 CI 中现场生成。
