### 功能描述

把指定的执行方案同步到其所属作业模板的**当前最新版本**。

同步会按作业模板重建方案的步骤：模板新增的步骤会出现在方案中（默认未启用），模板删除的步骤会从方案中消失，其余步骤保留原有的启用状态；`follow_template` 为 `false` 的变量保留方案中已设置的取值，其余变量跟随模板默认值。

本接口不接收模板版本号。服务层的版本参数是给页面做乐观锁用的（用户看到版本差异对比后再确认），API 调用方没有这个界面概念，因此本接口语义固定为「同步到模板当前最新版本」，并在响应中回传实际同步到的版本。

方案未处于待同步状态（`need_update` 为 `false`）时调用本接口不会报错，同步到当前版本即为空操作。

调用前须满足：执行方案存在于该资源范围内；调用身份对该方案具备同步权限。

### 请求参数

#### Header参数

| 字段                    | 类型     | 必选 | 描述                                                                                                                               |
|-----------------------|--------|----|----------------------------------------------------------------------------------------------------------------------------------|
| X-Bkapi-Authorization | string | 是  | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept                | string | 是  | 固定值。application/json                                                                                                             |
| Content-Type          | string | 是  | 固定值。application/json                                                                                                             |

#### Body参数

| 字段            | 类型     | 必选 | 描述                                            |
|---------------|--------|----|-----------------------------------------------|
| bk_scope_type | string | 是  | 资源范围类型。可选值：`biz`（业务）、`biz_set`（业务集）           |
| bk_scope_id   | string | 是  | 资源范围 ID，须与 `bk_scope_type` 对应，表示业务 ID 或业务集 ID |
| job_plan_id   | long   | 是  | 执行方案 ID，须大于 0，且对应方案须已存在                       |

### 请求参数示例

- POST

```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "2",
    "job_plan_id": 50001
}
```

### 返回结果示例

#### 失败示例

```json
# http status: 400
{
    "error": {
        "code": "INVALID_ARGUMENT",
        "message": "请求参数[bk_scope_type]不合法"
    }
}
```

```json
# http status: 403
{
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


#### 成功示例

```json
# http status=200
{
    "data": {
        "bk_scope_type": "biz",
        "bk_scope_id": "2",
        "job_plan_id": 50001,
        "job_template_id": 1000,
        "template_version": "b6b1f5b8d0a94f0e9a0f2c1d3e4f5a6b"
    }
}
```

### 返回结果参数说明

##### 正常响应体

| 字段   | 类型     | 是否一定存在 | 描述                           |
|------|--------|--------|------------------------------|
| data | object | 是      | 响应数据，只有在正常响应时才存在该字段，异常响应时不存在 |

##### 异常响应体

| 字段    | 类型     | 是否一定存在 | 描述                                                     |
|-------|--------|--------|--------------------------------------------------------|
| error | object | 是      | 错误信息，只有在异常响应时（HTTP状态码!=2xx）才存在该字段（权限不足、参数错误等），正常响应时不存在 |

#### error

| 字段      | 类型     | 是否一定存在 | 描述           |
|---------|--------|--------|--------------|
| code    | string | 是      | 错误码          |
| message | string | 是      | 错误信息         |
| data    | object | 否      | 错误具体内容，权限信息等 |


#### data

| 字段               | 类型     | 描述                                  |
|------------------|--------|-------------------------------------|
| bk_scope_type    | string | 资源范围类型。可选值：`biz`（业务）、`biz_set`（业务集） |
| bk_scope_id      | string | 资源范围 ID                             |
| job_plan_id      | long   | 执行方案 ID                             |
| job_template_id  | long   | 作业模板 ID                             |
| template_version | string | 实际同步到的作业模板版本号                       |
