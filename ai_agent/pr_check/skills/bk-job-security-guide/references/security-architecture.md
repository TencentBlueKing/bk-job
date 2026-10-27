# 安全架构

## 服务与职责

| 服务 | 职责 | 对外暴露 |
|------|------|--------|
| `job-gateway` | 统一网关：Web 登录态校验、OpenAPI（ESB/APIGW）JWT 校验、路由转发 | 是 |
| `job-manage` | 脚本、作业模板、执行方案、账号、主机等资源管理 | 经网关 |
| `job-execute` | 作业执行引擎，与 GSE 交互下发任务 | 经网关 |
| `job-crontab` | 定时任务 | 经网关 |
| `job-file-gateway` / `job-file-worker` | 第三方文件源（如对象存储）文件分发 | 经网关 |
| `job-logsvr` | 执行日志存储 | 内部 |
| `job-analysis` | 运营统计分析 | 经网关 |
| `job-backup` | 作业导入导出、归档 | 经网关 |

## 流量入口类型与认证方式

网关路由定义在 `support-files/kubernetes/charts/bk-job/templates/job-gateway/configmap.yaml`，**修改该文件等同于修改信任边界**。

### Web 接口

- **路径**：外部 `/{service}/web/**`
- **调用方**：浏览器用户
- **认证方式**：网关 `Authorize`（登录态）+ `CsrfCheck`；服务端 `AuthAppInterceptor` 做业务级鉴权
- **接口特征**：`api/web/` 包下的 `Web*Resource`，`@RequestMapping("/web/...")`，业务资源路径通常带 `/scope/{scopeType}/{scopeId}`
- **主要实现类**：
  - `AuthorizeGatewayFilterFactory`：网关登录态校验
  - `CsrfCheckGatewayFilterFactory`：网关 CSRF 校验
  - `AddJwtHeaderGlobalFilter`：网关转发时注入服务间 JWT
  - `ServiceSecurityInterceptor`：服务端校验服务间 JWT（`x-job-auth-token`）
  - `AuthAppInterceptor`：业务级鉴权

### OpenAPI

- **路径**：外部 `/api/job/v{2,3,4}/...`，网关改写为 `/esb/api/...`；对外暴露的接口列表与认证策略定义在 `support-files/bk-api-gateway/v3/resources.yaml`
- **调用方**：蓝鲸 APIGW / ESB
- **认证方式**：
  - APIGW 侧（`resources.yaml`）：`authConfig` 控制是否校验用户登录态（`userVerifiedRequired`）与蓝鲸应用身份（`appVerifiedRequired`）；`bk-header-rewrite` 插件写入 `X-Bk-Job-Api-Type`（`user` 用户态：用户身份必须来自 APIGW JWT，不可被请求头覆盖；`app` 应用态：`CheckOpenApiJwtGatewayFilterFactory` 允许调用方通过请求头 `X-Bk-Username` 指定操作用户）
  - job-gateway 侧：`CheckTenant` + `CheckOpenApiJwt`（校验 APIGW JWT，提取 `app_code` 与 `username` 写入请求头）
  - 服务端：有业务上下文时由 `AuthAppInterceptor` 做业务级鉴权
- **接口特征**：`api/esb/` 包下的 `Esb*Resource`、`OpenApi*V4Resource`，`@RequestMapping("/esb/api/v{n}")`，标注 `@EsbAPI` / `@EsbV4API`，从请求头 `JobCommonHeaders.USERNAME` / `APP_CODE` 取调用者
- **主要实现类**：
  - `CheckTenantGatewayFilterFactory`：网关租户校验
  - `CheckOpenApiJwtGatewayFilterFactory`：网关 APIGW JWT 校验与用户身份提取
  - `AddJwtHeaderGlobalFilter`：网关转发时注入服务间 JWT
  - `ServiceSecurityInterceptor`：服务端校验服务间 JWT（`x-job-auth-token`）
  - `AuthAppInterceptor`：业务级鉴权

### 内部服务接口

- **路径**：`/service/**`
- **调用方**：微服务间调用
- **认证方式**：服务间 JWT：请求头 `x-job-auth-token`，由 `ServiceSecurityInterceptor` 校验，Feign 调用由 `FeignBasicAuthRequestInterceptor` 自动携带；无用户级 IAM 鉴权
- **接口特征**：`api/inner/` 包下的 `Service*Resource`，`@RequestMapping("/service/...")`，返回 `InternalResponse`
- **主要实现类**：
  - `FeignBasicAuthRequestInterceptor`：Feign 调用方注入服务间 JWT
  - `ServiceSecurityInterceptor`：服务端校验服务间 JWT
  - `AutoUpdateJwtManager` / `BasicJwtManager`：签发与校验 JWT

### OP 接口

- **路径**：`/op/**`
- **调用方**：运维人员（集群内访问，如 `kubectl port-forward`）
- **认证方式**：Spring Security HTTP Basic（`OpSecurityConfig`），账号与 actuator 共用 `job.security.actuator.user`；不校验服务间 JWT，不经过 `AuthAppInterceptor` 与 IAM 鉴权
- **接口特征**：`api/op/` 包下的 `*OpResource`，`@RequestMapping("/op/...")`，多为运维开关、白名单、数据同步等管理能力
- **主要实现类**：
  - `OpSecurityConfig`：`/op/**` HTTP Basic 安全配置

### 升级迁移接口

- **路径**：`/service/{module}/migration/**`
- **调用方**：`upgrader` 升级工具
- **认证方式**：服务间 JWT：`upgrader` 用服务私钥自行签发 `x-job-auth-token`；无用户上下文与 IAM 鉴权
- **接口特征**：`api/migration/impl/MigrationResource`，路径形如 `/service/manage/migration/action/{action}`，按全量数据批量处理（如加密账号密码、补充 hostId）
- **主要实现类**：
  - `BaseUpgradeTask`：升级工具签发 JWT 并调用迁移接口
  - `MigrationResource`（各服务 `api/migration/impl/`）：迁移动作入口
  - `ServiceSecurityInterceptor`：服务端校验服务间 JWT

### 租户迁移接口

- **路径**：挂在 `/esb/api/v3` 下（如 `update_cron_status_for_migration`）
- **调用方**：迁移工具，经 APIGW 调用
- **认证方式**：同 OpenAPI，且实现中需显式做 IAM 鉴权
- **接口特征**：类上标注 `@TenantMigrate`，接口名带 `_for_migration`；会绕过常规业务逻辑（如不更新最后修改人/时间），迁移完成后计划删除
- **主要实现类**：
  - `CheckOpenApiJwtGatewayFilterFactory`：网关 APIGW JWT 校验（同 OpenAPI）
  - `EsbCronJobMigrationResource` / `EsbCronJobMigrationResourceImpl`：定时任务租户迁移示例
  - `CronJobMigrationService` / `CronJobMigrationServiceImpl`：迁移专用业务逻辑（标注 `@TenantMigrate`）


## 安全要求
1. 不允许删除上述各类接口的认证逻辑或使其失效，如果有认证机制改动必须在审查结论中进行体现；
2. 新增接口必须限制在上述流量入口类型之内，不允许新增其他超出限制且不命中认证逻辑的裸接口；
3. 接口参数中由网关自动传入的 username 参数视为可信数据，但是在执行具体业务逻辑时必须结合资源范围等信息对其进行鉴权，鉴权逻辑可以下沉到 Service 类中，但是整个逻辑链路中必须要有，不允许越权访问无权限的资源；
4. `support-files/bk-api-gateway/v3/resources.yaml` 中新增或修改 OpenAPI 时：常规接口必须保持 `userVerifiedRequired: true` 与 `appVerifiedRequired: true`；若将用户态接口误配为 `X-Bk-Job-Api-Type: app`，调用方可通过 `X-Bk-Username` 伪造任意用户身份。
