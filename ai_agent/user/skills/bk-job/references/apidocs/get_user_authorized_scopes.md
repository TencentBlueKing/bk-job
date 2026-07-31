### 功能描述

查询当前调用用户有权限访问的资源范围（业务/业务集）列表，支持分页。

常用于**首次使用**、尚不确定 `bk_scope_type` / `bk_scope_id` 时，先列出可选的业务（`biz`）或业务集（`biz_set`）供用户选择，再进行后续操作（如快速执行脚本、搜索执行方案等）。资源范围以调用身份的权限为准。

### 请求参数

#### Query参数

| 字段 | 类型 | 必选 | 描述 |
|------|------|------|------|
| offset | int | 否 | 分页起始偏移，从 0 开始，须不小于 0，默认 0 |
| length | int | 否 | 单页返回条数，取值范围 1～200，默认 10 |

### 请求参数示例

- GET

```
/api/v4/get_user_authorized_scopes?offset=0&length=20
```

### 返回结果示例

#### 失败示例

```json
# http status: 400
{
    "error": {
        "code": "INVALID_ARGUMENT",
        "message": "请求参数[length]不合法"
    }
}
```

#### 成功示例

```json
# http status=200
{
    "data": {
        "total": 2,
        "offset": 0,
        "length": 20,
        "scope_list": [
            {
                "bk_scope_type": "biz",
                "bk_scope_id": "2",
                "name": "蓝鲸",
                "favor": true,
                "favor_time": "2026-01-01 10:00:00",
                "time_zone": "Asia/Shanghai"
            },
            {
                "bk_scope_type": "biz_set",
                "bk_scope_id": "9991001",
                "name": "运维业务集",
                "favor": false,
                "favor_time": null,
                "time_zone": "Asia/Shanghai"
            }
        ]
    }
}
```

### 返回结果参数说明

#### response

##### 正常响应体

| 字段 | 类型 | 是否一定存在 | 描述 |
|------|------|--------|------|
| data | object | 是 | 响应数据，只有在正常响应时才存在该字段，异常响应时不存在 |

##### 异常响应体

| 字段 | 类型 | 是否一定存在 | 描述 |
|------|------|--------|------|
| error | object | 是 | 错误信息，只有在异常响应时（HTTP状态码!=2xx）才存在该字段（参数错误等），正常响应时不存在 |

#### error

| 字段 | 类型 | 是否一定存在 | 描述 |
|------|------|--------|------|
| code | string | 是 | 错误码 |
| message | string | 是 | 错误信息 |
| data | object | 否 | 错误具体内容，权限信息等 |

#### data

| 字段 | 类型 | 描述 |
|------|------|------|
| total | long | 有权限的资源范围总数（用于分页） |
| offset | int | 本次分页起始偏移 |
| length | int | 本次分页返回条数 |
| scope_list | array | 资源范围列表，定义见 scope |

##### scope

| 字段 | 类型 | 描述 |
|------|------|------|
| bk_scope_type | string | 资源范围类型。可选值：`biz`（业务）、`biz_set`（业务集） |
| bk_scope_id | string | 资源范围 ID，与 `bk_scope_type` 对应 |
| name | string | 资源范围名称（业务名或业务集名） |
| favor | boolean | 当前用户是否已收藏该资源范围。可选值：`true`、`false` |
| favor_time | string | 收藏时间；未收藏时为 `null` |
| time_zone | string | 资源范围所属时区，如 `Asia/Shanghai` |
