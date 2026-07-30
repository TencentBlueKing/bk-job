# 执行账号查询与选择（执行类操作填写账号）

部分执行类操作需要指定**执行账号**（脚本以哪个系统账号在目标机器上运行）。最典型的是 `fast-execute-script`，它要求 `--account-alias` 或 `--account-id` 至少提供一个。当用户没有指定账号时，用只读子命令 `account-list` 列出该资源范围下可用账号，引导用户选择，避免凭空猜别名或 ID。字段级说明见 [`../apidocs/get_account_list.md`](../apidocs/get_account_list.md)。

> `account-list` 是**查询（只读）**，不改变任何状态，无需过写操作确认门禁；但选定账号后用于执行类写操作（如 `fast-execute-script`），仍须遵守 [confirmation-and-output-protocol.md](confirmation-and-output-protocol.md) 第 1 节的确认门禁。

## 1. 子命令

| 子命令 | 接口 | 说明 |
|--------|------|------|
| `account-list` | `GET /api/v3/get_account_list` | 查询资源范围下执行账号列表，可按用途/名称/别名过滤，分页返回 |

## 2. 何时使用

用户请求执行类操作（如 `fast-execute-script` 快速执行脚本、`fast-transfer-file` 文件分发的目标账号与服务器文件源账号）、需要执行账号，但**未指定 `account_alias` / `account_id`** 时：

- 用户说「用 root 跑这个脚本」但不确定账号是否存在 → 用 `account-list --account root` 或 `--alias root` 确认。
- 用户完全没提账号 → 先 `account-list` 列出可用账号（默认系统账号 `--category system`），以表格引导选择。
- 需要 DB 账号（如执行 SQL 相关操作）→ `account-list --category db`。

若尚未确定业务/业务集，先用 `list-authorized-scopes` 引导选定资源范围，见 [scope-selection-and-onboarding.md](scope-selection-and-onboarding.md)。

## 3. 参数要点

| 参数 | 说明 |
|------|------|
| `--bk-scope-type` / `--bk-scope-id` | 资源范围；`biz` 业务 / `biz_set` 业务集 |
| `--category` | 账号用途：名称 `system`/`db` 或编码 `1`（系统账号）、`2`（DB账号）；不传则不区分。**脚本执行一般用系统账号（1）** |
| `--account` | 账号名称过滤 |
| `--alias` | 账号别名过滤 |
| `--start` / `--length` | 分页；默认 `--length 20`（接口最大 1000），遵循「先一页」策略 |

**返回中关注字段**：`id`（用于 `fast-execute-script --account-id`）、`alias`（用于 `--account-alias`）、`account`（账号名）、`category`（1 系统 / 2 DB）、`type`（1 Linux、2 Windows、9 MySQL、10 Oracle、11 DB2）、`os`。

> 目标机器为 Linux 时通常选 `type=1` 的系统账号（如 root）；Windows 选 `type=2`。`fast-execute-script` 同时提供 `--account-alias` 与 `--account-id` 时以 `account_id` 为准。

## 4. 与执行命令的衔接

```
[A] 确认资源范围（缺失先 list-authorized-scopes）
        ↓
[B] account-list（默认 --category system）→ 表格展示可用账号
        ↓
[C] 用户选定账号 → 取 alias（--account-alias）或 id（--account-id）
        ↓
[D] fast-execute-script 组装账号参数 → dry-run → 确认摘要 → 用户独立确认（G2）→ 真实执行
```

## 5. 命令示例

```bash
# A. 列出业务下系统账号（脚本执行常用），先一页 20 条
python scripts/job_apigw_client.py account-list \
  --bk-scope-id <业务ID> --category system --length 20

# B. 按别名确认 root 是否存在
python scripts/job_apigw_client.py account-list \
  --bk-scope-id <业务ID> --alias root

# C. 列出 DB 账号
python scripts/job_apigw_client.py account-list \
  --bk-scope-id <业务ID> --category db
```

## 6. Token 效率与展示

- 账号列表遵循「先一页」策略（默认 `--length 20`），`total > length` 时说明「本页 N 条，共 M 条」再问翻页；大列表用 jq / `python -c` 本地过滤，勿把整页 JSON 贴进对话。见 [listing-and-token-efficient-analysis.md](listing-and-token-efficient-analysis.md)。
- 向用户展示用**表格**：别名（alias）/ 账号名（account）/ 用途（系统/DB）/ 类型（Linux/Windows...）/ 账号 ID。优先让用户按**别名**选择，更直观。

## 7. 相关手册与接口文档

- [fast-execute-script.md](fast-execute-script.md) — 快速执行脚本的执行账号参数（`--account-alias` / `--account-id`）
- [file-transfer.md](file-transfer.md) — 文件分发的目标账号与服务器文件源账号
- [host-query-and-selection.md](host-query-and-selection.md) — 查/搜业务下主机，填写执行目标
- [scope-selection-and-onboarding.md](scope-selection-and-onboarding.md) — 首次引导选择业务/业务集
- [confirmation-and-output-protocol.md](confirmation-and-output-protocol.md) — 执行类写操作确认门禁
- [`../apidocs/get_account_list.md`](../apidocs/get_account_list.md) — 账号列表字段级 API 文档
