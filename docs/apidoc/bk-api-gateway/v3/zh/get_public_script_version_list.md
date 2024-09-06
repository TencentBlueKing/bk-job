### 功能描述

查询公共脚本版本列表

### 请求参数

{{ bkapi_authorization_description }}

#### Header参数

| 字段      |  类型      | 必选   |  描述      |
|-----------|------------|--------|------------|
| X-Bkapi-Authorization       |  string    | 是     | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept       |  string    | 是     | 固定值。application/json|
| Content-Type |  string    | 是     | 固定值。application/json|

#### Query参数

| 字段       |  类型      | 必选   |  描述      |
|----------------------|------------|--------|------------|
| script_id              |  string    | 是     | 脚本ID |
| return_script_content  |  bool      | 否     | 是否需要返回脚本内容。true:返回脚本内容；false：不返回脚本内容。默认为false。 |
| start                  |  int       | 否     | 分页记录起始位置，不传默认为0 |
| length                 |  int       | 否     | 单次返回最大记录数，最大1000，不传默认为20 |

### 请求参数示例

- GET
```json
/api/v3/get_public_script_version_list?script_id=000dbdddc06c453baf1f2decddf00c69&return_script_content=true&start=0&length=10
```

### 返回结果示例

```json
{
    "result": true, 
    "code": 0, 
    "message": "success", 
    "data": {
        "data": [
            {
                "id": 1, 
                "script_id": "000dbdddc06c453baf1f2decddf00c69", 
                "version": "V1.0", 
                "content": "#!/bin/bash***", 
                "status": 1, 
                "version_desc": "版本描述", 
                "creator": "admin", 
                "create_time": 1600746078520, 
                "last_modify_user": "admin", 
                "last_modify_time": 1600746078520,
                "script_language": 1,
                "public_script": true,
                "description": "脚本描述"
            }
        ], 
        "start": 0, 
        "length": 10, 
        "total": 1
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

| 字段      | 类型      | 描述      |
|-----------|-----------|-----------|
| id                | long      | 脚本版本ID |
| script_id         | string    | 脚本版本所属的脚本ID |
| version           | string    | 版本号 |
| content           | string    | 脚本内容 |
| status            | int       | 脚本版本状态（0：未上线，1：已上线，2：已下线，3：已禁用） |
| version_desc      | string    | 版本描述  |
| creator           | string    | 创建人 |
| create_time       | long      | 创建时间Unix时间戳（ms） |
| last_modify_user  | string    | 最近一次修改人 |
| last_modify_time  | long      | 最近一次修改时间Unix时间戳（ms） |
| script_language   | int    | 脚本语言:1 - shell, 2 - bat, 3 - perl, 4 - python, 5 - powershell |
| public_script     | bool   | 是否公共脚本                                                 |
| description       | string | 脚本描述                                                     |
