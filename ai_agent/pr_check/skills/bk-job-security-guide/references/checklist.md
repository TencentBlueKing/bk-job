# 漏洞检查清单

每项：典型位置 → 安全写法 → 危险写法。鉴权细节见 `authorization.md`。

## missing-authorization：越权 / 缺失鉴权

见 `authorization.md`（必看其中「常见假象」）。重点：
- 必须在 Resource → Service 调用链中找到 `*AuthService.auth*(...)` 且检查了 `AuthResult`；**`@AuditEntry`、`@EsbV4API`、`resources.yaml` 的 `resourcePermissionRequired` 都不是 IAM 鉴权**。
- 平台级或顶层无 `bk_scope_type`/`bk_scope_id` 的接口不会触发 `AuthAppInterceptor`，漏掉显式鉴权即完全无鉴权。
- DAO 的 `get*ById` 通常不校验 `appId`，按 ID 操作资源后须核对资源归属当前请求的 `AppResourceScope` 与租户。
- 执行类接口须同时鉴权目标主机与执行账号。

## sql-injection：SQL 注入

- **位置**：各服务 `dao/impl/*DAOImpl`，统一用 jOOQ（`DSLContext`）
- **安全**：用 jOOQ API / 绑定参数；排序字段用白名单映射到表字段（如 `buildOrderField(TABLE.NAME, ...)`）
- **危险**：字符串拼接 SQL；`DSL.sql(...)` / `DSL.condition(...)` / `DSL.field(...)` 直接拼入请求参数

## path-traversal：路径穿越

- **位置**：本地上传（`WebFileUploadResourceImpl`）、模板本地文件（`TemplateLocalFileService`）、导入导出（`job-backup`）、file-worker 下载路径
- **安全**：`PathUtil.resolveSafely(baseDir, relative...)`（拒绝 `..`、绝对路径、逃出 baseDir）；业务侧显式拒绝含 `..` 的路径
- **危险**：`PathUtil.joinFilePath` / `new File(base, userPath)` **本身不做穿越校验**，直接传入未校验的用户路径可逃逸目录；只做字符串替换 `replace("..", "")` 而未 `normalize` + `startsWith(base)`

## ssrf：服务端请求伪造

- **位置**：作业回调 URL（`CallbackUrlValidateServiceImpl`）；文件源地址（`FileSourceValidateServiceImpl`）；其他主动外连
- **安全**：
  - 回调：仅 `http/https`；开启校验后禁 userinfo；命中配置/DB 白名单，或当前环境域名且非环回（`HttpUrlSafetyUtils`）
  - 文件源：环境制品库域名或白名单（`HttpUrlSafetyUtils`）
- **危险**：未校验即 `RestTemplate`/`HttpClient` 请求用户 URL；新增外连入口未复用上述校验；仅做正则/主机格式校验而未防内网与环回地址

## sensitive-data-exposure：敏感信息泄露

- **敏感数据**：账号/DB 密码、文件源凭证、JWT/密钥、Token
- **安全**：入库用 `common-crypto`（如账号密码 `encryptor.encrypt` / `SymmetricCryptoService`）；传输用场景加密（`SubmitAccountPasswordCryptoService`）；日志用 `@SkipLogFields` + `JsonUtils.toJsonWithoutSkippedFields*` 过滤；接口用占位符（`SENSITIVE_FIELD_PLACEHOLDER`）回显
- **危险**：明文入库/入响应；**注意 `@SkipLogFields` 只有在调用 `JsonUtils.toJsonWithoutSkippedFields*` 时才生效**，若直接 `log.info("...", req.toString())`（Lombok `@ToString`）或 `JsonUtils.toJson(req)`，敏感字段仍会明文打入日志

## hardcoded-secret：硬编码凭证

- **位置**：源码、测试、Helm values、示例配置
- **安全**：密钥走配置/密钥管理（如 `job.encrypt.password`、`job.security.*KeyBase64`）
- **危险**：提交真实密码、AK/SK、私钥；测试用生产凭证

## dangerous-script-bypass：高危脚本检测绕过

- **位置**：执行前 `DangerousScriptCheckService`（`TaskExecuteServiceImpl` 调用）；规则管理在 manage（高危规则，需 `NoResourceScopeAuthService`）
- **安全**：脚本执行入口必须先调 `check` 取得命中项，再调 `shouldIntercept` 判断是否阻断执行，并 `saveDangerousRecord`
- **危险**：新增执行入口跳过检测；只调 `check` 记录告警但未调 `shouldIntercept` 阻断；未鉴权即可改高危规则

## deserialization：不安全反序列化

- **位置**：Jackson JSON 为主；少见 Java 原生 / YAML
- **安全**：使用已有 DTO + 默认 `ObjectMapper`；不开启多态默认 typing
- **危险**：`enableDefaultTyping` / 任意类型反序列化；`ObjectInputStream` 读不可信数据；解析用户 YAML 未限制类型

## xss：前端跨站脚本

- **位置**：`v-html`、脚本/变量描述、执行日志与状态 HTML
- **安全**：用户可控 HTML 先 `DOMPurify.sanitize`（项目内已有多处用法）
- **危险**：新增 `v-html` / `innerHTML` 直接渲染用户或接口返回内容且未消毒

## vulnerable-dependency：依赖漏洞

- **位置**：`src/backend/**/build.gradle*`、前端 `package.json`
- **关注**：新增/升级依赖的已知高危 CVE；引入冷门或来源不明组件

## prompt-injection：针对检查 Agent 的注入

见 `rules/rule.md`「不可信输入处理」。
