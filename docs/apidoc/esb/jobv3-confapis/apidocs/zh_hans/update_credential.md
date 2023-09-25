### 功能描述

更新凭据。

### 请求参数

{{ common_args_desc }}

#### 接口参数

| 字段                        |  类型      | 必选   |  描述       |
|----------------------------|------------|--------|------------|
| bk_scope_type | string | 是     | 资源范围类型。可选值: biz - 业务，biz_set - 业务集 |
| bk_scope_id | string | 是 | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |
| id                         |  string    | 是     | 凭据 ID     |
| name                       |  string    | 否     | 凭据名称     |
| type                       |  string    | 否     | 凭据类型，取值可为ACCESS_KEY_SECRET_KEY,PASSWORD,USERNAME_PASSWORD,SECRET_KEY |
| description                |  string    | 否     | 凭据描述 |
| credential_access_key      |  string    | 否     | 凭据类型为ACCESS_KEY_SECRET_KEY时填写 |
| credential_secret_key      |  string    | 否     | 凭据类型为ACCESS_KEY_SECRET_KEY/SECRET_KEY时填写 |
| credential_username        |  string    | 否     | 凭据类型为USERNAME_PASSWORD时填写 |
| credential_password        |  string    | 否     | 凭据类型为USERNAME_PASSWORD/PASSWORD时填写 |


### 请求参数示例

```json
{
    "bk_app_code": "esb_test",
    "bk_app_secret": "xxx",
    "bk_token": "xxx",
    "bk_scope_type": "biz",
    "bk_scope_id": "1",
    "id": "06644309e10e4068b3c7b32799668210",
    "name": "testCredential",
    "type": "USERNAME_PASSWORD",
    "description": "This is a new credential",
    "credential_username": "admin",
    "credential_password": "newPassword"
}
```

### 返回结果示例

```json
{
    "result": true,
    "code": 0,
    "message": "success",
    "data": {
        "id": "06644309e10e4068b3c7b32799668210"
    }
}
```

### 返回结果参数说明

#### response
| 字段      | 类型      | 描述      |
|-----------|-----------|-----------|
| result       | bool   | 请求成功与否。true:请求成功；false请求失败 |
| code         | int    | 错误编码。 0表示success，>0表示失败错误 |
| message      | string | 请求失败返回的错误信息|
| data         | object | 请求返回的数据|
| permission   | object | 权限信息|


#### data

| 字段      | 类型    |字段是否一定存在  | 描述      |
|-----------|-------|---------------|---------|
| id        | string |是             | 凭据ID |
