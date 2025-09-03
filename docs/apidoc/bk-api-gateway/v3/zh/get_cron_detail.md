### 功能描述

查询定时作业详情

### 请求参数

{{ bkapi_authorization_description }}

#### Header参数

| 字段                    | 类型     | 必选  | 描述                                                                                                                               |
|-----------------------|--------|-----|----------------------------------------------------------------------------------------------------------------------------------|
| X-Bkapi-Authorization | string | 是   | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept                | string | 是   | 固定值。application/json                                                                                                             |
| Content-Type          | string | 是   | 固定值。application/json                                                                                                             |

#### Query参数

| 字段            | 类型     | 必选  | 描述                                                |
|---------------|--------|-----|---------------------------------------------------|
| bk_scope_type | string | 是   | 资源范围类型。可选值: biz - 业务，biz_set - 业务集                |
| bk_scope_id   | string | 是   | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID           |
| bk_biz_id     | long   | 是   | *已弃用*。业务ID。此字段已被弃用并由字段bk_scope_type+bk_scope_id替换 |
| id            | long   | 否   | 定时任务 ID                                           |

### 请求参数示例

- GET

```json
/api/v3/get_cron_detail?bk_scope_type=biz&bk_scope_id=1&id=1
```

### 返回结果示例

```json
{
    "result": true,
    "code": 0,
    "data": {
        "bk_scope_type": "biz",
        "bk_scope_id": "1",
        "job_plan_id": 100,
        "id": 1,
        "name": "test",
        "status": 1,
        "expression": "0/5 * * * ?",
        "global_var_list": [
            {
                "id": 436,
                "name": "ip",
                "server": {
                    "dynamic_group_list": [
                        {
                            "id": "blo8gojho0skft7pr5q0"
                        },
                        {
                            "id": "blo8gojho0sabc7priuy"
                        }
                    ],
                    "ip_list": [
                        {
                            "bk_host_id": 101,
                            "bk_cloud_id": 0,
                            "ip": "127.0.0.1"
                        },
                        {
                            "bk_host_id": 102,
                            "bk_cloud_id": 0,
                            "ip": "127.0.0.2"
                        }
                    ],
                    "topo_node_list": [
                        {
                            "id": 1000,
                            "node_type": "module"
                        }
                    ]
                }
            },
            {
                "id": 437,
                "name": "text",
                "value": "new String value"
            }
        ],
        "creator": "admin",
        "create_time": 1546272000000,
        "last_modify_user": "admin",
        "last_modify_time": 1577807999999
    },
    "job_request_id": "xxx"
}
```

### 返回结果参数说明

| 字段             | 类型     | 是否一定存在 | 描述                         |
|----------------|--------|--------|----------------------------|
| result         | bool   | 是      | 请求成功与否。true:请求成功；false请求失败 |
| code           | int    | 是      | 错误编码。 0表示success，>0表示失败错误  |
| message        | string | 否      | 请求失败返回的错误信息                |
| data           | object | 否      | 请求返回的数据，删除操作可能没有值          |
| job_request_id | string | 否      | 请求ID，请求唯一标识                |
| permission     | object | 否      | 无权限返回的权限信息                 |

##### data

| 字段               | 类型     | 是否一定存在 | 描述                                      |
|------------------|--------|--------|-----------------------------------------|
| id               | long   | 是      | 定时任务 ID                                 |
| name             | string | 是      | 定时作业名称                                  |
| status           | int    | 是      | 定时任务状态                                  |
| bk_scope_type    | string | 是      | 资源范围类型。可选值: biz - 业务，biz_set - 业务集      |
| bk_scope_id      | string | 是      | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |
| job_plan_id      | long   | 是      | 要定时执行的作业的执行方案 ID                        |
| creator          | string | 是      | 创建人                                     |
| create_time      | long   | 是      | 创建时间Unix时间戳（s）                          |
| last_modify_user | string | 是      | 最近一次修改人                                 |
| last_modify_time | long   | 是      | 最近一次修改时间Unix时间戳（s）                      |
| expression       | string | 否      | 定时任务 crontab 的定时规则，                     |
| execute_time     | long   | 否      | 定时任务单次执行的执行时间，Unix时间戳                   |
| global_var_list  | array  | 否      | 全局变量信息，定义见global_var                    |

##### global_var

| 字段     | 类型     | 是否一定存在 | 描述                                     |
|--------|--------|--------|----------------------------------------|
| id     | long   | 是      | 全局变量 id，唯一标识。如果 id 为空，那么使用 name 作为唯一标识 |
| name   | string | 是      | 全局变量名                                  |
| type   | int    | 是      | 全局变量类型                                 |
| value  | string | 否      | 字符、密码、数组类型的全局变量的值                      |
| server | object | 否      | 主机类型全局变量的值                             |

##### server

| 字段                 | 类型     | 是否一定存在 | 描述                 |
|--------------------|--------|--------|--------------------|
| variable           | string | 否      | 引用的变量名             |
| ip_list            | array  | 否      | 静态IP列表             |
| host_id_list       | array  | 否      | 静态主机ID列表，元素为Long类型 |
| dynamic_group_list | array  | 否      | 动态分组ID列表           |
| topo_node_list     | array  | 否      | 动态topo节点列表         |

##### ip

| 字段          | 类型     | 是否一定存在 | 描述     |
|-------------|--------|--------|--------|
| bk_cloud_id | int    | 是      | 管控区域ID |
| ip          | string | 是      | IP地址   |

##### dynamic_group

| 字段  | 类型     | 是否一定存在 | 描述      |
|-----|--------|--------|---------|
| id  | string | 是      | 动态分组 ID |

##### topo_node

| 字段        | 类型     | 是否一定存在 | 描述                                                  |
|-----------|--------|--------|-----------------------------------------------------|
| id        | long   | 是      | 动态topo节点ID，对应CMDB API 中的 bk_inst_id                 |
| node_type | string | 是      | 动态topo节点类型，对应CMDB API 中的 bk_obj_id,比如"module","set" |
