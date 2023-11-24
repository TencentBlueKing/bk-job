### 功能描述

查询主机Agent在作业平台中的信息（可执行状态、版本等）

### 请求参数说明

{{ bkapi_authorization_description }}

#### Header参数

| 字段      |  类型      | 必选   |  描述      |
|-----------|------------|--------|------------|
| X-Bkapi-Authorization       |  string    | 是     | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept       |  string    | 是     | 固定值。application/json|
| Content-Type |  string    | 是     | 固定值。application/json|

#### Body参数

| 字段                    |  类型         | 必选   |  描述      |
|------------------------|--------------|-------|------------|
| bk_scope_type          | string       | 是     | 资源范围类型。可选值: biz - 业务，biz_set - 业务集 |
| bk_scope_id            | string       | 是     | 资源范围ID，与bk_scope_type对应, 表示业务ID或者业务集ID |
| host_id_list           | list<long>   | 是     | 主机ID数组，单次查询主机数量不可超过5000  |


### 请求参数示例

- POST
```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "1",
    "host_id_list": [1,2,3]
}
```

### 返回结果示例

```json
{
    "result": true,
    "code": 0,
    "message": "",
    "data": {
        "agent_info_list": [
          {
              "bk_host_id": 1,
              "status": 0,
              "version": "2.1.4"
          },
          {
              "bk_host_id": 2,
              "status": 1,
              "version": "2.1.5"
          },
          {
              "bk_host_id": 3,
              "status": 1,
              "version": "2.1.6"
          }
        ]
    }
}
```
### 返回结果说明

| 字段      | 类型      | 描述      |
|-----------|-----------|-----------|
| result       | bool   | 请求成功与否。true:请求成功；false请求失败 |
| code         | int    | 错误编码。 0表示success，>0表示失败错误 |
| message      | string | 请求失败返回的错误信息|
| data         | object | 请求返回的数据|
| permission   | object | 权限信息|

##### data

| 字段                   | 类型            | 描述      |
|-----------------------|----------------|-----------|
| agent_info_list       | list<object>   | 元素为Agent信息，详情见agent_info对象定义，若传入的host_id在返回结果的列表中不存在，则表示未查询到该主机的agent信息 |


##### agent_info

| 字段             | 类型              | 描述      |
|-----------------|------------------|-----------|
| bk_host_id      | long             | 主机ID     |
| status          | int              | Agent状态：0-异常，1-正常 |
| version         | string           | Agent的版本 |
