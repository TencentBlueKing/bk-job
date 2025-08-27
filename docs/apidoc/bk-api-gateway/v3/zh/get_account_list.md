### 功能描述

查询业务下的执行账号列表

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
| category      | int    | 否   | 账号用途（1：系统账号，2：DB账号），不传则不区分                        |
| account       | string | 否   | 账号名称                                              |
| alias         | string | 否   | 账号别名                                              |
| start         | int    | 否   | 分页记录起始位置，不传默认为0                                   |
| length        | int    | 否   | 单次返回最大记录数，最大1000，不传默认为20                          |

### 请求参数示例

- GET

```json
/api/v3/get_account_list?bk_scope_type=biz&bk_scope_id=1&category=1&account=aaa&alias=aaa&start=0&length=1
```

### 返回结果示例

```json
{
    "code": 0,
    "result": true,
    "data": {
        "start": 0,
        "total": 12,
        "data": [
            {
                "id": 70,
                "account": "aaa",
                "alias": "aaa",
                "category": 1,
                "type": 1,
                "os": "Linux",
                "creator": "admin",
                "bk_scope_type": "biz",
                "bk_scope_id": "1",
                "create_time": 1614659536108,
                "last_modify_user": "admin",
                "last_modify_time": 1614659536116
            }
        ],
        "length": 1
    },
    "job_request_id": "xxx"
}
```

### 返回结果说明

| 字段             | 类型     | 是否一定存在 | 描述                         |
|----------------|--------|--------|----------------------------|
| result         | bool   | 是      | 请求成功与否。true:请求成功；false请求失败 |
| code           | int    | 是      | 错误编码。 0表示success，>0表示失败错误  |
| message        | string | 否      | 请求失败返回的错误信息                |
| data           | object | 否      | 请求返回的数据，删除操作可能没有值          |
| job_request_id | string | 是      | 请求ID，请求唯一标识                |
| permission     | object | 否      | 无权限返回的权限信息                 |

##### data

| 字段    | 类型   | 描述                |
|-------|------|-------------------|
| start | int  | 分页记录起始位置          |
| total | int  | 查询结果总量            |
| data  | long | 分页数据，见data.data定义 |

##### data.data

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
