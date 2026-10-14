# 漏洞检查清单

每项：典型位置 → 安全写法 → 危险写法。鉴权细节见 `authorization.md`。

## missing-authorization：越权 / 缺失鉴权

见 `authorization.md`。重点：Web/OpenAPI 是否对资源实例显式调用 `*AuthService`；按 ID 取资源后是否校验归属 scope/租户；执行类是否鉴权主机与账号。

## sql-injection：SQL 注入

- **位置**：各服务 `dao/impl/*DAOImpl`，统一用 jOOQ（`DSLContext`）
- **安全**：用 jOOQ API / 绑定参数；排序字段用白名单映射到表字段（如 `buildOrderField(TABLE.NAME, ...)`）
- **危险**：字符串拼接 SQL；`order`/`sort`/`字段名` 直接拼进 SQL

## path-traversal：路径穿越

- **位置**：本地上传（`WebFileUploadResourceImpl`）、模板本地文件（`TemplateLocalFileService`）、导入导出（`job-backup`）、file-worker 下载路径
- **安全**：`PathUtil.resolveSafely(baseDir, relative...)`（拒绝 `..`、绝对路径、逃出 baseDir）；业务侧显式拒绝含 `..` 的路径
- **危险**：用户路径直接 `join`/`new File(base, userPath)` 且未校验；只用字符串替换而未 `normalize` + `startsWith(base)`

## ssrf：服务端请求伪造

- **位置**：作业回调 URL（`CallbackUrlValidateServiceImpl`）；文件源地址（`FileSourceValidateServiceImpl`）；其他主动外连
- **安全**：
  - 回调：仅 `http/https`；开启校验后禁 userinfo；命中配置/DB 白名单，或当前环境域名且非环回（`HttpUrlSafetyUtils`）
  - 文件源：环境制品库域名或白名单（`HttpUrlSafetyUtils`）
- **危险**：未校验即 `RestTemplate`/`HttpClient` 请求用户 URL；新增外连入口未复用上述校验；关闭校验开关却扩大可触发面

## sensitive-data-exposure：敏感信息泄露

- **敏感数据**：账号/DB 密码、文件源凭证、JWT/密钥、Token
- **安全**：入库用 `common-crypto`（如账号密码 `encryptor.encrypt` / `SymmetricCryptoService`）；传输用场景加密（`SubmitAccountPasswordCryptoService`）；日志用 `@SkipLogFields` + `JsonUtils` 过滤；接口用占位符（`SENSITIVE_FIELD_PLACEHOLDER`）回显
- **危险**：明文入库/入日志/入响应；新增敏感字段未加 `@SkipLogFields`；调试日志打印完整请求体

## hardcoded-secret：硬编码凭证

- **位置**：源码、测试、Helm values、示例配置
- **安全**：密钥走配置/密钥管理（如 `job.encrypt.password`、`job.security.*KeyBase64`）
- **危险**：提交真实密码、AK/SK、私钥；测试用生产凭证

## dangerous-script-bypass：高危脚本检测绕过

- **位置**：执行前 `DangerousScriptCheckService`（`TaskExecuteServiceImpl` 调用）；规则管理在 manage（高危规则，需 `NoResourceScopeAuthService`）
- **安全**：脚本执行入口统一走 `check` / `shouldIntercept`
- **危险**：新增执行入口跳过检测；削弱拦截逻辑或规则加载；未鉴权即可改高危规则

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
