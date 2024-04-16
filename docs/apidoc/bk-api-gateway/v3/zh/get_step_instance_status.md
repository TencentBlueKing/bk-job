### 功能描述

根据步骤实例 ID 查询步骤执行状态

### 请求参数说明

{{ bkapi_authorization_description }}

#### Header参数

| 字段      |  类型      | 必选   |  描述      |
|-----------|------------|--------|------------|
| X-Bkapi-Authorization       |  string    | 是     | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept       |  string    | 是     | 固定值。application/json|
| Content-Type |  string    | 是     | 固定值。application/json|

#### Query参数

| 字段                    |  类型      | 必选   | 描述                                                                                                                                                                                                                                                                                                                                                                                                             |
|------------------------|-----------|-------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| bk_scope_type          | string    | 是     | 资源范围类型。可选值: biz - 业务，biz_set - 业务集                                                                                                                                                                                                                                                                                                                                                                             |
| bk_scope_id            | string    | 是     | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID                                                                                                                                                                                                                                                                                                                                                                        |
| job_instance_id        | long      | 是     | 作业实例ID                                                                                                                                                                                                                                                                                                                                                                                                         |
| step_instance_id       | long      | 是     | 步骤实例ID                                                                                                                                                                                                                                                                                                                                                                                                         |
| execute_count          | int       | 否     | 步骤重试次数，从0开始计数，不传表示获取最近一次重试的数据。                                                                                                                                                                                                                                                                                                                                                                                 |
| batch                  | int       | 否     | 滚动批次，从0开始计数，默认值为null表示获取所有批次的数据。                                                                                                                                                                                                                                                                                                                                                                               |
| max_host_num_per_group | int       | 否     | 每个分组（按照status与tag进行分组）里的最大主机数量，不传则返回全量数据。                                                                                                                                                                                                                                                                                                                                                                      |
| keyword                | string    | 否     | 日志搜索关键字，只返回日志中包含该关键字的主机执行状态数据（注意：仅脚本步骤支持）                                                                                                                                                                                                                                                                                                                                                                      |
| search_ip              | string    | 否     | 主机IP/IPv6搜索关键字，只返回主机IP/IPv6地址包含该关键字的主机执行状态数据                                                                                                                                                                                                                                                                                                                                                                   |
| status                 | int       | 否     | 执行状态，对应返回值中的status字段，传入后则只有执行状态匹配分组下才返回主机执行状态数据，其余分组下只返回主机数量等基本分组信息。可能的取值：0-未知错误，1-Agent异常，2-无效主机，3-上次已成功，5-等待执行，7-正在执行，9-执行成功，11-执行失败，12-任务下发失败，13-任务超时，15-任务日志错误，16-GSE脚本日志超时，17-GSE文件日志超时，101-脚本执行失败，102-脚本执行超时，103-脚本执行被终止，104-脚本返回码非零，202-文件传输失败，203-源文件不存在，301-文件任务系统错误-未分类的，303-文件任务超时，310-Agent异常，311-用户名不存在，312-用户密码错误，320-文件获取失败，321-文件超出限制，329-文件传输错误，399-任务执行出错，403-任务强制终止成功，404-任务强制终止失败，500-未知状态 |
| tag                    | string    | 否     | 结果标签，对应返回值中的tag字段                                                                                                                                                                                                                                                                                                                                                                                              |

### 请求参数示例

- GET
```json
/api/v3/get_step_instance_status?bk_scope_type=biz&bk_scope_id=1&job_instance_id=100&step_instance_id=100&execute_count=0&batch=0&max_host_num_per_group=1&keyword=aaa&search_ip=&status=9&tag=
```

### 返回结果示例

```json
{
    "result": true,
    "code": 0,
    "message": "",
    "data": {
        "step_instance_id": 75,
        "execute_count": 0,
        "name": "API Quick execution scriptxxx",
        "type": 1,
        "status": 4,
        "create_time": 1605064271000,
        "start_time": 1605064271000,
        "end_time": 1605064272000,
        "total_time": 1000,
        "step_result_group_list": [
            {
                "result_type": 9,
                "result_type_desc": "执行成功",
                "tag": "tag1",
                "host_size": 2,
                "host_result_list": [
                    {
                        "bk_host_id": 101,
                        "ip": "127.0.0.1",
                        "ipv6": null,
                        "bk_agent_id": null,
                        "bk_cloud_id": 0,
                        "bk_cloud_name": "Default Area",
                        "status": 9,
                        "status_desc": "执行成功",
                        "tag": "tag1",
                        "exit_code": 0,
                        "start_time": 1605064271000,
                        "end_time": 1605064272000,
                        "total_time": 1000
                    }
                ]
            },
            {
                "result_type": 9,
                "result_type_desc": "执行成功",
                "tag": "tag2",
                "host_size": 2,
                "host_result_list": [
                    {
                        "bk_host_id": 102,
                        "ip": "127.0.0.2",
                        "ipv6": null,
                        "bk_agent_id": null,
                        "bk_cloud_id": 0,
                        "bk_cloud_name": "Default Area",
                        "status": 9,
                        "status_desc": "执行成功",
                        "tag": "tag2",
                        "exit_code": 0,
                        "start_time": 1605064271000,
                        "end_time": 1605064272000,
                        "total_time": 1000
                    }
                ]
            }
        ]
    }
}
```
### 返回结果说明

| 字段          | 类型    | 是否一定存在 | 描述      |
|--------------|--------|------------|-----------|
| result       | bool   |  是        | 请求成功与否。true:请求成功；false请求失败 |
| code         | int    |  是        | 错误编码。 0表示success，>0表示失败错误 |
| message      | string |  是        | 请求失败返回的错误信息|
| data         | object |  否        | 请求返回的数据|
| permission   | object |  否        | 权限信息|

##### data

| 字段                    | 类型                      | 是否一定存在 | 描述      |
|------------------------|--------------------------|------------|-----------|
| step_instance_id       | long                     |  是        | 作业步骤实例ID |
| execute_count          | int                      |  是        | 步骤重试次数 |
| name                   | string                   |  是        | 步骤名称 |
| type                   | int                      |  是        | 步骤类型：1-脚本步骤；2-文件步骤；4-SQL步骤 |
| status                 | int                      |  是        | 作业步骤状态码: 1-未执行，2-正在执行，3-执行成功，4-执行失败，5-跳过，6-忽略错误，7-等待用户，8-手动结束，9-状态异常，10-步骤强制终止中，11-步骤强制终止成功，12-步骤强制终止失败  |
| create_time            | long                     |  是        | 作业步骤实例创建时间，Unix时间戳，单位毫秒 |
| start_time             | long                     |  是        | 开始执行时间，Unix时间戳，单位毫秒 |
| end_time               | long                     |  是        | 执行结束时间，Unix时间戳，单位毫秒 |
| total_time             | int                      |  是        | 总耗时，单位毫秒 |
| step_result_group_list | list<step_result_group>  |  是        | 任务执行结果分组列表，元素定义见step_result_group |


##### step_result_group

| 字段                | 类型                | 是否一定存在 | 描述        |
|--------------------|--------------------|------------|-------------|
| result_type        | int                |  是        | 分组类型      |
| result_type_desc   | string             |  是        | 分组类型描述   |
| tag                | string             |  是        | 分组标签      |
| host_size          | int                |  是        | 分组内主机总量  |
| host_result_list   | list<host_result>  |  是        | 每个分组内的主机任务执行结果列表，元素定义见host_result     |


##### host_result

| 字段                | 类型       | 是否一定存在 | 描述      |
|--------------------|-----------|------------|-----------|
| bk_host_id         | long      |  是        | 主机ID     |
| ip                 | string    |  否        | IP        |
| ipv6               | string    |  否        | IPv6      |
| bk_agent_id        | string    |  否        | AgentId   |
| bk_cloud_id        | long      |  否        | 管控区域ID  |
| bk_cloud_name      | string    |  否        | 管控区域名称 |
| status             | int       |  是        | 任务状态：0-未知错误，1-Agent异常，2-无效主机，3-上次已成功，5-等待执行，7-正在执行，9-执行成功，11-执行失败，12-任务下发失败，13-任务超时，15-任务日志错误，16-GSE脚本日志超时，17-GSE文件日志超时，101-脚本执行失败，102-脚本执行超时，103-脚本执行被终止，104-脚本返回码非零，202-文件传输失败，203-源文件不存在，301-文件任务系统错误-未分类的，303-文件任务超时，310-Agent异常，311-用户名不存在，312-用户密码错误，320-文件获取失败，321-文件超出限制，329-文件传输错误，399-任务执行出错，403-任务强制终止成功，404-任务强制终止失败，500-未知状态 |
| status_desc        | string    |  是        | 任务状态描述 |
| tag                | string    |  否        | 用户通过job_success/job_fail函数模板自定义输出的结果。仅脚本任务存在该参数 |
| exit_code          | int       |  否        | 脚本任务exit code |
| start_time         | long      |  是        | 开始执行时间，Unix时间戳，单位毫秒 |
| end_time           | long      |  是        | 执行结束时间，Unix时间戳，单位毫秒 |
| total_time         | int       |  是        | 总耗时，单位毫秒 |
