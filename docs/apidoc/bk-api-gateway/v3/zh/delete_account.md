### 功能描述

删除账号。

### 请求参数

{{ bkapi_authorization_description }}

#### Header参数

| 字段                       |  类型      | 必选     |  描述       |
|---------------------------|------------|---------|------------|
| X-Bkapi-Authorization     | string     | 是      | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept                    |  string    | 是      | 固定值。application/json|
| Content-Type              |  string    | 是      | 固定值。application/json|

#### Body参数

| 字段           |  类型      | 必选    |  描述      |
|---------------|------------|--------|------------|
| bk_scope_type | string     | 是     | 资源范围类型。可选值: biz - 业务，biz_set - 业务集 |
| bk_scope_id   | string     | 是     | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |
| bk_biz_id     | long   | 是   | *已弃用*。业务ID。此字段已被弃用并由字段bk_scope_type+bk_scope_id替换 |
| id            | long       | 否     | 账号ID |

### 请求参数示例

- POST
```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "1",
    "id": 70
}
```

### 返回结果示例

```json
{
    "code": 0,
    "result": true,
    "data": {
        "id": 70,
        "bk_scope_type": "biz",
        "bk_scope_id": "1",
        "account": "Admin",
        "alias": "Admin",
        "category": 1,
        "type": 2,
        "db_system_account_id": null,
        "os": "Windows",
        "description": "An account for windows",
        "creator": "admin",
        "create_time": 1614659536108,
        "last_modify_user": "admin",
        "last_modify_time": 1614659536116
    },
    "job_request_id": "xxx"
}
```

### 返回结果参数说明

#### response
| 字段             | 类型     | 是否一定存在 | 描述                         |
|----------------|--------|--------|----------------------------|
| result         | bool   | 是      | 请求成功与否。true:请求成功；false请求失败 |
| code           | int    | 是      | 错误编码。 0表示success，>0表示失败错误  |
| message        | string | 否      | 请求失败返回的错误信息                |
| data           | object | 否      | 请求返回的数据，删除操作可能没有值          |
| job_request_id | string | 是      | 请求ID，请求唯一标识                |
| permission     | object | 否      | 无权限返回的权限信息                 |


#### data

| 字段                   | 类型     | 是否一定存在 | 描述                                               |
|----------------------|--------|--------|--------------------------------------------------|
| id                   | long   | 是      | 账号ID                                             |
| bk_scope_type        | string | 是      | 资源范围类型。可选值: biz - 业务，biz_set - 业务集               |
| bk_scope_id          | string | 是      | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID          |
| account              | string | 是      | 账号名称                                             |
| alias                | string | 是      | 账号别名                                             |
| category             | int    | 是      | 账号用途（1：系统账号，2：DB账号）                              |
| type                 | int    | 是      | 账号类型（1：Linux，2：Windows，9：MySQL，10：Oracle，11：DB2） |
| os                   | string | 否      | 账号用途为系统账号时该字段生效，账号对应的OS                          |
| description          | string | 否      | 账号描述                                             |
| db_system_account_id | long   | 否      | 账号用途为DB账号时该字段生效，对应的系统账号ID                        |
| creator              | string | 是      | 创建人                                              |
| create_time          | long   | 是      | 创建时间Unix时间戳（ms）                                  |
| last_modify_user     | string | 是      | 最近一次修改人                                          |
| last_modify_time     | long   | 是      | 最近一次修改时间Unix时间戳（ms）                              |
