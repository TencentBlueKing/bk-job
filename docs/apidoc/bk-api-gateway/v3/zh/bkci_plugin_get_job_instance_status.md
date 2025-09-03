### 功能描述

根据作业实例 ID 查询作业执行状态(蓝盾作业执行插件专用，非正式公开 API)

### 请求参数说明

#### Header参数

| 字段                    | 类型     | 必选  | 描述                                                                                                                               |
|-----------------------|--------|-----|----------------------------------------------------------------------------------------------------------------------------------|
| X-Bkapi-Authorization | string | 是   | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept                | string | 是   | 固定值。application/json                                                                                                             |
| Content-Type          | string | 是   | 固定值。application/json                                                                                                             | 

#### Body 参数

| 字段                                 | 类型      | 必选  | 描述                                                                    |
|------------------------------------|---------|-----|-----------------------------------------------------------------------|
| bk_scope_type                      | string  | 是   | 资源范围类型。可选值: biz - 业务，biz_set - 业务集                                    |
| bk_scope_id                        | string  | 是   | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID                               |
| bk_biz_id                          | long    | 是   | *已弃用*。业务ID。此字段已被弃用并由字段bk_scope_type+bk_scope_id替换                     |
| job_instance_id                    | long    | 是   | 作业实例ID                                                                |
| include_execute_object_task_result | boolean | 否   | 是否返回每个执行对象上的任务详情，对应返回结果中的 step_execute_object_result_list 。默认值为false。 |

### 请求参数示例

- Body

```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "2",
    "job_instance_id": 100,
    "include_execute_object_task_result": true
}
```

### 返回结果示例

```json
{
    "result": true,
    "code": 0,
    "data": {
        "finished": true,
        "job_instance": {
            "job_instance_id": 100,
            "bk_scope_type": "biz",
            "bk_scope_id": "1",
            "name": "API Quick execution script1521089795887",
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
                "name": "API Quick execution scriptxxx",
                "step_instance_id": 75,
                "execute_count": 0,
                "create_time": 1605064271000,
                "end_time": 1605064272000,
                "type": 1,
                "start_time": 1605064271000,
                "execute_object_result_list": [
                    {
                        "execute_object": {
                            "type": 1,
                            "resource_id": "101",
                            "host": {
                                "bk_host_id": 101,
                                "ip": "127.0.0.1",
                                "bk_cloud_id": 0
                            }
                        },
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

### 返回结果说明

#### response

| 字段             | 类型     | 是否一定存在 | 描述                         |
|----------------|--------|--------|----------------------------|
| result         | bool   | 是      | 请求成功与否。true:请求成功；false请求失败 |
| code           | int    | 是      | 错误编码。 0表示success，>0表示失败错误  |
| message        | string | 否      | 请求失败返回的错误信息                |
| data           | object | 否      | 请求返回的数据，删除操作可能没有值          |
| job_request_id | string | 否      | 请求ID，请求唯一标识                |
| permission     | object | 否      | 无权限返回的权限信息                 |

##### data

| 字段                 | 类型     | 是否一定存在 | 描述                       |
|--------------------|--------|--------|--------------------------|
| finished           | bool   | 是      | 作业是否结束                   |
| job_instance       | object | 是      | 作业实例基本信息。见job_instance定义 |
| step_instance_list | array  | 是      | 作业步骤列表。见step_instance定义  |

##### job_instance

| 字段              | 类型     | 是否一定存在 | 描述                                                                                                                            |
|-----------------|--------|--------|-------------------------------------------------------------------------------------------------------------------------------|
| name            | string | 是      | 作业实例名称                                                                                                                        |
| status          | int    | 是      | 作业状态码: 1.未执行; 2.正在执行; 3.执行成功; 4.执行失败; 5.跳过; 6.忽略错误; 7.等待用户; 8.手动结束; 9.状态异常; 10.步骤强制终止中; 11.步骤强制终止成功; 13.确认终止; 14.被丢弃; 15.滚动等待 |
| create_time     | long   | 是      | 作业创建时间，Unix时间戳，单位毫秒                                                                                                           |
| start_time      | long   | 否      | 开始执行时间，Unix时间戳，单位毫秒                                                                                                           |
| end_time        | long   | 否      | 执行结束时间，Unix时间戳，单位毫秒                                                                                                           |
| total_time      | int    | 否      | 总耗时，单位毫秒                                                                                                                      |
| bk_scope_type   | string | 是      | 资源范围类型。可选值: biz - 业务，biz_set - 业务集                                                                                            |
| bk_scope_id     | string | 是      | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID                                                                                       |
| job_instance_id | long   | 是      | 作业实例ID                                                                                                                        |

##### step_instance

| 字段                         | 类型     | 是否一定存在 | 描述                                                                                                                                            |
|----------------------------|--------|--------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| step_instance_id           | long   | 是      | 作业步骤实例ID                                                                                                                                      |
| type                       | int    | 是      | 步骤类型：1.脚本步骤; 2.文件步骤; 3.人工确认步骤; 4.SQL步骤                                                                                                        |
| name                       | string | 是      | 步骤名称                                                                                                                                          |
| status                     | int    | 是      | 作业步骤状态码: 1.等待执行; 2.正在执行; 3.执行成功; 4.执行失败; 5.跳过; 6.忽略错误; 7.等待用户; 8.手动结束; 9.状态异常; 10.步骤强制终止中; 11.步骤强制终止成功; 12.步骤强制终止失败; 13.确认终止; 14.被丢弃; 15.滚动等待 |
| create_time                | long   | 是      | 作业步骤实例创建时间，Unix时间戳，单位毫秒                                                                                                                       |
| start_time                 | long   | 否      | 开始执行时间，Unix时间戳，单位毫秒                                                                                                                           |
| end_time                   | long   | 否      | 执行结束时间，Unix时间戳，单位毫秒                                                                                                                           |
| total_time                 | int    | 否      | 总耗时，单位毫秒                                                                                                                                      |
| execute_count              | int    | 是      | 步骤重试次数                                                                                                                                        |
| execute_object_result_list | array  | 否      | 每个主机的任务执行结果，return_ip_result参数为true时才可能存在,定义见 execute_object_result                                                                           |

##### execute_object_result

| 字段             | 类型     | 是否一定存在 | 描述                                                                                                                                                                                                                                                                                                                                                                                  |
|----------------|--------|--------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| execute_object | object | 是      | 执行对象，定义见 execute_object                                                                                                                                                                                                                                                                                                                                                             |
| status         | int    | 是      | 作业执行状态:0.未知错误; 1.Agent异常; 2.无效的执行对象; 3.上次已成功; 5.等待执行; 7.正在执行; 9.执行成功; 11.执行失败; 12.任务下发失败; 13.任务超时; 15.任务日志错误; 16.GSE脚本日志超时; 17.GSE文件日志超时; 18.Agent未安装; 101.脚本执行失败; 102.脚本执行超时; 103.脚本执行被终止; 104.脚本返回码非零; 202.文件传输失败; 203.源文件不存在; 301.文件任务系统错误; 303.文件任务超时; 310.Agent异常; 311.用户不存在; 312.用户密码错误; 320.文件获取失败; 321.文件超出限制; 329.文件传输错误; 399.任务执行出错; 403.任务强制终止成功; 404.任务强制终止失败; 500.未知状态 |
| tag            | string | 否      | 用户通过job_success/job_fail函数模板自定义输出的结果。仅脚本任务存在该参数                                                                                                                                                                                                                                                                                                                                     |
| exit_code      | int    | 否      | 脚本任务exit code                                                                                                                                                                                                                                                                                                                                                                       |
| error_code     | int    | 是      | 错误码，0.已完成; 115.执行中; 126.任务强制终止; -1.其他错误类型                                                                                                                                                                                                                                                                                                                                           |
| start_time     | long   | 否      | 开始执行时间，Unix时间戳，单位毫秒                                                                                                                                                                                                                                                                                                                                                                 |
| end_time       | long   | 否      | 执行结束时间，Unix时间戳，单位毫秒                                                                                                                                                                                                                                                                                                                                                                 |
| total_time     | int    | 否      | 总耗时，单位毫秒                                                                                                                                                                                                                                                                                                                                                                            |

##### execute_object

| 字段          | 类型     | 是否一定存在 | 描述                                   |
|-------------|--------|--------|--------------------------------------|
| type        | int    | 是      | 执行对象类型, 1 - 主机，2 - 容器                |
| resource_id | string | 是      | 执行对象资源 ID（比如主机对应的资源 ID 为 bk_host_id) |
| host        | object | 否      | 主机详情，执行对象是主机时存在，定义见 host             |
| container   | object | 否      | 容器详情，执行对象是容器时存在，定义见 container        |

##### host

| 字段          | 类型     | 是否一定存在 | 描述           |
|-------------|--------|--------|--------------|
| bk_host_id  | long   | 是      | 主机 ID (cmdb) |
| bk_cloud_id | long   | 是      | 管控区域ID       |
| ip          | string | 是      | Ipv4         |

##### container

| 字段            | 类型     | 是否一定存在 | 描述                 |
|---------------|--------|--------|--------------------|
| id            | long   | 是      | 容器 ID (cmdb)       |
| container_uid | string | 是      | 容器在集群中的唯一 ID (UID) |
| name          | string | 是      | 容器名称               |
