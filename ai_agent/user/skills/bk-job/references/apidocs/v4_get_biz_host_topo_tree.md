### 功能描述

查询指定业务的主机拓扑树，默认全部展开：以「业务 → 集群（set）→ 模块（module）」的层级递归返回完整拓扑，并给出每个节点（含所有子节点去重后）的主机数量。

常用于**执行类操作填写主机**前，先让用户按拓扑结构选择目标范围（如某个模块），再用 `search_scope_host` 传入对应拓扑节点精确取出主机，得到 `bk_host_id` / `ip` + `bk_cloud_id` 用于快速执行脚本、启动执行方案等。

本接口**仅支持业务（`biz`）**；传业务集（`biz_set`）、租户集（`tenant_set`）将返回参数错误。业务集/租户集下的主机检索请改用 `search_scope_host`（无拓扑树，可用关键字过滤）。

### 请求参数

#### Body参数

| 字段 | 类型 | 必选 | 描述 |
|------|------|------|------|
| bk_scope_type | string | 是 | 资源范围类型。本接口仅支持 `biz`（业务）；传 `biz_set`（业务集）、`tenant_set`（租户集）将返回参数错误 |
| bk_scope_id | string | 是 | 资源范围 ID，与 `bk_scope_type` 对应，表示业务 ID |

### 请求参数示例

- POST `/api/v4/get_biz_host_topo_tree`

```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "2"
}
```

### 返回结果示例

#### 失败示例

```json
# http status: 400
{
    "error": {
        "code": "INVALID_ARGUMENT",
        "message": "资源范围类型[biz_set]不支持该功能，仅支持[BIZ]"
    }
}
```

#### 成功示例

```json
# http status=200
{
    "data": {
        "object_id": "biz",
        "object_name": "业务",
        "instance_id": 2,
        "instance_name": "蓝鲸",
        "host_count": 12,
        "child": [
            {
                "object_id": "set",
                "object_name": "集群",
                "instance_id": 100,
                "instance_name": "公共组件",
                "host_count": 8,
                "child": [
                    {
                        "object_id": "module",
                        "object_name": "模块",
                        "instance_id": 2001,
                        "instance_name": "consul",
                        "host_count": 3
                    },
                    {
                        "object_id": "module",
                        "object_name": "模块",
                        "instance_id": 2002,
                        "instance_name": "mysql",
                        "host_count": 5
                    }
                ]
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
| data | object | 是 | 响应数据（拓扑树根节点），只有在正常响应时才存在该字段，异常响应时不存在 |

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

#### data（拓扑节点，树形递归）

| 字段 | 类型 | 是否一定存在 | 描述 |
|------|------|--------|------|
| object_id | string | 是 | 节点类型 ID。可选值：`biz`（业务）、`set`（集群）、`module`（模块） |
| object_name | string | 是 | 节点类型名称 |
| instance_id | long | 是 | 节点实例 ID；作为 `search_scope_host` 拓扑节点过滤中的 `instance_id` |
| instance_name | string | 是 | 节点实例名称 |
| host_count | int | 是 | 该节点（含所有子节点去重后）的主机数量 |
| child | array | 否 | 子节点列表，元素结构与本节点相同，逐层递归直至叶子节点；**叶子节点不返回该字段** |

> 拓扑树默认全部展开，不含懒加载（lazy）相关字段。节点主机数量为去重统计，父节点的 `host_count` 不等于子节点 `host_count` 之和（同一主机可能属于多个模块）。
