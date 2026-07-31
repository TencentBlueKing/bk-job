# 首次引导选择业务/业务集

绝大多数子命令都需要 `--bk-scope-type`（`biz`/`biz_set`）与 `--bk-scope-id`。当用户**初次使用**、既没有在对话中给出资源范围、`memory/businesses/` 也没有可用记忆时，应先列出用户有权限的资源范围并引导其选择，选定后再执行后续操作。字段级说明见 [`../apidocs/get_user_authorized_scopes.md`](../apidocs/get_user_authorized_scopes.md)。

## 1. 子命令

| 子命令 | 接口 | 说明 |
|--------|------|------|
| `list-authorized-scopes` | `GET /api/v4/get_user_authorized_scopes` | 分页查询当前用户有权限的业务/业务集 |

| 参数 | 说明 |
|------|------|
| `--offset` | 分页起始偏移，从 0 开始，默认 0 |
| `--length` | 单页返回条数，默认 20，取值范围 1~200 |

## 2. 何时触发首次引导

同时满足以下条件时，先引导选择资源范围，**不要**擅自猜测 `bk_scope_id`：

1. 用户请求需要资源范围（如快速执行脚本、搜索执行方案/定时任务等）；
2. 当前对话上下文中没有明确的 `bk_scope_type` / `bk_scope_id`；
3. `memory/businesses/` 下没有可复用的业务记忆（见 [business-memory.md](business-memory.md)）。

## 3. 引导流程

1. 调用 `list-authorized-scopes` 拉取一页（默认 20 条）。
2. 以**表格**向用户展示：名称、类型（业务/业务集）、`bk_scope_id`、是否收藏（`favor`）。`total > length` 时说明「本页 N 条，共 M 条」并询问是否翻页。
3. **等用户选定**某个资源范围后，再带上对应的 `--bk-scope-type` / `--bk-scope-id` 执行后续子命令。
4. 选定后可结合业务记忆沉淀：将常用资源范围写入 `memory/businesses/`，**写入前须经用户确认**（见 [business-memory.md](business-memory.md)）。

> 已有明确资源范围或业务记忆时，无需每次都调用本接口；仅在上下文缺失、需要用户从多个授权范围中选择时使用。

## 4. 命令示例

```bash
# 列出前 20 个有权限的业务/业务集
python scripts/job_apigw_client.py list-authorized-scopes --length 20

# 翻页
python scripts/job_apigw_client.py list-authorized-scopes --offset 20 --length 20
```

## 5. 相关手册与接口文档

- [environment-and-auth.md](environment-and-auth.md) — 资源范围 `bk_scope` 与鉴权
- [business-memory.md](business-memory.md) — 业务记忆的加载、预填与沉淀（写入须确认）
- [`../apidocs/get_user_authorized_scopes.md`](../apidocs/get_user_authorized_scopes.md) — 字段级 API 文档
