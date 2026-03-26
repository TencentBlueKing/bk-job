# 环境与鉴权

## 1. 脚本

[`scripts/job_apigw_client.py`](../../scripts/job_apigw_client.py)（相对技能根目录）。

## 2. 环境变量

| 变量 | 含义 |
|------|------|
| `BK_JOB_ACCESS_TOKEN` | 蓝鲸用户态 **access_token**，放入请求头 `X-Bkapi-Authorization: {"access_token":"<token>"}` |
| `BK_JOB_APIGW_BASE_URL` | 作业平台在 **API 网关**上的根地址，须能直接拼接路径 `/api/v3/...`、`/api/v4/...`（含 `https://`、环境/阶段前缀，如 `.../prod/bk-job`，以实际网关发布为准） |

命令行可覆盖：`--access-token`、`--base-url`。

## 3. 资源范围

绝大多数子命令需要：

- `--bk-scope-type`：`biz`（业务）或 `biz_set`（业务集），默认 `biz`
- `--bk-scope-id`：与类型对应的 ID（字符串）

无权限时接口返回 403，需在权限中心申请作业平台/业务相关权限。

## 4. 注意事项

- **跨平台中文**：客户端脚本会将标准输出/错误设为 **UTF-8**（Windows 常见 GBK 控制台可减轻乱码）。若 IDE/管道仍异常，可尝试终端 UTF-8、`python -X utf8`。
- **不要把令牌写进 SKILL、手册或仓库**：用环境变量或本地私密配置。
- 网关根 URL 若配置错误，会出现 404、HTML 错误页或路由到错误服务；应与 API 网关控制台中「作业平台」组件对外路径一致。
- `access_token` 通常有有效期，过期后表现为 401，需按蓝鲸流程重新申请。
