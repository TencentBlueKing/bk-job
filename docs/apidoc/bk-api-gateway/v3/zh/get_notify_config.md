### 功能描述

查询业务的通知配置状态

### 请求参数

{{ bkapi_authorization_description }}

#### Header参数

| 字段                    | 类型     | 必选 | 描述                                                                                                                               |
|-----------------------|--------|----|----------------------------------------------------------------------------------------------------------------------------------|
| X-Bkapi-Authorization | string | 是  | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept                | string | 是  | 固定值。application/json                                                                                                             |
| Content-Type          | string | 是  | 固定值。application/json                                                                                                             |


#### Query参数

| 字段            | 类型     | 必选 | 描述                                      |
|---------------|--------|----|-----------------------------------------|
| bk_scope_type | string | 是  | 资源范围类型。可选值: biz - 业务，biz_set - 业务集      |
| bk_scope_id   | string | 是  | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |

### 请求参数示例

- GET

```json
/api/v3/get_notify_config?bk_scope_type=biz&bk_scope_id=2
```

### 返回结果示例

```json
{
    "code": 0,
    "result": true,
    "data": [
        {
            "trigger_type": "PAGE_EXECUTE",
            "resource_type_list": [
                "SCRIPT",
                "JOB",
                "FILE"
            ],
            "role_list": [
                "JOB_RESOURCE_TRIGGER_USER"
            ],
            "extra_observer_list": [],
            "resource_status_channel_map": {
                "READY": {
                    "voice": false,
                    "weixin": false,
                    "mail": false,
                    "rtx": false,
                    "sms": false
                },
                "SUCCESS": {
                    "voice": false,
                    "weixin": false,
                    "mail": false,
                    "rtx": false,
                    "sms": false
                },
                "FAIL": {
                    "voice": false,
                    "weixin": false,
                    "mail": true,
                    "rtx": false,
                    "sms": false
                }
            }
        },
        {
            "trigger_type": "API_INVOKE",
            "resource_type_list": [
                "SCRIPT",
                "JOB",
                "FILE"
            ],
            "role_list": [
                "JOB_RESOURCE_TRIGGER_USER"
            ],
            "extra_observer_list": [],
            "resource_status_channel_map": {
                "READY": {
                    "voice": false,
                    "weixin": false,
                    "mail": false,
                    "rtx": false,
                    "sms": false
                },
                "SUCCESS": {
                    "voice": false,
                    "weixin": false,
                    "mail": false,
                    "rtx": false,
                    "sms": false
                },
                "FAIL": {
                    "voice": false,
                    "weixin": false,
                    "mail": true,
                    "rtx": false,
                    "sms": false
                }
            }
        },
        {
            "trigger_type": "TIMER_TASK",
            "resource_type_list": [
                "SCRIPT",
                "FILE"
            ],
            "role_list": [
                "JOB_EXTRA_OBSERVER",
                "JOB_RESOURCE_TRIGGER_USER"
            ],
            "extra_observer_list": [
                "zhangsan"
            ],
            "resource_status_channel_map": {
                "READY": {
                    "voice": false,
                    "weixin": false,
                    "mail": false,
                    "rtx": false,
                    "sms": false
                },
                "SUCCESS": {
                    "voice": false,
                    "weixin": false,
                    "mail": false,
                    "rtx": true,
                    "sms": false
                },
                "FAIL": {
                    "voice": false,
                    "weixin": false,
                    "mail": true,
                    "rtx": true,
                    "sms": false
                }
            }
        }
    ]
}
```

### 返回结果说明

#### response

| 字段             | 类型     | 是否一定不为null | 描述                         |
|----------------|--------|------------|----------------------------|
| result         | bool   | 是          | 请求成功与否。true:请求成功；false请求失败 |
| code           | int    | 是          | 错误编码。 0表示success，>0表示失败错误  |
| message        | string | 否          | 请求失败返回的错误信息                |
| data           | object | 否          | 请求返回的数据，删除操作可能没有值          |
| job_request_id | string | 否          | 请求ID，请求唯一标识                |
| permission     | object | 否          | 无权限返回的权限信息                 |

##### data

| 字段                          | 类型                                | 是否一定不为null | 描述                                          |
|-----------------------------|-----------------------------------|------------|---------------------------------------------|
| trigger_type                | string                            | 是          | 触发方式                                        |
| resource_type_list          | array                             | 是          | 操作（任务）类型列表                                  |
| role_list                   | array                             | 是          | 任务角色（通知对象）列表                                |
| extra_observer_list         | array                             | 是          | 额外通知人列表，用户名列表                               |
| resource_status_channel_map | Map<string, Map<string, boolean>> | 是          | 状态通知渠道映射。第一层key为执行状态，第二层key为渠道类型，value为是否启用 |

##### trigger_type 值说明

- `TIMER_TASK`：定时任务
- `PAGE_EXECUTE`：页面执行
- `API_INVOKE`：API调用

##### resource_type_list 值说明

- `SCRIPT`：脚本执行
- `JOB`：执行方案
- `FILE`：文件分发

##### role_list 值说明

- `JOB_RESOURCE_TRIGGER_USER`：任务执行人
- `JOB_RESOURCE_OWNER`：资源所属者
- `JOB_EXTRA_OBSERVER`：额外通知人
- `bk_biz_maintainer`：运维人员
- `bk_biz_productor`：产品人员
- `bk_biz_tester`：测试人员
- `bk_biz_developer`：开发人员
- `operator`：操作人员

##### resource_status_channel_map 说明

第一层 key（执行状态）：
- `SUCCESS`：执行成功
- `FAIL`：执行失败
- `READY`：准备执行

第二层 key（渠道类型）：
- `sms`：短信
- `mail`：邮件
- `weixin`：微信
- `voice`：语音
- `rtx`：RTX

value：布尔值，true 表示该状态下该渠道启用，false 表示未启用
