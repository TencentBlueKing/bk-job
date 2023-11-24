### 功能描述

结合业务（集）灰度情况查询主机Agent在作业平台中的信息（可执行状态、版本等）

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
| host_id_list           | list<long>   | 是     | 主机ID数组，单次查询元素数量不可超过5000  |


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
        "agent_info_map": {
          "1": {
              "execute_status": 0,
              "version": "2.1.4"
          },
          "2": {
              "execute_status": 1,
              "version": "2.1.5"
          },
          "3": {
              "execute_status": 1,
              "version": "2.1.6"
          }
        }
    }
}
```
### 返回结果说明

| 字段      | 类型      | 描述      |
|-----------|-----------|-----------|
| result       | bool   | 请求成功与否。true:请求成功；false请求失败 |
| code         | int    | 错误编码。 0表示success，>0表示失败错误 |
| message      | string | 请求失败返回的错误信息|
| data         | map    | 请求返回的数据|
| permission   | object | 权限信息|

##### data

| 字段                   | 类型                    | 描述      |
|-----------------------|------------------------|-----------|
| agent_info_map        | map<string,object>     | key为host_id字符串，value为Agent信息，详情见agent_info对象定义，若传入的host_id在返回结果的key集合中不存在，则表示未查询到该主机的agent信息 |


##### agent_info

| 字段                   | 类型              | 描述      |
|-----------------------|------------------|-----------|
| execute_status        | int              | Agent的可执行状态：0-异常不可执行，1-正常可执行 |
| version               | string           | Agent的版本 |

