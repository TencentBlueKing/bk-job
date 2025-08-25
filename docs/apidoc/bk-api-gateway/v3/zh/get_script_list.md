### 功能描述

查询业务脚本列表

### 请求参数

{{ bkapi_authorization_description }}

#### Header参数

| 字段                    | 类型     | 必选  | 描述                                                                                                                               |
|-----------------------|--------|-----|----------------------------------------------------------------------------------------------------------------------------------|
| X-Bkapi-Authorization | string | 是   | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept                | string | 是   | 固定值。application/json                                                                                                             |
| Content-Type          | string | 是   | 固定值。application/json                                                                                                             |

#### Query参数

| 字段              | 类型     | 必选  | 描述                                                                   |
|-----------------|--------|-----|----------------------------------------------------------------------|
| bk_scope_type   | string | 是   | 资源范围类型。可选值: biz - 业务，biz_set - 业务集                                   |
| bk_scope_id     | string | 是   | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID                              |
| bk_biz_id       | long   | 是   | *已弃用*。业务ID。此字段已被弃用并由字段bk_scope_type+bk_scope_id替换                    |
| name            | string | 否   | 脚本名称，支持模糊查询                                                          |
| script_language | int    | 否   | 脚本语言。0：所有脚本类型，1：shell，2：bat，3：perl，4：python，5：powershell，6：sql。默认值为0 |
| start           | int    | 否   | 分页记录起始位置，不传默认为0                                                      |
| length          | int    | 否   | 单次返回最大记录数，最大1000，不传默认为20                                             |

### 请求参数示例

- GET

```json
/api/v3/get_script_list?bk_scope_type=biz&bk_scope_id=1&name=script1&script_language=1&start=0&length=10
```

### 返回结果示例

```json
{
    "result": true,
    "code": 0,
    "data": {
        "data": [
            {
                "id": "000dbdddc06c453baf1f2decddf00c69",
                "bk_scope_type": "biz",
                "bk_scope_id": "1",
                "name": "a.sh",
                "script_language": 1,
                "online_script_version_id": 100,
                "creator": "admin",
                "create_time": 1600746078520,
                "last_modify_user": "admin",
                "last_modify_time": 1600746078520
            }
        ],
        "start": 0,
        "length": 10,
        "total": 1
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

| 字段                       | 类型     | 是否一定存在 | 描述                                                                     |
|--------------------------|--------|--------|------------------------------------------------------------------------|
| id                       | string | 是      | 脚本ID                                                                   |
| bk_scope_type            | string | 是      | 资源范围类型。可选值: biz - 业务，biz_set - 业务集                                     |
| bk_scope_id              | string | 是      | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID                                |
| name                     | string | 是      | 脚本名称                                                                   |
| script_language          | int    | 是      | 脚本语言。1 - shell, 2 - bat, 3 - perl, 4 - python, 5 - powershell, 6 - SQL |
| online_script_version_id | long   | 否      | 已上线脚本版本ID;如果脚本没有已上线版本，该值为空                                             |
| creator                  | string | 是      | 创建人                                                                    |
| create_time              | long   | 是      | 创建时间Unix时间戳（ms）                                                        |
| last_modify_user         | string | 是      | 最近一次修改人                                                                |
| last_modify_time         | long   | 是      | 最近一次修改时间Unix时间戳（ms）                                                    |
| description              | string | 否      | 脚本描述                                                                   |
