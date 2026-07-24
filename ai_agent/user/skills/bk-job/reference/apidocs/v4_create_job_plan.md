### 功能描述

在指定的资源范围（业务或业务集）内，基于**已存在**的作业模板创建一条新的执行方案。

调用前须满足：目标作业模板存在；调用身份对该资源范围具备查看该模板、创建执行方案的操作权限。同一资源范围、同一作业模板下，执行方案名称不可重复。

### 请求参数

#### Body参数

| 字段              | 类型    | 必选 | 描述 |
|-------------------|---------|------|------|
| bk_scope_type     | string  | 是   | 资源范围类型。可选值：`biz`（业务）、`biz_set`（业务集） |
| bk_scope_id       | string  | 是   | 资源范围 ID，须与 `bk_scope_type` 对应，表示业务 ID 或业务集 ID |
| job_template_id   | long    | 是   | 作业模板 ID，须大于 0，且对应模板须已存在 |
| name              | string  | 是   | 执行方案名称，长度 1～60 个字符；不可包含 HTML 特殊字符 `<`、`>`、`"`、`'`；在同一资源范围、同一作业模板下须唯一 |
| enable_steps      | array   | 否   | 要在新方案中启用的模板步骤 ID 列表；不传时启用模板中的全部步骤；不可传空数组；列表中的 ID 须均属于该模板 |
| variables         | array   | 否   | 模板全局变量覆盖列表，按变量名与模板中的全局变量匹配；变量名不存在或与模板变量类型不匹配时将报错；同一请求中变量名不可重复 |

##### variables[] 元素

| 字段             | 类型    | 必选 | 描述 |
|------------------|---------|------|------|
| name             | string  | 是   | 模板中的全局变量名称，按名称匹配（不支持按变量 ID）；不可为空 |
| value            | string  | 否   | 变量默认值。适用于模板中变量类型为字符串、命名空间、密文、关联数组、索引数组时；当 `follow_template` 为 `true` 时不生效 |
| execute_target   | object  | 否   | 执行目标。仅当模板中该变量为「执行目标列表」类型且 `follow_template` 为 `false` 时填写；填写时不可再填 `value` |
| follow_template  | boolean | 否   | 是否沿用模板中该变量的默认值。可选值：`true`（沿用模板默认值）、`false`（使用本请求中的 `value` 或 `execute_target`，默认 `false`）。为 `true` 时不可填写 `execute_target` |

##### execute_target

| 字段          | 类型     | 必选 | 描述                                                                      |
|-------------|--------|----|-------------------------------------------------------------------------|
| bk_host_id  | long   | 否  | 主机ID。与ip+bk_cloud_id必须存在一个。当同时存在bk_host_id和ip+bk_cloud_id时，bk_host_id优先 |
| bk_cloud_id | long   | 否  | 云区域ID。与bk_host_id必须存在一个。当同时存在bk_host_id和ip+bk_cloud_id时，bk_host_id优先    |
| ip          | string | 否  | IP地址。与bk_host_id必须存在一个。当同时存在bk_host_id和ip+bk_cloud_id时，bk_host_id优先     |

### 请求参数示例

- POST

```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "2",
    "job_template_id": 1000,
    "name": "my-api-plan",
    "enable_steps": [101, 102, 103],
    "variables": [
        {
            "name": "TARGET_DIR",
            "value": "/data/release",
            "follow_template": false
        },
        {
            "name": "KEEP_TEMPLATE_DEFAULT",
            "follow_template": true
        },
        {
            "name": "HOST_TARGET",
            "follow_template": false,
            "execute_target": {
                "host_list": [
                    {
                        "bk_host_id": 10001
                    },
                    {
                        "bk_cloud_id": 0,
                        "ip": "127.0.0.2"
                    }
                ],
                "dynamic_group_list": [
                    {
                        "id": "asdo8gojhasdfskft7pr5"
                    }
                ],
                "topo_node_list": [
                    {
                        "id": 1000,
                        "node_type": "module"
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

#### response

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

| 字段              | 类型    | 描述 |
|-------------------|---------|------|
| bk_scope_type     | string  | 资源范围类型。可选值：`biz`（业务）、`biz_set`（业务集） |
| bk_scope_id       | string  | 资源范围 ID |
| job_plan_id       | long    | 新建的执行方案 ID |
| job_plan_name     | string  | 执行方案名称 |
| job_template_id   | long    | 作业模板 ID |
| creator           | string  | 创建人 |
| create_time       | long    | 创建时间，Unix 时间戳，单位毫秒 |
| need_update       | boolean | 是否需要根据作业模板同步更新该执行方案。可选值：`true`（需要同步）、`false`（不需要） |
