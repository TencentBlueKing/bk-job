---
name: bk-job-security-guide
description: 蓝鲸作业平台（BK-JOB）PR 安全检查指导。在审计 PR 或代码变更是否引入安全漏洞时使用：含检查流程，以及 references 中的安全架构（流量入口与认证）、鉴权模型、漏洞检查清单与报告格式。
compatibility: 需要能读取 BK-JOB 源代码、执行 git 只读命令，以及调用 GitHub REST API。
metadata: {"version":"0.4.0","bk_skill_code":"bk-job-security-guide"}
---

# BK-JOB 安全检查指导

## 输入

| 输入 | 必填 | 说明 |
|------|------|------|
| 工作目录 | 是 | 预合并代码检出（只读：禁止修改文件，禁止执行项目代码或构建脚本） |
| PR 链接 | 是 | GitHub Pull Request URL，如 `https://github.com/TencentBlueKing/bk-job/pull/1234` |
| `headCommitId` | 否 | 预合并后的提交 ID（工作目录对应的 HEAD / 合并结果提交） |
| `lastCommitId` | 否 | 合并前的提交 ID（目标分支在合并前的 tip，用作 diff 起点） |

确定代码变更范围时的提交 ID 解析规则：

1. **优先使用用户输入**：若用户已同时提供 `headCommitId` 与 `lastCommitId`，**直接使用这两个值进入步骤 2**，无需为了获取 commitId 调用 GitHub API，也**禁止执行 `git fetch` / `git remote`**。PR 标题可从 `git log -1 --format=%s {headCommitId}` 获取。
2. **缺省再查 API**：仅当 `headCommitId` 或 `lastCommitId` 缺失时，才调用 GitHub REST API（`GET /repos/{owner}/{repo}/pulls/{number}`）补齐——`lastCommitId` ← `base.sha`，`headCommitId` ← `head.sha`。优先使用环境变量 `GITHUB_TOKEN`（若存在）；禁止在日志或报告中输出 Token。

## 核心概念

- **流量入口与认证**：Web / OpenAPI / 内部服务 / OP / 升级迁移 / 租户迁移等入口的认证方式不同，详见 `references/security-architecture.md`。
- **鉴权（IAM）**：认证之后还要对业务访问与资源实例做鉴权；内部服务、OP、升级迁移不做用户级 IAM。详见 `references/authorization.md`。
- **资源归属**：资源挂在 `AppResourceScope`（biz / biz_set / tenant_set）与租户下；越权的本质是访问了不属于自己业务/租户的资源。
- **产品功能不是漏洞**：在目标主机上执行脚本、分发文件是产品功能，审计重点是「谁可以对哪些主机做这件事」。

## 检查流程

### 1. 解析提交 ID 与版本检查

按上文「提交 ID 解析规则」确定 `lastCommitId`、`headCommitId`，并在工作目录中执行：

```bash
git rev-parse HEAD
git log -1 --format=%s {headCommitId}
```

**版本一致性要求（重要）**：若当前工作目录的 `HEAD` 与 `{headCommitId}` 不一致，后续读取任何项目源码文件时，**必须使用 `git show {headCommitId}:{path}`**，禁止直接读取工作区文件（否则会读到其他提交的代码，导致误判）。

### 2. 获取变更文件列表并按需查看 diff（禁止一次性全量 diff）

先只获取变更文件列表：

```bash
git diff --name-status {lastCommitId}...{headCommitId}
```

**降噪与按需读取原则（节省上下文）**：

1. **禁止直接执行不带路径的 `git diff {lastCommitId}...{headCommitId}`**（PR 常包含上千行文档、国际化与单测，全量 diff 会严重污染上下文）。
2. **直接跳过以下纯非风险文件**（无需查看 diff）：
   - 国际化资源：`**/i18n/**/*.properties`
   - 接口文档模板：`support-files/bk-api-gateway/**/apidocs/**`
   - 单元测试：`src/backend/**/src/test/**`（除非怀疑含真实硬编码凭证）
3. 只针对**核心生产代码**（如 `*Resource*.java`、`*Service*.java`、`*DAO*.java`、`*Interceptor*.java`、`*Filter*.java`、前端业务组件）与**敏感配置文件**，按文件查看 diff 或用 `git show {headCommitId}:{path}` 读取完整文件（优先读完整新文件以获得准确行号）。

命中以下敏感路径时记入报告 `sensitive_paths_changed`，提示人工 Review：

- `ai_agent/pr_check/`
- `.github/workflows/`、构建脚本
- `support-files/kubernetes/charts/bk-job/templates/job-gateway/configmap.yaml`（网关路由）
- `support-files/bk-api-gateway/v3/resources.yaml`（APIGW 接口暴露与认证配置）

### 3. 按风险面加载 references 并分析

> **读取方式**：本 Skill 的 `references/` 位于当前 Skill 目录下（若通过 `.gemini` 加载，路径为 `.gemini/skills/bk-job-security-guide/references/*.md`），该目录被 `.gitignore` 忽略且不在 git 中，**必须直接用文件读取工具按路径读取，禁止对其使用 `git show` 或文件搜索工具**。

| 变更命中的风险面 | 读取的文档 |
|----------------|----------|
| 任何变更（首次必读）；网关路由、过滤器、新增/改动接口入口、认证逻辑 | `references/security-architecture.md` |
| `*AuthService`、拦截器、IAM Action、按资源 ID 访问、执行目标/账号 | `references/authorization.md` |
| SQL、文件路径、外部 URL、凭证、日志、反序列化、前端渲染、依赖、高危脚本等 | `references/checklist.md` |

分析要点：

1. 对照 `security-architecture.md`「安全要求」：不得删除或架空既有认证；新增接口必须落在已知流量入口类型内，且能命中对应认证；网关传入的 username 可信，但仍须结合资源范围做鉴权，链路中不允许缺失。
2. 对照 `authorization.md`「常见假象」与「审计要点」：核对 Resource → Service 调用链中的显式 `*AuthService` 调用、IDOR、批量、执行主机/账号、无业务上下文、平台级操作、内部/OP/迁移边界。**相信 `authorization.md` 已给出的结论，禁止在代码库中全局搜索 `@AuditEntry` 或 `AuditEntryAspect` 的实现**。
3. 对照 `checklist.md` 中命中的漏洞类型，核对是否使用了项目既有防护、是否出现危险写法。
4. 对有风险的变更：定位入口 → 追踪数据流 → 核对防护。只读取与本次变更直接相关的上下游类（1～2 个既有同类实现作参照即可），禁止在全仓库漫无目的地遍历无关模块。

### 4. 自检

1. 证据是否充分、是否为本 PR 引入、攻击者是否可控该输入？证据不足则降低置信度，勿堆砌推测。
2. **去重检查**：同一条调用链（如 `Resource.createXxx` → `Service.createXxx`）缺失鉴权，**只合并报告为 1 条 Finding（定位于入口 Resource 方法）**，并在描述中说明“Resource 层与 Service 层均未调用鉴权”，不得拆成两条 Finding。
3. **行号与修复建议检查**：确认 `start_line` / `end_line` 与 `evidence` 中的行号来自 `{headCommitId}` 下的完整源码真实行号（而非 diff 片段的相对行号）；确认修复建议引用的是项目中真实存在的 `*AuthService` 类与方法（可对照 `authorization.md`「模块鉴权服务」表）。

### 5. 输出报告

按 `references/report-format.md`，将 `report.json` 与 `report.html` 写到工作目录下的 **`tmp/pr-security-check-report/`**（目录不存在则创建；`tmp/` 已被项目 `.gitignore` 忽略）。无发现也必须输出报告。

- `base_sha`：填解析后的 `lastCommitId`；`head_sha`：填解析后的 `headCommitId`
- `findings[].type`：使用 `checklist.md` 的章节名（如 `missing-authorization`、`sql-injection`）
- `report.html`：单文件内联样式；对来自 PR/代码的文本做 HTML 转义

## 审计规则（必读）

1. **不论判定“存在漏洞”还是“已有防护”，都必须有代码证据**：
   - 严禁根据 references 中的设计描述倒推代码“应该已经鉴权/校验”。
   - 凡是断定某接口已完成 IAM 鉴权，必须在代码调用链（Resource → Service）中找到具体的 `*AuthService.auth*(...)` 调用及结果检查，并在分析或 `notes` 中给出 `文件:行号`；找不到即为缺失鉴权。
   - **严禁将 `@AuditEntry` / `@ActionAuditRecord`（外部 `bk-audit` 操作审计注解）、`@EsbV4API`、`resources.yaml` 中的 `resourcePermissionRequired` 当作 IAM 鉴权**（详见 `authorization.md`「常见假象」）。
2. 判断「是否有鉴权」时，确认鉴权**作用于本次被访问的资源实例**，且按 ID 查询后校验了资源归属当前 `AppResourceScope` 与租户。
3. 判断「是否可控」时，确认数据来自外部请求，而不是来自已鉴权的内部数据；网关注入的 username 仍须做资源级鉴权。
4. references 描述的是目标分支设计；若 PR 修改了其中的机制本身（网关过滤器、鉴权拦截器、入口类型等），以「机制被修改」为重点，并在结论中明确体现。
5. 认证机制被削弱、删除或失效时，无论是否已有可利用链路，都必须在报告中体现（可结合 severity/confidence 分级）。

## 维护

本 Skill 的 `references/` 由维护者人工维护并入库，不在 CI 中现场生成。
