### 功能描述

根据作业实例 ID 查询作业执行状态

### 请求参数

#### Header参数

| 字段                    |  类型      | 必选   |  描述      |
|-----------------------|------------|--------|------------|
| X-Bkapi-Authorization |  string    | 是     | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept                |  string    | 是     | 固定值。application/json|
| Content-Type          |  string    | 是     | 固定值。application/json|

#### Query参数

| 字段               | 类型      | 必选 | 描述                                                   |
|------------------|---------|----|------------------------------------------------------|
| bk_scope_type    | string  | 是  | 资源范围类型。可选值: biz - 业务，biz_set - 业务集                   |
| bk_scope_id      | string  | 是  | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID              |
| job_instance_id  | long    | 是  | 作业实例ID                                               |
| return_ip_result | boolean | 否  | 是否返回每个ip上的任务详情，对应返回结果中的step_ip_result_list。默认为false。 |

### 请求参数示例

- GET

```json
/api/v4/get_job_instance_status?bk_scope_type=biz&bk_scope_id=1&job_instance_id=100&return_ip_result=true
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
        "finished": true,
        "job_instance": {
            "job_instance_id": 100,
            "bk_scope_type": "biz",
            "bk_scope_id": "1",
            "name": "API Quick execution script1",
            "create_time": 1605064271000,
            "status": 4,
            "start_time": 1605064271000,
            "end_time": 1605064272000,
            "total_time": 1000
        },
        "step_instance_list": [
            {
                "status": 4,
                "total_time": 1000,
                "name": "API Quick execution script1",
                "step_instance_id": 75,
                "execute_count": 0,
                "create_time": 1605064271000,
                "end_time": 1605064272000,
                "type": 1,
                "start_time": 1605064271000,
                "step_ip_result_list": [
                    {
                        "bk_host_id": 101,
                        "ip": "127.0.0.1",
                        "bk_cloud_id": 0,
                        "status": 9,
                        "tag": "",
                        "exit_code": 0,
                        "error_code": 0,
                        "start_time": 1605064271000,
                        "end_time": 1605064272000,
                        "total_time": 1000
                    }
                ]
            }
        ]
    },
    "job_request_id": "xxx"
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

| 字段                 | 类型     | 是否一定不为null | 描述                       |
|--------------------|--------|------------|--------------------------|
| finished           | bool   | 是          | 作业是否结束                   |
| job_instance       | object | 是          | 作业实例基本信息。见job_instance定义 |
| step_instance_list | array  | 是          | 作业步骤列表。见step_instance定义  |

#### job_instance

| 字段              | 类型     | 是否一定不为null | 描述                                      |
|-----------------|--------|------------|-----------------------------------------|
| name            | string | 是          | 作业实例名称                                  |
| status          | int    | 是          | 作业状态码，见 run_status 定义                   |
| create_time     | long   | 是          | 作业创建时间，Unix时间戳，单位毫秒                     |
| start_time      | long   | 否          | 开始执行时间，Unix时间戳，单位毫秒                     |
| end_time        | long   | 否          | 执行结束时间，Unix时间戳，单位毫秒                     |
| total_time      | int    | 否          | 总耗时，单位毫秒                                |
| bk_scope_type   | string | 是          | 资源范围类型。可选值: biz - 业务，biz_set - 业务集      |
| bk_scope_id     | string | 是          | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |
| job_instance_id | long   | 是          | 作业实例ID                                  |

#### step_instance

| 字段                  | 类型     | 是否一定不为null | 描述                                                          |
|---------------------|--------|------------|-------------------------------------------------------------|
| step_instance_id    | long   | 是          | 作业步骤实例ID                                                    |
| type                | int    | 是          | 步骤类型：1.脚本步骤; 2.文件步骤; 3.人工确认步骤; 4.SQL步骤                      |
| name                | string | 是          | 步骤名称                                                        |
| status              | int    | 是          | 作业步骤状态码，见 run_status 定义                                     |
| create_time         | long   | 是          | 作业步骤实例创建时间，Unix时间戳，单位毫秒                                     |
| start_time          | long   | 是          | 开始执行时间，Unix时间戳，单位毫秒                                         |
| end_time            | long   | 是          | 执行结束时间，Unix时间戳，单位毫秒                                         |
| total_time          | int    | 是          | 总耗时，单位毫秒                                                    |
| execute_count       | int    | 是          | 步骤重试次数                                                      |
| step_ip_result_list | array  | 否          | 每个主机的任务执行结果，return_ip_result参数为true时才可能存在，定义见step_ip_result |

#### step_ip_result

| 字段          | 类型     | 是否一定不为null | 描述                                              |
|-------------|--------|------------|-------------------------------------------------|
| bk_host_id  | long   | 是          | 主机ID                                            |
| ip          | string | 是          | IP                                              |
| bk_cloud_id | long   | 是          | 管控区域ID                                          |
| status      | int    | 是          | 该主机上任务状态，见target_object_status定义                |
| tag         | string | 否          | 用户通过job_success/job_fail函数模板自定义输出的结果。仅脚本任务存在该参数 |
| exit_code   | int    | 否          | 脚本任务exit code，脚本任务时非空                           |
| start_time  | long   | 是          | 开始执行时间，Unix时间戳，单位毫秒                             |
| end_time    | long   | 是          | 执行结束时间，Unix时间戳，单位毫秒                             |
| total_time  | int    | 是          | 总耗时，单位毫秒                                        |

#### run_status

| 状态码 | 描述     |
|-----|--------|
| 1   | 等待执行   |
| 2   | 正在执行   |
| 3   | 执行成功   |
| 4   | 执行失败   |
| 5   | 跳过     |
| 6   | 忽略错误   |
| 7   | 等待用户   |
| 8   | 手动结束   |
| 9   | 状态异常   |
| 10  | 强制终止中  |
| 11  | 强制终止成功 |
| 13  | 确认终止   |
| 14  | 被丢弃    |
| 15  | 滚动等待   |

#### target_object_status

| 状态码 | 描述           |
|-----|--------------|
| 0   | 未知错误         |
| 1   | Agent异常      |
| 2   | 无效执行对象       |
| 3   | 上次已成功        |
| 5   | 等待执行         |
| 7   | 正在执行         |
| 9   | 执行成功         |
| 11  | 执行失败         |
| 12  | 任务下发失败       |
| 13  | 任务超时         |
| 15  | 任务日志错误       |
| 16  | GSE脚本日志超时    |
| 17  | GSE文件日志超时    |
| 18  | Agent未安装     |
| 101 | 脚本执行失败       |
| 102 | 脚本执行超时       |
| 103 | 脚本执行被终止      |
| 104 | 脚本返回码非零      |
| 202 | 文件传输失败       |
| 203 | 源文件不存在       |
| 301 | 文件任务系统错误-未分类 |
| 303 | 文件任务超时       |
| 310 | Agent异常      |
| 311 | 用户名不存在       |
| 312 | 用户密码错误       |
| 320 | 文件获取失败       |
| 321 | 文件超出限制       |
| 329 | 文件传输错误       |
| 399 | 任务执行出错       |
| 403 | GSE任务强制终止成功  |
| 404 | GSE任务强制终止失败  |
| 500 | 未知状态         |
