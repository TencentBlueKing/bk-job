### 功能描述

在指定的资源范围（业务或业务集）内，向用户指定的目标机器快速执行一段脚本（无需预先创建作业模板/执行方案）。

脚本来源支持三种，按优先级从高到低为：引用脚本版本（`script_version_id`）> 引用脚本（`script_id`）> 直接传入脚本内容（`script_content`）。执行目标支持静态主机、动态分组、拓扑节点、容器四种方式，至少提供一种。

调用前须满足：调用身份对该资源范围具备脚本执行操作权限；目标机器已正常纳管；所用执行账号（`account_alias`/`account_id`）在该资源范围内存在。

> **写操作提示**：本接口会在目标机器上真实执行脚本，属高风险生产变更。经技能脚本调用时须遵守 [confirmation-and-output-protocol.md](../manuals/confirmation-and-output-protocol.md) 第 1 节的确认门禁（先展示确认摘要、等用户下一条独立回复后再执行）。

### 请求参数

#### Body参数

| 字段 | 类型 | 必选 | 描述 |
|------|------|------|------|
| bk_scope_type | string | 是 | 资源范围类型。可选值：`biz`（业务）、`biz_set`（业务集） |
| bk_scope_id | string | 是 | 资源范围 ID，须与 `bk_scope_type` 对应，表示业务 ID 或业务集 ID |
| script_version_id | long | 否 | 引用的脚本版本 ID。三种脚本来源之一，优先级最高；填写后 `script_id`、`script_content`、`script_language` 均忽略 |
| script_id | string | 否 | 引用的脚本 ID（使用其上线版本）。三种脚本来源之一；`script_version_id` 未填时生效，填写后 `script_content`、`script_language` 忽略 |
| script_content | string | 否 | 脚本内容，须经 BASE64 编码。三种脚本来源之一；`script_version_id`、`script_id` 均未填时必填。最大长度受 MEDIUMTEXT 限制 |
| script_language | int | 否 | 脚本语言。可选值：`1`（shell）、`2`（bat）、`3`（perl）、`4`（python）、`5`（powershell）。仅当以 `script_content` 传入脚本内容时必填；引用脚本时忽略（以脚本自身语言为准） |
| script_param | string | 否 | 脚本参数，须经 BASE64 编码。执行时参数中的换行会被替换为空格 |
| param_sensitive | boolean | 否 | 是否为敏感参数（为 `true` 时执行详情页隐藏参数值）。可选值：`true`、`false`，默认 `false` |
| account_alias | string | 否 | 执行账号别名，如 `root`。与 `account_id` 至少提供一个 |
| account_id | long | 否 | 执行账号 ID。与 `account_alias` 至少提供一个；同时提供时以 `account_id` 为准 |
| task_name | string | 否 | 自定义作业名称，最大长度 512 个字符。不传时由系统自动生成 |
| windows_interpreter | string | 否 | 自定义 Windows 解释器路径，须以 `.exe` 结尾，最大长度 260 个字符 |
| timeout | int | 否 | 脚本执行超时时间，单位秒，取值范围 1～259200（3 天）；不传时使用默认值 7200 秒。实际上限可能受资源范围级配置进一步限制 |
| execute_target | object | 是 | 执行目标。须至少包含 `host_list`、`dynamic_group_list`、`topo_node_list`、`kube_container_filters` 其中一种，定义见 execute_target |
| callback_url | string | 否 | 任务执行完成后的回调 URL，作业平台在任务结束后回调该地址告知执行结果 |
| start_task | boolean | 否 | 是否创建后立即启动任务。可选值：`true`、`false`，默认 `true`；为 `false` 时仅创建任务不自动启动 |
| rolling_config | object | 否 | 滚动执行配置，定义见 rolling_config |
| host_password_list | array | 否 | 目标主机自定义密码列表（用于口令登录场景），定义见 host_password |

##### execute_target

| 字段 | 类型 | 必选 | 描述 |
|------|------|------|------|
| host_list | array | 否 | 静态主机列表，定义见 host |
| dynamic_group_list | array | 否 | 动态分组列表，定义见 dynamic_group |
| topo_node_list | array | 否 | 拓扑节点列表，定义见 topo_node |
| kube_container_filters | array | 否 | 容器执行目标过滤器列表（k8s 场景），元素含 `kube_cluster_filter`、`kube_namespace_filter`、`kube_workload_filter`、`kube_pod_filter`、`kube_container_prop_filter`、`execute_in_whole_cluster` 等字段。结构较复杂，建议通过技能脚本 `--execute-target-file` 传入完整 JSON |

##### host

| 字段 | 类型 | 必选 | 描述 |
|------|------|------|------|
| bk_host_id | long | 否 | 主机 ID。与 `ip`+`bk_cloud_id` 必须存在一个；同时存在时 `bk_host_id` 优先 |
| bk_cloud_id | long | 否 | 管控区域 ID。与 `bk_host_id` 必须存在一个；须与 `ip` 成对出现 |
| ip | string | 否 | IP 地址。与 `bk_host_id` 必须存在一个；须与 `bk_cloud_id` 成对出现 |

##### dynamic_group

| 字段 | 类型 | 必选 | 描述 |
|------|------|------|------|
| id | string | 是 | CMDB 动态分组 ID |

##### topo_node

| 字段 | 类型 | 必选 | 描述 |
|------|------|------|------|
| id | long | 是 | 拓扑节点 ID，对应 CMDB 中的 `bk_inst_id` |
| node_type | string | 是 | 拓扑节点类型，对应 CMDB 中的 `bk_obj_id`，如 `module`、`set` |

##### rolling_config

| 字段 | 类型 | 必选 | 描述 |
|------|------|------|------|
| type | int | 否 | 滚动对象。可选值：`1`（传输目标）、`2`（源文件），默认 `1` |
| mode | int | 否 | 滚动机制。可选值：`1`（执行失败则暂停）、`2`（忽略失败，自动滚动下一批）、`3`（人工确认），默认 `1` |
| expression | string | 否 | 滚动分批策略表达式，滚动对象为「传输目标」时必填 |

##### host_password

| 字段 | 类型 | 必选 | 描述 |
|------|------|------|------|
| bk_cloud_id | long | 否 | 管控区域 ID |
| ip | string | 否 | 主机 IP |
| host_id | long | 否 | 主机 ID |
| encrypted_password | string | 是 | 加密后的密码，最大长度 172 个字符 |

### 请求参数示例

- POST

```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "2",
    "task_name": "restart-nginx",
    "script_content": "ZWNobyAiaGVsbG8i",
    "script_language": 1,
    "account_alias": "root",
    "timeout": 1000,
    "execute_target": {
        "host_list": [
            {
                "bk_host_id": 10001
            },
            {
                "bk_cloud_id": 0,
                "ip": "127.0.0.2"
            }
        ]
    }
}
```

### 返回结果示例

#### 失败示例

```json
# http status: 400
{
    "error": {
        "code": "INVALID_ARGUMENT",
        "message": "请求参数[bk_scope_type]不合法"
    }
}
```

```json
# http status: 403
{
    "error": {
        "code": "NO_PERMISSION",
        "message": "用户(张三)权限不足，请前往权限中心确认并申请补充后重试",
        "data": {
            "system_id": "bk_job",
            "system_name": "作业平台",
            "actions": [
                {
                    "id": "execute_job",
                    "name": "作业执行",
                    "related_resource_types": []
                }
            ]
        }
    }
}
```

#### 成功示例

```json
# http status=200
{
    "data": {
        "job_instance_id": 10000,
        "job_instance_name": "restart-nginx",
        "step_instance_id": 20000
    }
}
```

### 返回结果参数说明

#### response

##### 正常响应体

| 字段 | 类型 | 是否一定存在 | 描述 |
|------|------|--------|------|
| data | object | 是 | 响应数据，只有在正常响应时才存在该字段，异常响应时不存在 |

##### 异常响应体

| 字段 | 类型 | 是否一定存在 | 描述 |
|------|------|--------|------|
| error | object | 是 | 错误信息，只有在异常响应时（HTTP状态码!=2xx）才存在该字段（权限不足、参数错误等），正常响应时不存在 |

#### error

| 字段 | 类型 | 是否一定存在 | 描述 |
|------|------|--------|------|
| code | string | 是 | 错误码 |
| message | string | 是 | 错误信息 |
| data | object | 否 | 错误具体内容，权限信息等 |

#### data

| 字段 | 类型 | 描述 |
|------|------|------|
| job_instance_id | long | 作业实例 ID，可用于后续查询任务状态与执行日志 |
| job_instance_name | string | 作业实例名称 |
| step_instance_id | long | 步骤实例 ID |
