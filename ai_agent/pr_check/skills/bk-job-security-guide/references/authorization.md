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

| 模块 | 鉴权服务 | 主要 Action（见 `ActionId`） |
|------|---------|------------------------------|
| job-manage | `ScriptAuthService` | `create/view/manage/execute_script` |
| job-manage | `TemplateAuthService` | `create/view/edit/delete/debug_job_template` |
| job-manage | `PlanAuthService` | `create/view/edit/delete/launch/sync_job_plan` |
| job-manage | `AccountAuthService` | `create/manage/use_account` |
| job-manage | `TagAuthService` / `TicketAuthService` / `FileSourceAuthService` | tag / ticket / file_source 的 create/manage/view/use |
| job-manage | `NoResourceScopeAuthService` | 与业务无关：公共脚本、IP 白名单、全局设置、高危规则、运营视图等 |
| job-crontab | `CronAuthService` | `create_cron` / `manage_cron` |
| job-execute | `ExecuteAuthService` | 快速执行/分发、执行脚本/方案、查看历史；并组合主机与账号鉴权 |
| job-file-gateway | `FileSourceAuthService` | 文件源 create/view/manage |

资源类型枚举见 `ResourceTypeEnum`（`SCRIPT`、`TEMPLATE`、`PLAN`、`CRON`、`ACCOUNT`、`HOST`、`FILE_SOURCE`、`PUBLIC_SCRIPT` 等）。

## 哪些入口需要鉴权

| 入口 | 业务访问鉴权 | 资源操作鉴权 |
|------|-------------|-------------|
| Web / OpenAPI | 有业务上下文时由拦截器自动做 | **必须**在 Resource/Service 中显式调用 |
| 租户迁移（`@TenantMigrate`） | 同 OpenAPI | **必须**显式鉴权，且强度不低于对应常规接口 |
| 内部服务 `/service/**` | 无 | 无（信任服务间 JWT） |
| OP `/op/**` | 无 | 无（仅 HTTP Basic） |
| 升级迁移 `/service/.../migration/**` | 无 | 无 |

## 审计要点

1. **新增 Web/OpenAPI 接口**：有业务上下文时拦截器会做 `access_business`；是否对操作的**具体资源实例**调用了对应 `*AuthService`？
2. **IDOR**：按 ID 查出资源后，是否校验资源属于请求中的 `AppResourceScope`（及租户），而不是只鉴权请求里声明的 scope？
3. **批量接口**：是否对列表中每个资源做了鉴权/归属校验？
4. **执行类接口**：是否鉴权了执行目标（主机等）与执行账号，而不仅是脚本/方案本身？
5. **无业务上下文接口**：是否误用了会跳过 `AuthAppInterceptor` 的路径，却又操作了业务资源？
6. **平台级操作**：公共脚本、白名单、全局设置、高危规则等是否走了 `NoResourceScopeAuthService` / `AuthService`，而不是误用业务资源鉴权或漏鉴权？
7. **内部/OP/升级迁移接口**：不做 IAM；若 PR 扩大其能力或经网关对外暴露，风险在信任边界，而非“补一个 Action”。
