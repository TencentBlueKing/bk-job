# Job v4 接口响应协议

V4版本的接口全面采用蓝鲸新版 HTTP API 协议，严格遵守标准 HTTP 状态码。

注意：与V2/V3接口最大的区别是，从V4接口开始，不再全部返回 200 状态码，使用标准HTTP状态码

## 常用状态码

- 10x 正常不会用到, 特殊场景除外(例如websocket)

- 20x

  - 200 OK: 请求成功
  
  - 201 Created: POST接口创建时使用, 通常响应中包含Location

  - 204 No Content: 表示操作成功但是无响应体, 例如 DELETE成功

- 40x

  - 400 Bad Request: 客户端请求数据错误, 错误的请求语法, 无效的请求消息等

  - 401 Unauthorized: 缺乏身份验证凭证

  - 403 Forbidden: 拒绝授权访问

  - 404 Not Found: 资源不存在

  - 405 Method Not Allowed: 服务器禁止该HTTP请求方法

  - 409 Conflict: 资源冲突

  - 429 Too Many Requests: 给定的时间内发送了太多请求（"限制请求速率"）

- 50x

  - 500 Internal Server Error: 服务端错误

注意：20x 都代表请求成功, 判断请求成功请使用 http_status>=200 && http_status<300 来判断，而不是http_status==200。

## 响应体

### 正常响应体

```json
HTTP状态码:200
{
  "data": {} / []
}
```

| 字段   | 类型     | 是否一定存在 | 描述                           |
|------|--------|--------|------------------------------|
| data | object | 是      | 响应数据，只有在正常响应时才存在该字段，异常响应时不存在 |

#### 正常响应示例

```json
HTTP状态码:200
响应体:{
    "data": {
        "job_instance_id": 1,
        "step_instance_list": [
            {
                "step_instance_id": 1,
                "step_instance_name": "step1"
            },
            {
                "step_instance_id": 2,
                "step_instance_name": "step2"
            }
        ]
    }
}
```

### 异常响应体

```json
HTTP状态码!=20x
{
  "error": {}
}
```

| 字段    | 类型     | 是否一定存在 | 描述                                                     |
|-------|--------|--------|--------------------------------------------------------|
| error | object | 是      | 错误信息，只有在异常响应时（HTTP状态码!=2xx）才存在该字段（权限不足、参数错误等），正常响应时不存在 |

#### error

| 字段      | 类型     | 是否一定存在 | 描述           |
|---------|--------|--------|--------------|
| code    | string | 是      | 错误码          |
| message | string | 是      | 错误信息         |
| data    | object | 否      | 错误具体内容，权限信息等 |

#### 异常响应示例

```json
HTTP状态码:400
响应体:{
    "error": {
        "code": "INVALID_ARGUMENT",
        "message": "请求参数[bk_scope_type]不合法"
    }
}
```

```json
HTTP状态码:403
响应体:{
    "error": {
        "code": "NO_PERMISSION",
        "message": "用户(张三)权限不足，请前往权限中心确认并申请补充后重试",
        "data": {
            "system_id": "bk_job",
            "system_name": "作业平台",
            "actions": [
                {
                    "id": "access_business",
                    "name": "业务访问",
                    "related_resource_types": [
                        {
                            "type": "biz",
                            "system_id": "bk_cmdb",
                            "system_name": "配置平台",
                            "type_name": "业务",
                            "instances": [
                                [
                                    {
                                        "id": "1",
                                        "type": "business",
                                        "name": "blueking",
                                        "type_name": "业务"
                                    }
                                ]
                            ]
                        }
                    ]
                }
            ]
        }
    }
}
```
