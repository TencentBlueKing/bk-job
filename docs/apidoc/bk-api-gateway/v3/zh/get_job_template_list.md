### 功能描述

查询作业模版列表

### 请求参数

{{ bkapi_authorization_description }}

#### Header参数

| 字段                    | 类型     | 必选  | 描述                                                                                                                               |
|-----------------------|--------|-----|----------------------------------------------------------------------------------------------------------------------------------|
| X-Bkapi-Authorization | string | 是   | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept                | string | 是   | 固定值。application/json                                                                                                             |
| Content-Type          | string | 是   | 固定值。application/json                                                                                                             |

#### Query参数

| 字段                     | 类型     | 必选  | 描述                                                |
|------------------------|--------|-----|---------------------------------------------------|
| bk_scope_type          | string | 是   | 资源范围类型。可选值: biz - 业务，biz_set - 业务集                |
| bk_scope_id            | string | 是   | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID           |
| bk_biz_id              | long   | 是   | *已弃用*。业务ID。此字段已被弃用并由字段bk_scope_type+bk_scope_id替换 |
| creator                | string | 否   | 作业执行方案创建人账号                                       |
| name                   | string | 否   | 作业执行方案名称，模糊匹配                                     |
| create_time_start      | long   | 否   | 创建起始时间，Unix 时间戳                                   |
| create_time_end        | long   | 否   | 创建结束时间，Unix 时间戳                                   |
| last_modify_user       | string | 否   | 作业执行方案修改人账号                                       |
| last_modify_time_start | long   | 否   | 最后修改起始时间，Unix 时间戳                                 |
| last_modify_time_end   | long   | 否   | 最后修改结束时间，Unix 时间戳                                 |
| start                  | int    | 否   | 默认0表示从第1条记录开始返回                                   |
| length                 | int    | 否   | 单次返回最大记录数，最大1000，不传默认为20                          |

### 请求参数示例

- GET

```json
/api/v3/get_job_template_list?bk_scope_type=biz&bk_scope_id=1&creator=admin&start=0&length=20&create_time_start=1546272000000&create_time_end=1577807999999
```

### 返回结果示例

```json
{
    "result": true,
    "code": 0,
    "data": {
        "data": [
            {
                "bk_scope_type": "biz",
                "bk_scope_id": "1",
                "id": 100,
                "name": "test",
                "creator": "admin",
                "create_time": 1546272000000,
                "last_modify_user": "admin",
                "last_modify_time": 1546272000000
            }
        ],
        "start": 0,
        "length": 20,
        "total": 1
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

#### data

| 字段     | 类型    | 是否一定存在 | 描述                 |
|--------|-------|--------|--------------------|
| start  | int   | 是      | 分页的起始位置            |
| total  | int   | 是      | 本次请求返回的记录数         |
| length | int   | 是      | 单次请求返回的最大记录数       |
| data   | array | 是      | 返回的数据列表，见data.data |

##### data.data

| 字段               | 类型     | 是否一定存在 | 描述                                      |
|------------------|--------|--------|-----------------------------------------|
| bk_scope_type    | string | 是      | 资源范围类型。可选值: biz - 业务，biz_set - 业务集      |
| bk_scope_id      | string | 是      | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |
| id               | long   | 是      | 作业模版 ID                                 |
| name             | string | 是      | 作业模版名称                                  |
| creator          | string | 是      | 创建人账号                                   |
| create_time      | long   | 是      | 创建时间，Unix 时间戳                           |
| last_modify_user | string | 是      | 修改人账号                                   |
| last_modify_time | long   | 是      | 最后修改时间，Unix 时间戳                         |
