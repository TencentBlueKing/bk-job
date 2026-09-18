### 功能描述

更新指定的执行方案，可修改方案名称、启用的步骤以及全局变量取值。

本接口作用于**执行方案当前的步骤与变量快照**，不要求方案已与作业模板同步。作业模板变更后（方案的 `need_update` 为 `true`），仍可直接调用本接口修改方案；只有要启用模板新增的步骤时，才需要先调用 `sync_job_plan` 把方案同步到模板最新版本。

调用前须满足：执行方案存在于该资源范围内；调用身份对该方案具备编辑权限。传入 `name` 时，同一资源范围、同一作业模板下的执行方案名称不可重复。

### 请求参数

#### Header参数

| 字段                    | 类型     | 必选 | 描述                                                                                                                               |
|-----------------------|--------|----|----------------------------------------------------------------------------------------------------------------------------------|
| X-Bkapi-Authorization | string | 是  | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept                | string | 是  | 固定值。application/json                                                                                                             |
| Content-Type          | string | 是  | 固定值。application/json                                                                                                             |

#### Body参数

| 字段            | 类型     | 必选 | 描述                                                                                            |
|---------------|--------|----|-----------------------------------------------------------------------------------------------|
| bk_scope_type | string | 是  | 资源范围类型。可选值：`biz`（业务）、`biz_set`（业务集）                                                           |
| bk_scope_id   | string | 是  | 资源范围 ID，须与 `bk_scope_type` 对应，表示业务 ID 或业务集 ID                                                 |
| job_plan_id   | long   | 是  | 执行方案 ID，须大于 0，且对应方案须已存在                                                                       |
| name          | string | 否  | 执行方案名称。不传或传空白表示不改名，保留方案原有名称；传入时长度 1～60 个字符，不可包含 HTML 特殊字符 `<`、`>`、`"`、`'`，且在同一资源范围、同一作业模板下须唯一 |
| enable_steps  | array  | 是  | 要启用的**方案步骤** ID 列表，不可为空数组；未列出的步骤会被置为未启用；列表中的 ID 须均属于该方案                                       |
| variables     | array  | 否  | 全局变量覆盖列表，按变量名与**执行方案自身的**全局变量匹配；变量名在该方案中不存在，或与变量类型不匹配时将报错；同一请求中变量名不可重复                        |

`enable_steps` 中的 ID 是**方案步骤 ID**，与 `create_job_plan` 使用的模板步骤 ID 不同，可通过 v3 接口 `get_job_plan_detail` 获取。传入不属于该方案的 ID 会报错；若要启用的是作业模板新增的步骤，请先调用 `sync_job_plan` 同步执行方案。

该字段设为必填是为了避免误清空：服务端会把未列出的步骤一律置为未启用，若允许缺省，漏传就等同于禁用方案的所有步骤。

`variables` 与 `enable_steps` 一样按方案自身的快照寻址。方案的全局变量是创建或同步时从作业模板复制过来的，模板之后新增的变量在同步前并不存在于方案中，传入这类变量名会报错，须先调用 `sync_job_plan`；反过来，模板中已删除但方案仍保留的变量，在这里依然可以修改。

##### variables[] 元素

| 字段              | 类型      | 必选 | 描述                                                                                                                        |
|-----------------|---------|----|---------------------------------------------------------------------------------------------------------------------------|
| name            | string  | 是  | 执行方案中的全局变量名称，按名称匹配（不支持按变量 ID）；不可为空                                                                                        |
| value           | string  | 否  | 变量默认值。适用于该变量类型为字符串、命名空间、密文、关联数组、索引数组、执行账号时；执行账号变量的值为账号 ID 字符串；当 `follow_template` 为 `true` 时不生效                           |
| execute_target  | object  | 否  | 执行目标。仅当该变量为「执行目标列表」类型且 `follow_template` 为 `false` 时填写；填写时不可再填 `value`                                                    |
| follow_template | boolean | 否  | 是否沿用模板中该变量的默认值。可选值：`true`（沿用模板默认值）、`false`（使用本请求中的 `value` 或 `execute_target`，默认 `false`）。为 `true` 时不可填写 `execute_target` |

##### execute_target

| 字段                 | 类型    | 必选 | 描述          |
|--------------------|-------|----|-------------|
| host_list          | array | 否  | 静态主机列表      |
| dynamic_group_list | array | 否  | 动态分组列表      |
| topo_node_list     | array | 否  | CMDB 拓扑节点列表 |

填写 `execute_target` 时，`host_list`、`dynamic_group_list`、`topo_node_list` 至少须有一项非空。

##### host_list[] 元素

| 字段          | 类型     | 必选 | 描述                                                                      |
|-------------|--------|----|-------------------------------------------------------------------------|
| bk_host_id  | long   | 否  | 主机ID。与ip+bk_cloud_id必须存在一个。当同时存在bk_host_id和ip+bk_cloud_id时，bk_host_id优先 |
| bk_cloud_id | long   | 否  | 云区域ID。与bk_host_id必须存在一个。当同时存在bk_host_id和ip+bk_cloud_id时，bk_host_id优先    |
| ip          | string | 否  | IP地址。与bk_host_id必须存在一个。当同时存在bk_host_id和ip+bk_cloud_id时，bk_host_id优先     |


##### dynamic_group_list[] 元素

| 字段 | 类型     | 必选 | 描述      |
|----|--------|----|---------|
| id | string | 是  | 动态分组 ID |

##### topo_node_list[] 元素

| 字段        | 类型     | 必选 | 描述                                |
|-----------|--------|----|-----------------------------------|
| id        | long   | 是  | 拓扑节点 ID                           |
| node_type | string | 是  | 拓扑节点类型。可选值：`module`（模块）、`set`（集群） |


### 请求参数示例

- POST

```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "2",
    "job_plan_id": 50001,
    "name": "my-api-plan",
    "enable_steps": [
        201,
        203
    ],
    "variables": [
        {
            "name": "TARGET_DIR",
            "value": "/data/release-v2",
            "follow_template": false
        },
        {
            "name": "HOST_TARGET",
            "follow_template": false,
            "execute_target": {
                "host_list": [
                    {
                        "bk_host_id": 10001
                    }
                ]
            }
        }
    ]
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
                    "id": "access_business",
                    "name": "业务访问",
                    "related_resource_types": [
                        {
                            "type": "biz",
                            "system_id": "bk_cmdb",
                            "system_name": "配置平台",
                            "type_name": "业务",
                            "instances": [
                                [
                                    {
                                        "id": "1",
                                        "type": "business",
                                        "name": "blueking",
                                        "type_name": "业务"
                                    }
                                ]
                            ]
                        }
                    ]
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
        "bk_scope_type": "biz",
        "bk_scope_id": "2",
        "job_plan_id": 50001,
        "job_plan_name": "my-api-plan",
        "job_template_id": 1000,
        "creator": "admin",
        "create_time": 1738220000000,
        "need_update": false
    }
}
```

### 返回结果参数说明

##### 正常响应体

| 字段   | 类型     | 是否一定存在 | 描述                           |
|------|--------|--------|------------------------------|
| data | object | 是      | 响应数据，只有在正常响应时才存在该字段，异常响应时不存在 |

##### 异常响应体

| 字段    | 类型     | 是否一定存在 | 描述                                                     |
|-------|--------|--------|--------------------------------------------------------|
| error | object | 是      | 错误信息，只有在异常响应时（HTTP状态码!=2xx）才存在该字段（权限不足、参数错误等），正常响应时不存在 |

#### error

| 字段      | 类型     | 是否一定存在 | 描述           |
|---------|--------|--------|--------------|
| code    | string | 是      | 错误码          |
| message | string | 是      | 错误信息         |
| data    | object | 否      | 错误具体内容，权限信息等 |


#### data

| 字段              | 类型      | 描述                                                |
|-----------------|---------|---------------------------------------------------|
| bk_scope_type   | string  | 资源范围类型。可选值：`biz`（业务）、`biz_set`（业务集）               |
| bk_scope_id     | string  | 资源范围 ID                                           |
| job_plan_id     | long    | 执行方案 ID                                           |
| job_plan_name   | string  | 执行方案名称                                            |
| job_template_id | long    | 作业模板 ID                                           |
| creator         | string  | 创建人                                               |
| create_time     | long    | 创建时间，Unix 时间戳，单位毫秒                                |
| need_update     | boolean | 是否需要根据作业模板同步更新该执行方案。可选值：`true`（需要同步）、`false`（不需要） |
