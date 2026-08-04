# 快速执行脚本

到用户指定的机器上快速执行一段脚本（无需预先创建作业模板与执行方案）。字段级说明见 [`../apidocs/v4_fast_execute_script.md`](../apidocs/v4_fast_execute_script.md)。

> **⚠️ 高风险写操作**：本子命令会在目标机器上**真实执行脚本**，属生产变更类操作。真实执行前**必须**遵守 [confirmation-and-output-protocol.md](confirmation-and-output-protocol.md) 第 1 节的确认门禁（先展示确认摘要、等用户下一条独立回复后再执行）。`--dry-run` 仅打印请求体，不视为已确认执行。

## 1. 子命令

| 子命令 | 接口 | 说明 |
|--------|------|------|
| `fast-execute-script` | `POST /api/v4/fast_execute_script` | 到指定目标机器执行脚本，写操作，须过确认门禁 |

## 2. 脚本来源（三选一，优先级从高到低）

| 参数 | 说明 |
|------|------|
| `--script-version-id` | 引用已有脚本的某个版本 ID（优先级最高） |
| `--script-id` | 引用已有脚本（使用其上线版本） |
| `--script-content` / `--script-content-file` | 直接传入脚本内容明文，脚本会**自动 Base64 编码**后提交；PowerShell 等环境推荐用 `--script-content-file` 从文件读取，避免转义问题 |

- 使用 `--script-content` 时须指定 `--script-language`（名称 `shell`/`bat`/`perl`/`python`/`powershell`，或编码 `1`~`5`），未指定默认 `shell`。
- 引用脚本（`--script-id`/`--script-version-id`）时，脚本语言以脚本自身为准，`--script-language` 忽略。

## 3. 执行目标（至少一种）

| 参数 | 说明 |
|------|------|
| `--host-id-list` | 目标主机 `bk_host_id` 列表，逗号分隔，如 `101,102` |
| `--ip-list` | 目标主机 IP 列表，逗号分隔，格式为 `bk_cloud_id:ip`，如 `0:127.0.0.1,0:127.0.0.2` |
| `--execute-target-file` | 从文件读取完整 `execute_target` JSON，支持**动态分组 / 拓扑节点 / 容器**等复杂目标 |

> **⚠️ 主机参数注意**：使用 IP 指定目标时，必须同时提供 `bk_cloud_id`（管控区域ID）和 `ip`；只给 IP 会导致「目标执行对象为空」错误。动态分组用 `dynamic_group_list[{id}]`、拓扑节点用 `topo_node_list[{id,node_type}]`，容器用 `kube_container_filters`，均通过 `--execute-target-file` 传入。

> **不确定目标主机时先查再填**：用户只给了 IP 网段/主机名/操作系统/拓扑等模糊条件时，先用只读的 `host-search` / `host-topo-tree` 帮其定位主机（取 `bk_host_id` 优先，或 `bk_cloud_id:ip`），确认后再填入 `--host-id-list` / `--ip-list`。详见 [host-query-and-selection.md](host-query-and-selection.md)。

## 4. 执行账号与其它常用参数

| 参数 | 说明 |
|------|------|
| `--account-alias` / `--account-id` | 执行账号，至少提供一个；同时提供时以 `account_id` 为准。**用户未指定账号时**先用只读的 `account-list` 列出可用账号（脚本执行一般用 `--category system`）引导选择，见 [account-query-and-selection.md](account-query-and-selection.md) |
| `--name` | 自定义作业名称，最长 512 字符；不传由系统自动生成 |
| `--script-param` | 脚本参数明文（自动 Base64 编码）；敏感参数可加 `--param-sensitive` |
| `--timeout` | 超时时间（秒），取值 1~259200，不传用接口默认 7200 |
| `--windows-interpreter` | 自定义 Windows 解释器路径，须以 `.exe` 结尾 |
| `--callback-url` | 任务完成后的回调 URL |
| `--no-start-task` | 仅创建任务不自动启动（`start_task=false`），默认自动启动 |

## 5. 推荐顺序

1. 若尚不确定业务/业务集，先用 `list-authorized-scopes` 引导用户选定资源范围，见 [scope-selection-and-onboarding.md](scope-selection-and-onboarding.md)。用户未给目标主机时用 `host-search`/`host-topo-tree`（见 [host-query-and-selection.md](host-query-and-selection.md)），未指定执行账号时用 `account-list`（见 [account-query-and-selection.md](account-query-and-selection.md)）先查再填。
2. 组装脚本来源、执行目标、执行账号等参数，先用 **`--dry-run`** 打印将提交的请求体（含 Base64 后脚本）。
3. **展示确认摘要**（见第 6 节）→ **等用户下一条独立回复确认**（G2）→ 再真实执行。
4. 执行成功后脚本 JSON 会补充 **`job_instance_url`**（来自 `config.yaml` 的 `job_base_url`），格式为 `{job_base_url}/api_execute/{job_instance_id}`；须将该链接以可点击形式交付用户，并可用 `instance-status` / `get-instance-log` 跟进结果。

## 6. 确认摘要格式（真实执行前必须展示）

```
即将执行的操作：
- 操作类型：快速执行脚本
- 资源范围：{bk_scope_type} / {bk_scope_id}
- 目标机器：{共 N 台 / IP 或 host_id 片段 / 动态分组 / 拓扑节点 / 容器}
- 执行账号：{account_alias 或 account_id}
- 脚本来源：{脚本内容（语言：shell...）/ 引用脚本 script_id / 脚本版本 script_version_id}
- 脚本预览：{前若干行，敏感信息脱敏}
- 超时时间：{timeout 秒，默认 7200}

请确认是否立即执行。
```

脚本内容与参数中的敏感信息须脱敏；主机可摘要为「共 N 台」或列举片段。

## 7. 命令示例

```bash
# A. dry-run 校验请求体（脚本内容从文件读取，避免转义）
python scripts/job_apigw_client.py fast-execute-script \
  --bk-scope-id <业务ID> \
  --script-content-file tmp/restart.sh --script-language shell \
  --account-alias root \
  --host-id-list 101,102 \
  --dry-run

# B. 展示确认摘要 → 用户下一条独立确认（G2）后再真实执行
python scripts/job_apigw_client.py fast-execute-script \
  --bk-scope-id <业务ID> \
  --script-content-file tmp/restart.sh --script-language shell \
  --account-alias root \
  --ip-list 0:127.0.0.1,0:127.0.0.2 \
  --timeout 1000
# 成功返回含 job_instance_id 与 job_instance_url，须以可点击链接交付用户

# C. 复杂目标（动态分组/拓扑/容器）用 --execute-target-file
cat > tmp/target.json << 'EOF'
{
  "dynamic_group_list": [{"id": "blo8gojho0skft7pr5q0"}],
  "topo_node_list": [{"id": 1000, "node_type": "module"}]
}
EOF
python scripts/job_apigw_client.py fast-execute-script \
  --bk-scope-id <业务ID> \
  --script-content-file tmp/restart.sh --script-language shell \
  --account-alias root \
  --execute-target-file tmp/target.json --dry-run
```

## 8. 相关手册与接口文档

- [confirmation-and-output-protocol.md](confirmation-and-output-protocol.md) — 写操作确认门禁与输出规范
- [scope-selection-and-onboarding.md](scope-selection-and-onboarding.md) — 首次引导选择业务/业务集
- [host-query-and-selection.md](host-query-and-selection.md) — 查/搜业务下主机，填写执行目标
- [account-query-and-selection.md](account-query-and-selection.md) — 查询业务下可用执行账号，填写执行账号
- [`../apidocs/v4_fast_execute_script.md`](../apidocs/v4_fast_execute_script.md) — 字段级 API 文档
