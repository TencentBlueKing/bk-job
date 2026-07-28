# 主机查询与选择（执行类操作填写主机）

执行类操作（`fast-execute-script` 快速执行脚本、`plan-execute` 启动执行方案中的主机变量、`plan-create` / `cron-save` 的执行目标变量）都需要具体的**目标主机**。本手册说明如何用两个**只读**子命令帮用户查出主机，再把结果喂给执行命令，避免让用户手工记 `bk_host_id` 或 `bk_cloud_id`。字段级说明见 [`../apidocs/v4_get_biz_host_topo_tree.md`](../apidocs/v4_get_biz_host_topo_tree.md) 与 [`../apidocs/v4_search_scope_host.md`](../apidocs/v4_search_scope_host.md)。

> 这两个子命令都是**查询（只读）**，不改变任何状态，无需过写操作确认门禁。但它们查出的主机**一旦用于执行类写操作**，仍须遵守 [confirmation-and-output-protocol.md](confirmation-and-output-protocol.md) 第 1 节的确认门禁。

## 1. 子命令

| 子命令 | 接口 | 说明 |
|--------|------|------|
| `host-topo-tree` | `POST /api/v4/get_biz_host_topo_tree` | 查询业务主机拓扑树（**仅业务 biz**，默认全展开），返回 业务→集群(set)→模块(module) 层级与各节点主机数量 |
| `host-search` | `POST /api/v4/search_scope_host` | 按 IP/主机名/操作系统/Agent 状态/拓扑节点搜索主机，返回 `bk_host_id`、`ip`、`bk_cloud_id` 等；支持 biz / biz_set / tenant_set |

## 2. 何时使用

用户在执行类操作中需要指定主机，且**没有直接给出可用的 `bk_host_id` 或 `bk_cloud_id:ip`** 时：

- 用户用**自然语言**描述目标（如「重启 mysql 模块的机器」「给 10.0.0.x 网段的 centos 机器跑脚本」）→ 先查主机再执行。
- 用户只给了 **IP / 主机名 / 网段 / 操作系统**等模糊条件 → 用 `host-search` 关键字过滤定位。
- 用户按**拓扑（集群/模块）**圈定范围 → 先 `host-topo-tree` 看结构，让用户选节点，再 `host-search --topo-nodes` 精确取主机。

若尚未确定业务/业务集，先用 `list-authorized-scopes` 引导选定资源范围，见 [scope-selection-and-onboarding.md](scope-selection-and-onboarding.md)。

## 3. 推荐流程

```
[A] 确认资源范围（bk_scope_type / bk_scope_id）
        └─ 缺失时先 list-authorized-scopes 引导选择
             ↓
[B] 定位主机（二选一或组合）
        ├─ 按关键字：host-search --ipv4 / --host-name / --os-name / --alive
        └─ 按拓扑：  host-topo-tree 看层级 → 让用户选节点
                     → host-search --topo-nodes '[{"object_id":"module","instance_id":<拓扑树里的 instance_id>}]'
             ↓
[C] 以表格向用户展示候选主机（名称 / IP / bk_cloud_id / bk_host_id / os_name / Agent 状态），
    total > length 时说明「本页 N 条，共 M 条」再问翻页
             ↓
[D] 用户确认目标主机后，取 bk_host_id（优先）或 bk_cloud_id + ip，
    组装到执行命令的主机参数（见第 5 节）
             ↓
[E] 执行类写操作 → 走确认门禁（G1–G4）
```

## 4. 参数要点

### `host-topo-tree`

| 参数 | 说明 |
|------|------|
| `--bk-scope-type` | **仅支持 `biz`**；业务集/租户集无拓扑树，请改用 `host-search` |
| `--bk-scope-id` | 业务 ID |

- 返回是**树形结构**（`child` 递归），默认全展开；叶子节点无 `child` 字段。
- `host_count` 为去重统计，父节点数量 ≠ 子节点数量之和（同一主机可属多个模块）。
- 节点的 `object_id`（`biz`/`set`/`module`）与 `instance_id` 直接用于 `host-search --topo-nodes` 的过滤。

### `host-search`

| 参数 | 说明 |
|------|------|
| `--ipv4` / `--ipv6` | IP 关键字列表，**逗号分隔**，模糊匹配（与任意关键字相似即命中） |
| `--host-name` | 主机名称关键字列表，逗号分隔，模糊匹配 |
| `--os-name` | 操作系统名称关键字列表，逗号分隔，模糊匹配，如 `linux,centos` |
| `--alive` | Agent 状态过滤：`0` 异常、`1` 正常；不传则不过滤（该状态可能非实时） |
| `--topo-nodes` / `--topo-nodes-file` | 拓扑节点 JSON 数组，元素含 `object_id` + `instance_id`（取自拓扑树）；**仅业务生效** |
| `--offset` / `--length` | 分页；默认 `--length 20`，取值 1~200 |

> **拓扑节点 `topo_node_list` 的三种语义（重要）**：
> - **不传**：不按拓扑过滤，返回资源范围下全部主机。
> - **空数组 `[]`**：视为「无满足条件节点」，直接返回空列表（`total=0`），不再查询。
> - **非空**：按节点过滤，**仅业务(biz)生效**，业务集/租户集会被忽略。
>
> PowerShell 传含引号的 JSON 易转义失败，`--topo-nodes` 建议改用 `--topo-nodes-file` 从文件读取。

## 5. 把查到的主机喂给执行命令（字段名因接口而异）

`host-search` 返回的 `bk_host_id`、`bk_cloud_id`、`ip` 可直接组装到执行参数。**不同执行接口的主机字段名不同**，务必对齐：

| 目标命令 | 主机参数 | 结构（优先用 bk_host_id） |
|----------|----------|---------------------------|
| `fast-execute-script` | `--host-id-list` / `--ip-list` / `--execute-target-file` | `--host-id-list 10001,10002`，或 `--ip-list 0:127.0.0.1`（`bk_cloud_id:ip`） |
| `plan-execute`（主机变量 type=3） | `--global-vars-file` | `server.ip_list` 或 `server.host_id_list`，见 [job-plans-search-and-execute.md](job-plans-search-and-execute.md) |
| `plan-create`（执行目标变量 type=3） | `--variables-file` | `execute_target.host_list`，见 [job-plans-create-and-cron.md](job-plans-create-and-cron.md) |
| `cron-save`（主机变量 type=3） | `--global-vars-file` | `server.ip_list` 或 `server.host_id_list` |

> **⚠️ 用 IP 指定主机时，必须同时给 `bk_cloud_id`（管控区域 ID）与 `ip`**，只给 IP 会报「目标执行对象为空」。`host-search` 返回里同时含 `bk_host_id` 与 `bk_cloud_id`，**优先用 `bk_host_id`** 可避免云区域歧义。

## 6. 命令示例

```bash
# A. 看业务拓扑树（仅业务），让用户选集群/模块
python scripts/job_apigw_client.py host-topo-tree \
  --bk-scope-id <业务ID>

# B. 按 IP 关键字 + 仅 Agent 正常，搜索主机（先一页 20 条）
python scripts/job_apigw_client.py host-search \
  --bk-scope-id <业务ID> \
  --ipv4 10.0.0 --alive 1 --length 20

# C. 按操作系统 + 主机名关键字搜索
python scripts/job_apigw_client.py host-search \
  --bk-scope-id <业务ID> \
  --os-name linux,centos --host-name mysql

# D. 按拓扑模块精确取主机（instance_id 取自 host-topo-tree）
cat > /tmp/topo_nodes.json << 'EOF'
[
  {"object_id": "module", "instance_id": 2001}
]
EOF
python scripts/job_apigw_client.py host-search \
  --bk-scope-id <业务ID> \
  --topo-nodes-file /tmp/topo_nodes.json --length 50

# E. 业务集下按 IP 搜索（拓扑节点对业务集不生效，仅关键字过滤）
python scripts/job_apigw_client.py host-search \
  --bk-scope-type biz_set --bk-scope-id <业务集ID> \
  --ipv4 127.0.0
```

## 7. Token 效率与展示

- 主机列表可能很大：遵循「先一页」策略（默认 `--length 20`），`total > length` 时说明「本页 N 条，共 M 条」并询问是否翻页；大列表用 jq / `python -c` 本地过滤，**勿把整页 JSON 贴进对话**。见 [listing-and-token-efficient-analysis.md](listing-and-token-efficient-analysis.md)。
- 向用户展示时用**表格**：主机名 / IP / `bk_cloud_id` / `bk_host_id` / 操作系统 / Agent 状态（`alive` 转「正常/异常」）。

## 8. 相关手册与接口文档

- [scope-selection-and-onboarding.md](scope-selection-and-onboarding.md) — 首次引导选择业务/业务集
- [fast-execute-script.md](fast-execute-script.md) — 快速执行脚本的执行目标参数
- [job-plans-search-and-execute.md](job-plans-search-and-execute.md) — 执行方案启动的主机变量
- [job-plans-create-and-cron.md](job-plans-create-and-cron.md) — 方案创建 / 定时任务的执行目标字段名差异
- [confirmation-and-output-protocol.md](confirmation-and-output-protocol.md) — 执行类写操作确认门禁
- [`../apidocs/v4_get_biz_host_topo_tree.md`](../apidocs/v4_get_biz_host_topo_tree.md) — 拓扑树字段级 API 文档
- [`../apidocs/v4_search_scope_host.md`](../apidocs/v4_search_scope_host.md) — 主机搜索字段级 API 文档
