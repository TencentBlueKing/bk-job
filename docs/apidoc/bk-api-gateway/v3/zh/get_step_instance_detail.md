### 功能描述

根据步骤实例 ID 查询步骤实例详情

### 请求参数说明

{{ bkapi_authorization_description }}

#### Header参数

| 字段      |  类型      | 必选   |  描述      |
|-----------|------------|--------|------------|
| X-Bkapi-Authorization       |  string    | 是     | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept       |  string    | 是     | 固定值。application/json|
| Content-Type |  string    | 是     | 固定值。application/json|

#### Query参数

| 字段                    |  类型      | 必选   |  描述      |
|------------------------|-----------|-------|------------|
| bk_scope_type          | string    | 是     | 资源范围类型。可选值: biz - 业务，biz_set - 业务集 |
| bk_scope_id            | string    | 是     | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |
| step_instance_id       | long      | 是     | 步骤实例ID |


### 请求参数示例

- GET
```json
/api/v3/get_step_instance_detail?bk_scope_type=biz&bk_scope_id=1&step_instance_id=100
```

### 返回结果示例

```json
{
    "result": true,
    "code": 0,
    "message": "",
    "data": {
        "id": 4,
        "type": 1,
        "name": "Step 1",
        "script_step_info": {
          "content": "IyEvYmluL2Jhc2gKCmFueW5vd3RpbWU9ImRhdGUgKyclWS0lbS0lZCAl",
          "script_language": 1,
          "script_param": "1 2 3",
          "timeout": 1000,
          "account_id": 123,
          "account_name": "root",
          "secure_param": 0
        },
        "file_step_info": {
          "file_source_list": [
            {
              "file_type": 1,
              "file_location": [
                "/tmp/1.txt",
                "/tmp/2.txt"
              ],
              "file_hash": "68b329da9893e34099c7d8ad5cb9c940",
              "file_size": 10240,
              "account_id": 1,
              "account_name": "root",
              "file_source_id": 1,
              "file_source_name": "测试用蓝鲸制品库"
            }
          ],
          "file_destination": {
            "path": "/tmp",
            "account_id": 1,
            "account_name": "root"
          },
          "timeout": 1000,
          "upload_speed_limit": 10,
          "download_speed_limit": 10,
          "transfer_mode": 2,
          "ignore_error": 0
        },
        "approval_step_info": {
          "approval_message": "Pass"
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
| data         | object | 请求返回的数据|
| permission   | object | 权限信息|

##### data

| 字段                  | 类型      | 描述      |
|----------------------|-----------|-----------|
| id                   | long      | 作业步骤ID |
| type                 | int       | 步骤类型：1-脚本，2-文件，3-人工确认 |
| name                 | string    | 步骤名称 |
| script_step_info     | object    | 脚本步骤信息 |
| file_step_info       | object    | 文件步骤信息 |
| approval_step_info   | object    | 审批步骤信息 |


##### script_step_info

| 字段                | 类型      | 描述      |
|--------------------|-----------|-----------|
| script_source      | int       | 脚本类型：1-本地脚本，2-引用业务脚本，3-引用公共脚本 |
| script_id          | string    | 脚本ID |
| script_version_id  | long      | 脚本版本ID |
| content            | string    | BASE64编码的脚本内容 |
| script_language    | int       | 脚本语言：1-shell，2-bat，3-perl，4-python，5-powershell，6-sql |
| script_param       | string    | 脚本参数 |
| timeout            | int       | 脚本超时时间，单位为秒 |
| account_id         | long      | 执行账号ID   |
| account_name       | string    | 执行账号名称  |
| secure_param       | int       | 参数是否为敏感参数：0-不敏感，1-敏感 |


##### file_step_info

| 字段                  | 类型               | 描述       |
|----------------------|-------------------|-----------|
| file_source_list     | list<file_source> | 源文件列表，元素详情见file_source对象定义 |
| file_destination     | object            | 目标信息，详情见file_destination对象定义 |
| timeout              | int               | 超时，单位为秒 |
| upload_speed_limit   | int               | 上传文件限速，单位为MB/s，没有值表示不限速 |
| download_speed_limit | int               | 下载文件限速，单位为MB/s，没有值表示不限速 |
| transfer_mode        | int               | 传输模式： 1-严谨模式，2-强制模式，3-安全模式 |
| ignore_error         | int               | 是否忽略错误：0-不忽略，1-忽略 |

##### file_source

| 字段               | 类型         | 描述       |
|-------------------|--------------|-----------|
| file_type         | int          | 文件类型：1-服务器文件，2-本地文件，3-文件源文件 |
| file_location     | list<string> | 文件路径列表 |
| file_hash         | string       | 文件Hash值，仅本地文件该字段有值 |
| file_size         | int          | 文件大小，单位为字节，仅本地文件该字段有值 |
| account_id        | long         | 执行账号ID |
| account_name      | string       | 执行账号名称 |
| file_source_id    | long         | 第三方文件源ID |
| file_source_name  | string       | 第三方文件源名称 |

##### file_destination

| 字段             | 类型      | 描述      |
|-----------------|-----------|-----------|
| path            | string    | 目标路径   |
| account_id      | long      | 执行账号ID |
| account_name    | string    | 执行账号名称 |


##### approval_step_info

| 字段               | 类型       | 描述      |
|-------------------|-----------|-----------|
| approval_message  | string    | 确认消息    |
