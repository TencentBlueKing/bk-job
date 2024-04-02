### 功能描述

查询凭证详情

### 请求参数

{{ common_args_desc }}

#### 接口参数

| 字段          | 类型   | 必选 | 描述                                                    |
| ------------- | ------ | ---- | ------------------------------------------------------- |
| bk_scope_type | string | 是   | 资源范围类型。可选值: biz - 业务，biz_set - 业务集      |
| bk_scope_id   | string | 是   | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |
| id            | String | 是   | 凭据ID                                                  |

### 请求参数示例

```json
{
    "bk_app_code": "esb_test",
    "bk_app_secret": "xxx",
    "bk_token": "xxx",
    "bk_scope_type": "biz",
    "bk_scope_id": "1",
    "id": "xxx"
}
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
        "id": "xxx",
        "name": "xxx",
        "type": "xxx",
        "description": "xxx",
        "creator": "admin",
        "create_time": 1712049204719,
        "last_modify_user": "admin",
        "last_modify_time": "1712049204719"
    }
}
```

### 返回结果参数说明

#### response

| 字段       | 类型   | 描述                                       |
| ---------- | ------ | ------------------------------------------ |
| result     | bool   | 请求成功与否。true:请求成功；false请求失败 |
| code       | int    | 错误编码。 0表示success，>0表示失败错误    |
| message    | string | 请求失败返回的错误信息                     |
| data       | object | 请求返回的数据                             |
| permission | object | 权限信息                                   |


#### data

| 字段             | 类型   | 描述                                                         |
| ---------------- | ------ | ------------------------------------------------------------ |
| bk_scope_type    | string | 资源范围类型。可选值: biz - 业务，biz_set - 业务集           |
| bk_scope_id      | string | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID      |
| id               | string    | 凭据ID                                                       |
| name             | string | 凭证名称                                                     |
| type             | string | 类型。可选值：APP_ID_SECRET_KEY，PASSWORD，USERNAME_PASSWORD，SECRET_KEY|
| description      | string | 描述                                                         |
| creator          | string | 作业创建人账号                                               |
| create_time      | long   | 创建时间，Unix 时间戳                                        |
| last_modify_user | string | 作业修改人账号                                               |
| last_modify_time | long   | 最后修改时间，Unix 时间戳                                    |
