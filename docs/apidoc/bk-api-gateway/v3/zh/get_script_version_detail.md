### 功能描述

查询业务脚本版本详情

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
| bk_scope_type | string | 是     | 资源范围类型。可选值: biz - 业务，biz_set - 业务集 |
| bk_scope_id | string | 是 | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |
| bk_biz_id        |  long       | 是     | *已弃用*。业务ID。此字段已被弃用并由字段bk_scope_type+bk_scope_id替换 |
| id             |  long       | 否   | 脚本版本ID，若传入则以此条件为准屏蔽其他条件 |
| script_id      |  string     | 否     | 脚本ID（可与version一起传入定位某个脚本版本）  |
| version        |  string     | 否     | 脚本版本（可与script_id一起传入定位某个脚本版本） |

### 请求参数示例

- GET
```json
/api/v3/get_script_version_detail?bk_scope_type=biz&bk_scope_id=1&id=1
```

### 返回结果示例

```json
{
    "result": true,
    "code": 0,
    "message": "success",
    "data": {
        "id": 1,
        "bk_scope_type": "biz",
        "bk_scope_id": "1",
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
        "public_script": false,
        "description": "脚本描述"
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
| bk_scope_type | string |资源范围类型。可选值: biz - 业务，biz_set - 业务集 |
| bk_scope_id   | string | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |
| script_id         | string    | 脚本版本所属的脚本ID |
| version           | string    | 版本号 |
| content           | string    | 脚本版本内容 |
| status            | int       | 脚本版本状态（0：未上线，1：已上线，2：已下线，3：已禁用） |
| version_desc      | string    | 版本描述  |
| creator           | string    | 创建人 |
| create_time       | long      | 创建时间Unix时间戳（ms） |
| last_modify_user  | string    | 最近一次修改人 |
| last_modify_time  | long      | 最近一次修改时间Unix时间戳（ms） |
| script_language   | int    | 脚本语言:1 - shell, 2 - bat, 3 - perl, 4 - python, 5 - powershell |
| public_script     | bool   | 是否公共脚本                                                 |
| description       | string | 脚本描述                                                     |
