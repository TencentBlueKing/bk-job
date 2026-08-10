### 功能描述

在指定资源范围（业务 `biz` / 业务集 `biz_set` / 租户集 `tenant_set`）下，按 IP、主机名、操作系统、Agent 状态、拓扑节点等条件搜索主机，分页返回。

常用于**执行类操作填写主机**：先用本接口按关键字或拓扑节点定位目标主机，取返回的 `bk_host_id`（或 `ip` + `bk_cloud_id`），再用于 `fast_execute_script` 的 `execute_target.host_list`、`execute_job_plan` 的主机变量 `server.ip_list` / `server.host_id_list` 等。

出于安全考虑，本接口**不返回 agentId**。`alive` 为作业平台持久化的 Agent 状态，可能非实时。

### 请求参数

#### Body参数

| 字段 | 类型 | 必选 | 描述 |
|------|------|------|------|
| bk_scope_type | string | 是 | 资源范围类型。可选值：`biz`（业务）、`biz_set`（业务集）、`tenant_set`（租户集） |
| bk_scope_id | string | 是 | 资源范围 ID，与 `bk_scope_type` 对应，表示业务 ID、业务集 ID 或租户集 ID |
| topo_node_list | array | 否 | 拓扑节点列表，用于圈定搜索范围，元素定义见 topo_node。三种语义见下方说明；**仅业务（`biz`）生效**，业务集/租户集下传入会被忽略（按整个资源范围搜索） |
| ipv4_key_list | array | 否 | IPv4 关键字列表，模糊匹配；主机 IPv4 与列表中任意一个关键字相似即命中 |
| ipv6_key_list | array | 否 | IPv6 关键字列表，模糊匹配；支持压缩写法；主机 IPv6 与列表中任意一个关键字相似即命中 |
| host_name_key_list | array | 否 | 主机名称关键字列表，模糊匹配；主机名称与列表中任意一个关键字相似即命中 |
| os_name_key_list | array | 否 | 操作系统名称关键字列表，模糊匹配；操作系统名称与列表中任意一个关键字相似即命中 |
| alive | int | 否 | Agent 状态过滤。可选值：`0`（异常）、`1`（正常）；不传则不按 Agent 状态过滤 |
| offset | int | 否 | 分页起始偏移，从 0 开始；须不小于 0，不传默认 0 |
| length | int | 否 | 单页返回条数；取值范围 1～200，不传默认 10 |

> **topo_node_list 三种语义（对所有资源范围一致）**：
> - **不传 / `null`**：拓扑节点不作为过滤条件，返回该资源范围下全部主机。
> - **空数组 `[]`**：视为「无满足条件的节点」（通常为上层交集计算后为空），直接返回空列表（`total=0`、`data=[]`），不再查询。
> - **非空节点列表**：按节点过滤；节点过滤**仅业务（`biz`）生效**，业务集/租户集下会被忽略。

##### topo_node（topo_node_list[] 元素）

| 字段 | 类型 | 必选 | 描述 |
|------|------|------|------|
| object_id | string | 是 | 节点类型 ID。可选值：`biz`（业务）、`set`（集群）、`module`（模块）；取自 `get_biz_host_topo_tree` 的 `object_id` |
| instance_id | long | 是 | 节点实例 ID，须大于 0；取自 `get_biz_host_topo_tree` 的 `instance_id` |

### 请求参数示例

- POST `/api/v4/search_scope_host`

按 IP 关键字搜索：

```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "2",
    "ipv4_key_list": ["127.0.0.1", "192.168.0"],
    "alive": 1,
    "offset": 0,
    "length": 20
}
```

按拓扑节点（模块）搜索（仅业务生效）：

```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "2",
    "topo_node_list": [
        {"object_id": "module", "instance_id": 2001}
    ],
    "offset": 0,
    "length": 20
}
```

### 返回结果示例

#### 失败示例

```json
# http status: 400
{
    "error": {
        "code": "INVALID_ARGUMENT",
        "message": "请求参数[length]不合法"
    }
}
```

#### 成功示例

```json
# http status=200
{
    "data": {
        "total": 2,
        "offset": 0,
        "length": 20,
        "data": [
            {
                "bk_host_id": 10001,
                "ip": "127.0.0.1",
                "ipv6": "",
                "bk_cloud_id": 0,
                "bk_cloud_name": "默认管控区域",
                "host_name": "VM-1-1-centos",
                "os_name": "linux centos",
                "os_type": "1",
                "alive": 1
            },
            {
                "bk_host_id": 10002,
                "ip": "127.0.0.2",
                "ipv6": "",
                "bk_cloud_id": 0,
                "bk_cloud_name": "默认管控区域",
                "host_name": "VM-1-2-centos",
                "os_name": "linux centos",
                "os_type": "1",
                "alive": 0
            }
        ]
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

| 字段 | 类型 | 是否一定存在 | 描述 |
|------|------|--------|------|
| total | long | 是 | 命中的主机总数（分页前），用于分页 |
| offset | int | 是 | 本次查询的分页起始偏移，从 0 开始 |
| length | int | 是 | 本次查询的单页条数 |
| data | array | 是 | 主机列表，元素定义见 host；无数据时为空数组 |

##### host（data[] 元素）

| 字段 | 类型 | 是否一定存在 | 描述 |
|------|------|--------|------|
| bk_host_id | long | 是 | 主机 ID；可直接用于执行类接口的 `host_list[{bk_host_id}]` / `host_id_list` |
| ip | string | 否 | 主机 IPv4 地址 |
| ipv6 | string | 否 | 主机 IPv6 地址 |
| bk_cloud_id | long | 是 | 管控区域 ID；用 IP 指定主机时须与 `ip` 一起使用 |
| bk_cloud_name | string | 否 | 管控区域名称 |
| host_name | string | 否 | 主机名称 |
| os_name | string | 否 | 操作系统名称 |
| os_type | string | 否 | 操作系统类型标识（由 CMDB 同步） |
| alive | int | 否 | Agent 状态。可选值：`0`（异常）、`1`（正常）；为作业平台持久化的 Agent 状态，可能非实时 |
