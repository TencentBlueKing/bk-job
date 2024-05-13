### 功能描述

修改账号（当前仅支持系统账号）

### 请求参数

{{ common_args_desc }}

#### 接口参数

| 字段           |  类型      | 必选    |  描述      |
|---------------|------------|--------|------------|
| bk_scope_type | string     | 是     | 资源范围类型。可选值: biz - 业务，biz_set - 业务集 |
| bk_scope_id   | string     | 是     | 资源范围ID，与bk_scope_type对应，表示业务ID或者业务集ID |
| id            | long       | 是     | 账号ID |
| password      | string     | 否     | Windows系统账号密码 |
| alias         | string     | 否     | 账号别名 |
| description   | string     | 否     | 账号描述 |

### 请求参数示例

```json
{
    "bk_app_code": "esb_test",
    "bk_app_secret": "xxx",
    "bk_token": "xxx",
    "bk_scope_type": "biz",
    "bk_scope_id": "1",
    "id": 100,
    "password": "testPwd",
    "alias": "testAlias",
    "description": "update account for windows"
}
```

### 返回结果示例

```json
{
    "code": 0,
    "message": null,
    "result": true,
    "data": {
        "id": 100,
        "bk_scope_type": "biz",
        "bk_scope_id": "1",
        "account": "test",
        "alias": "testAlias",
        "category": 1,
        "type": 2,
        "os": "Windows",
        "description": "update account for windows",
        "creator": "admin",
        "create_time": 1715584657584,
        "last_modify_user": "admin",
        "last_modify_time": 1715584681063
    },
    "job_request_id": "0126bf577c82344bc8167d46764d8c83"
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

| 字段                   | 类型       | 描述      |
|-----------------------|-----------|-----------|
| id                    | long      | 账号ID |
| bk_scope_type         | string    | 资源范围类型。可选值: biz - 业务，biz_set - 业务集 |
| bk_scope_id           | string    | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |
| account               | string    | 账号名称 |
| alias                 | string    | 账号别名 |
| category              | int       | 账号用途（1：系统账号） |
| type                  | int       | 账号类型（1：Linux，2：Windows）|
| os                    | string    | 账号用途为系统账号时该字段生效，账号对应的OS |
| description           | string    | 账号描述  |
| creator               | string    | 创建人 |
| create_time           | long      | 创建时间Unix时间戳（ms） |
| last_modify_user      | string    | 最近一次修改人 |
| last_modify_time      | long      | 最近一次修改时间Unix时间戳（ms） |
