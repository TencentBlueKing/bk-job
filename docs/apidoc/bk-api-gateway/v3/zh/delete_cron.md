### 功能描述

删除定时任务

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
| id            | long       | 否     | 定时任务ID |

### 请求参数示例

- POST
```json
{
    "bk_app_code": "esb_test",
    "bk_app_secret": "xxx",
    "bk_token": "xxx",
    "bk_scope_type":"biz",
    "bk_scope_id":"2",
    "id": 1000045
}
```

### 返回结果示例

```json
{
    "code": 0,
    "result": true
}
```

### 返回结果参数说明

#### response
| 字段          | 类型      | 描述      |
|--------------|-----------|-----------|
| result       | bool   | 请求成功与否。true:请求成功；false请求失败 |
| code         | int    | 错误编码。 0表示success，>0表示失败错误 |
| message      | string | 请求失败返回的错误信息|
| data         | object | 请求返回的数据|
| permission   | object | 权限信息|


#### data

无
