# 鉴权模型

BK-JOB 鉴权基于蓝鲸权限中心（BK-IAM）。认证完成后，对外用户接口还需做**业务访问鉴权**与**资源操作鉴权**；内部接口（`/service/**`）、OP 接口（`/op/**`）、升级迁移接口不做用户级 IAM 鉴权。

## 核心概念

- **资源范围 `AppResourceScope`**：业务（biz）/ 业务集（biz_set）/ 租户集（tenant_set），绝大多数资源挂在某个 scope 下。
- **租户 `tenantId`**：用户与业务必须同属一个租户；跨租户按资源不存在处理（`APP_NOT_EXIST`）。
- **Action + Resource**：向 IAM 校验「用户能否对某资源实例执行某操作」；Action 定义在 `ActionId`，资源类型定义在 `ResourceTypeEnum`。
- **鉴权结果 `AuthResult`**：`pass` / `fail`；失败时携带可申请的权限；业务侧常用 `denyIfNoPermission()` 直接拒绝。

## 鉴权分层

### 1. 业务访问鉴权（拦截器，自动）

- **拦截范围**：`/web/**`、`/esb/api/**`（`AuthAppInterceptor`）
- **前置条件**：`BasicAppInterceptor` 已从路径/请求体解析出 `BasicApp` 并写入 `JobContextUtil`
- **校验内容**：
  1. `app.tenantId == user.tenantId`
  2. `BusinessAuthService.authAccessBusiness`（Action: `access_business`）
- **注意**：解析不到 app（公共资源、无业务上下文接口）时跳过本层；**不等于已完成资源鉴权**。

### 2. 资源操作鉴权（业务代码，显式调用）

各模块通过 `*AuthService` 对具体资源实例调用 IAM，典型写法：

```java
scriptAuthService.authManageScript(user, appResourceScope, scriptId, null)
    .denyIfNoPermission();
```

批量操作应对**每一个**资源鉴权（`batchAuth*` / `batchAuthResult*`）。

### 3. 执行场景的组合鉴权

执行类操作除资源本身外，通常还要鉴权：

- **执行目标主机 / 拓扑 / 动态分组 / 容器**（`ResourceTypeEnum.HOST` 等）
- **执行账号**（`use_account`，`AccountAuthService` / `ExecuteAuthService.authAccountExecutable`）

入口：`ExecuteAuthService`（快速执行脚本、快速分发文件、执行业务/公共脚本、执行方案、模板调试、查看执行历史等）。

## 公共鉴权服务（`common-iam`）

| 服务 | 职责 |
|------|------|
| `AuthService` | 无业务范围或平台级资源的鉴权（公共脚本、全局设置等底层调用） |
| `AppAuthService` | 带 `AppResourceScope` 路径的鉴权与批量鉴权 |
| `BusinessAuthService` | 业务访问鉴权（`access_business`） |
| `WebAuthService` | Web 侧权限申请 URL 等辅助能力 |
| `AuthHelper`（IAM SDK） | 实际调用 IAM 判定是否允许 |

资源实例鉴权时通常构造带业务路径的 `PathInfoDTO`（业务 → 资源），保证权限策略可按业务拓扑生效。

## 模块鉴权服务（按资源）

| 模块 | 鉴权服务 | 主要 Action（见 `ActionId`） | 典型参照实现 |
|------|---------|------------------------------|-------------|
| job-manage | `ScriptAuthService` | `create/view/manage/execute_script` | `WebScriptResourceImpl`、`EsbScriptResourceV3Impl` |
| job-manage | `TemplateAuthService` | `create/view/edit/delete/debug_job_template` | `WebTaskTemplateResourceImpl` |
| job-manage | `PlanAuthService` | `create/view/edit/delete/launch/sync_job_plan` | `OpenApiJobPlanV4ResourceImpl` |
| job-manage | `AccountAuthService` | `create/manage/use_account` | `WebAccountResourceImpl` |
| job-manage | `TagAuthService` / `TicketAuthService` / `FileSourceAuthService` | tag / ticket / file_source 的 create/manage/view/use | `WebTagResourceImpl` |
| job-manage | `NoResourceScopeAuthService` | **与业务无关的平台级资源**：公共脚本（`authCreatePublicScript`/`authManagePublicScript`）、IP 白名单（`authCreateWhiteList`/`authManageWhiteList`）、全局设置（`authGlobalSetting`）、高危规则（`authHighRiskDetectRule`）、运营视图等 | `WebWhiteIPResourceImpl`、`WebPublicScriptResourceImpl`、`EsbDangerousRuleV3ResourceImpl` |
| job-crontab | `CronAuthService` | `create_cron` / `manage_cron` | `OpenApiSaveCronV4ResourceImpl` |
| job-execute | `ExecuteAuthService` | 快速执行/分发、执行脚本/方案、查看历史；并组合主机与账号鉴权 | `WebExecuteTaskResourceImpl` |
| job-file-gateway | `FileSourceAuthService` | 文件源 create/view/manage | `EsbFileSourceV3ResourceImpl` |

> **修复建议编写注意**：给出鉴权缺失的修复建议时，**必须引用上表中已存在的 `*AuthService` 接口与方法**（例如 IP 白名单必须建议注入 `NoResourceScopeAuthService` 并调用 `authCreateWhiteList` / `authManageWhiteList`，**严禁凭空捏造不存在的 `WhiteIpAuthService`**）。

资源类型枚举见 `ResourceTypeEnum`（`SCRIPT`、`TEMPLATE`、`PLAN`、`CRON`、`ACCOUNT`、`HOST`、`FILE_SOURCE`、`PUBLIC_SCRIPT` 等）。

## 哪些入口需要鉴权

| 入口 | 业务访问鉴权 | 资源操作鉴权 |
|------|-------------|-------------|
| Web / OpenAPI | 有业务上下文时由拦截器自动做 | **必须**在 Resource/Service 中显式调用 |
| 租户迁移（`@TenantMigrate`） | 同 OpenAPI | **必须**显式鉴权，且强度不低于对应常规接口 |
| 内部服务 `/service/**` | 无 | 无（信任服务间 JWT） |
| OP `/op/**` | 无 | 无（仅 HTTP Basic） |
| 升级迁移 `/service/.../migration/**` | 无 | 无 |

## ⚠️ 常见假象（不能视为已鉴权）

**本项目不存在基于注解或网关配置的自动 IAM 鉴权**。以下机制在代码中经常与 `ActionId` 同时出现，但**不做任何 IAM 权限校验**：

1. **操作审计注解**（`com.tencent.bk.audit.annotations.*`，来自外部依赖 `bk-audit-java-sdk`，**本仓库内无其源码，也无任何 `AuditEntryAspect` 鉴权切面，禁止在仓库中搜索其定义或切面**）：`@AuditEntry(actionId = ActionId.XXX)`、`@ActionAuditRecord`、`@AuditRequestBody`、`@AuditInstanceRecord` **仅用于记录操作审计日志，不会触发 IAM 鉴权**。若接口只有 `@AuditEntry` 而整条调用链（Resource → Service）没有任何 `*AuthService` / `AuthHelper` 调用，直接判定为**缺失鉴权**。
2. **接口标记注解**：`@WebAPI`、`@EsbAPI`、`@EsbV4API`、`@InternalAPI` 仅用于标记接口类型、指标与统一异常处理，不做鉴权。
3. **API 网关配置 `resources.yaml`**：`resourcePermissionRequired: true` 仅校验蓝鲸应用（`app_code`）是否申请了该 API 的网关调用权限，**不校验当前用户（`username`）在 IAM 中的业务或资源权限**。
4. **`AuthAppInterceptor` 的跳过分支**：
   - Web 接口路径不含 `/scope/{scopeType}/{scopeId}`，或 OpenAPI 请求顶层不含 `bk_scope_type`/`bk_scope_id`（或 `bk_biz_id`）时，`BasicAppInterceptor` 解析出的 `app` 为 `null`（典型：公共脚本、白名单等平台级接口，或参数名写成 `scope_list` 的批量接口）。
   - `app == null` 时 `AuthAppInterceptor` **直接放行**，连 `access_business` 都不会做。
5. **只调用 `auth*` 但未检查结果**：`*AuthService.auth*(...)` 返回 `AuthResult`（不会自动抛异常，除非是 `void authViewTaskInstance` 等少数方法）；若未调用 `.denyIfNoPermission()` 或 `if (!authResult.isPass()) throw new PermissionDeniedException(...)`，鉴权结果被丢弃，等同于未鉴权。

## 审计要点

1. **必须在代码调用链中找到显式鉴权调用**：确认 Resource 或其调用的 Service 方法中存在 `*AuthService.auth*(...)` / `batchAuth*(...)`，且结果被检查（`denyIfNoPermission()` 或判断 `isPass()`）。判定“已鉴权”时必须能指出该调用所在的 `文件:行号`。**注意：一条调用链只需在 Resource 或 Service 其中一层完成鉴权即可；若整条链路都未鉴权，只针对入口 Resource 方法报 1 条 Finding，不要把 Resource 缺鉴权和 Service 缺鉴权拆成两条重复报告**。
2. **新增 Web/OpenAPI 接口**：有业务上下文时拦截器只做 `access_business`；是否对操作的**具体资源实例**调用了对应 `*AuthService`？
3. **IDOR（越权访问他业资源）**：DAO 的 `get*ById(id)` 通常只按主键查询，不带 `appId`。按 ID 查出资源后，是否校验了资源归属当前请求的 `AppResourceScope`（及租户），而不是只对请求参数里声明的 scope 做了鉴权？
4. **批量接口**：是否对列表中每个资源做了鉴权/归属校验？若请求体用 `scope_list` 等列表结构（`AuthAppInterceptor` 不会拦截），是否在代码中对涉及的业务/资源逐一鉴权？
5. **执行类接口**：是否鉴权了执行目标（主机等）与执行账号，而不仅是脚本/方案本身？
6. **平台级操作（`app == null`）**：公共脚本、IP 白名单、全局设置、高危规则、运营视图等不会触发 `AuthAppInterceptor`，**必须**在代码中显式调用 `NoResourceScopeAuthService` / `AuthService`。
7. **内部/OP/升级迁移接口**：不做 IAM；若 PR 扩大其能力或经网关对外暴露，风险在信任边界，而非“补一个 Action”。
