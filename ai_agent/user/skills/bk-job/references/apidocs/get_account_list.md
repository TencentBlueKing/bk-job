### 功能描述

查询指定资源范围（业务 / 业务集）下的执行账号列表，支持按账号用途、账号名称、别名过滤，分页返回。

常用于**执行类操作需要填写执行账号**（如 `fast_execute_script` 的 `account_alias` / `account_id`）而用户未指定时，先列出该资源范围下可用账号供用户选择：返回的 `id` 用于 `account_id`，`alias` 用于 `account_alias`。

### 请求参数

{{ bkapi_authorization_description }}

#### Header参数

| 字段                    | 类型     | 必选 | 描述                                                                                                                               |
|-----------------------|--------|----|----------------------------------------------------------------------------------------------------------------------------------|
| X-Bkapi-Authorization | string | 是  | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept                | string | 是  | 固定值。application/json                                                                                                             |
| Content-Type          | string | 是  | 固定值。application/json                                                                                                             |

#### Query参数

| 字段            | 类型     | 必选 | 描述                                          |
|---------------|--------|----|---------------------------------------------|
| bk_scope_type | string | 是  | 资源范围类型。可选值：`biz`（业务）、`biz_set`（业务集）         |
| bk_scope_id   | string | 是  | 资源范围 ID，与 `bk_scope_type` 对应，表示业务 ID 或业务集 ID |
| category      | int    | 否  | 账号用途。可选值：`1`（系统账号）、`2`（DB账号）；不传则不区分         |
| account       | string | 否  | 账号名称过滤                                      |
| alias         | string | 否  | 账号别名过滤                                      |
| start         | int    | 否  | 分页记录起始位置，从 0 开始，不传默认为 0                     |
| length        | int    | 否  | 单次返回最大记录数，最大 1000，不传默认为 20                  |

### 请求参数示例

- GET

```json
/api/v3/get_account_list?bk_scope_type=biz&bk_scope_id=1&category=1&start=0&length=20
```

### 返回结果示例

```json
{
    "result": true,
    "code": 0,
    "data": {
        "start": 0,
        "total": 12,
        "length": 20,
        "data": [
            {
                "id": 70,
                "bk_scope_type": "biz",
                "bk_scope_id": "1",
                "account": "root",
                "alias": "root",
                "category": 1,
                "type": 1,
                "os": "Linux",
                "description": "系统 root 账号",
                "creator": "admin",
                "create_time": 1614659536108,
                "last_modify_user": "admin",
                "last_modify_time": 1614659536116
            }
        ]
    },
    "job_request_id": "xxx"
}
```

### 返回结果说明

| 字段             | 类型     | 是否一定不为null | 描述                         |
|----------------|--------|------------|----------------------------|
| result         | bool   | 是          | 请求成功与否。true:请求成功；false请求失败 |
| code           | int    | 是          | 错误编码。 0表示success，>0表示失败错误  |
| message        | string | 否          | 请求失败返回的错误信息                |
| data           | object | 否          | 请求返回的数据                    |
| job_request_id | string | 否          | 请求ID，请求唯一标识                |
| permission     | object | 否          | 无权限返回的权限信息                 |

#### data

| 字段     | 类型    | 是否一定不为null | 描述                 |
|--------|-------|------------|--------------------|
| start  | int   | 是          | 分页的起始位置            |
| total  | int   | 是          | 命中的记录总数（分页前）       |
| length | int   | 是          | 单次请求返回的最大记录数       |
| data   | array | 是          | 账号数据列表，见 data.data |

##### data.data

| 字段                   | 类型     | 是否一定不为null | 描述                                               |
|----------------------|--------|------------|--------------------------------------------------|
| id                   | long   | 是          | 账号 ID；执行类接口用作 `account_id`                       |
| bk_scope_type        | string | 是          | 资源范围类型。可选值：`biz`（业务）、`biz_set`（业务集）             |
| bk_scope_id          | string | 是          | 资源范围 ID，与 `bk_scope_type` 对应，表示业务 ID 或业务集 ID     |
| account              | string | 是          | 账号名称                                             |
| alias                | string | 是          | 账号别名；执行类接口用作 `account_alias`                     |
| category             | int    | 是          | 账号用途。可选值：`1`（系统账号）、`2`（DB账号）                     |
| type                 | int    | 是          | 账号类型。可选值：`1`（Linux）、`2`（Windows）、`9`（MySQL）、`10`（Oracle）、`11`（DB2） |
| os                   | string | 否          | 账号用途为系统账号（`category=1`）时生效，账号对应的 OS              |
| description          | string | 否          | 账号描述                                             |
| db_system_account_id | long   | 否          | 账号用途为 DB 账号（`category=2`）时生效，关联的系统账号 ID           |
| creator              | string | 是          | 创建人                                              |
| create_time          | long   | 是          | 创建时间，Unix 时间戳，单位毫秒                               |
| last_modify_user     | string | 是          | 最近一次修改人                                          |
| last_modify_time     | long   | 是          | 最近一次修改时间，Unix 时间戳，单位毫秒                           |
