### 功能描述

查询作业平台最新服务版本号

### 请求参数

{{ bkapi_authorization_description }}

#### Header参数

| 字段                    | 类型     | 必选  | 描述                                                                                                                               |
|-----------------------|--------|-----|----------------------------------------------------------------------------------------------------------------------------------|
| X-Bkapi-Authorization | string | 是   | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept                | string | 是   | 固定值。application/json                                                                                                             |
| Content-Type          | string | 是   | 固定值。application/json                                                                                                             |

#### Query参数

无

### 请求参数示例

- GET

```json
/api/v3/get_latest_service_version
```

### 返回结果示例

```json
{
    "result": true,
    "code": 0,
    "data": {
        "version": "3.6.4"
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

##### data

| 字段      | 类型     | 是否一定存在 | 描述      |
|---------|--------|--------|---------|
| version | string | 是      | 服务最新版本号 |
