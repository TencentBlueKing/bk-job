### 功能描述

查询文件源详情

### 请求参数

{{ bkapi_authorization_description }}

#### Header参数

| 字段      |  类型      | 必选   |  描述      |
|-----------|------------|--------|------------|
| X-Bkapi-Authorization       |  string    | 是     | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept       |  string    | 是     | 固定值。application/json|
| Content-Type |  string    | 是     | 固定值。application/json|

#### Query参数

| 字段                   |  类型       | 必选   |  描述       |
|------------------------|------------|--------|------------|
| bk_scope_type | string | 是     | 资源范围类型。可选值: biz - 业务，biz_set - 业务集 |
| bk_scope_id | string | 是 | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |
| bk_biz_id        |  long       | 是     | *已弃用*。业务ID。此字段已被弃用并由字段bk_scope_type+bk_scope_id替换 |
| code          | String | 是   | 文件源code                                              |

### 请求参数示例

- GET
```json
/api/v3/get_file_source_detail?bk_scope_type=biz&bk_scope_id=2&code=bkrepo-1
```

### 返回结果示例

```json
{
    "result": true,
    "code": 0,
    "message": "",
    "data": {
        "bk_scope_type": "biz",
        "bk_scope_id": "2",
        "id": 10001,
        "code": "bkrepo-1",
        "alias": "xxx",
        "status": 0,
        "file_source_type_code": "BLUEKING_ARTIFACTORY",
        "is_public": false,
        "credential_id": "xxx",
        "enable": true,
        "creator": "admin",
        "create_time": 1712050614742,
        "last_modify_user": "admin",
        "last_modify_time": 1712050614742
    }
}
```

### 返回结果参数说明

| 字段      | 类型      | 描述      |
|-----------|-----------|-----------|
| result       | bool   | 请求成功与否。true:请求成功；false请求失败 |
| code         | int    | 错误编码。 0表示success，>0表示失败错误 |
| message      | string | 请求失败返回的错误信息|
| data         | object | 请求返回的数据|
| permission   | object | 权限信息|

##### data
| 字段             | 类型   | 描述                                                         |
| ---------------- | ------ | ------------------------------------------------------------ |
| bk_scope_type    | string | 资源范围类型。可选值: biz - 业务，biz_set - 业务集           |
| bk_scope_id      | string | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID      |
| id               | int    | 文件源ID                                                     |
| code             | string | 文件源code                                                   |
| alias            | string | 文件源别名                                                   |
| status           | int    | 状态                                                         |
| file_source_type_code | string    | 文件源类型code。可选值：BLUEKING_ARTIFACTORY |
| is_public        | bool   | 是否为公用文件源                                             |
| credential_id    | string | 凭证ID                                                       |
| enable           | bool   | 是否启用                                                     |
| creator          | string | 作业创建人账号                                               |
| create_time      | long   | 创建时间，Unix时间戳（ms）                                        |
| last_modify_user | string | 作业修改人账号                                               |
| last_modify_time | long   | 最后修改时间，Unix时间戳（ms）                                   |
