### 功能描述

查询任务执行历史

### 请求参数

#### Header参数

| 字段                    |  类型      | 必选   |  描述      |
|-----------------------|------------|--------|------------|
| X-Bkapi-Authorization |  string    | 是     | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept                |  string    | 是     | 固定值。application/json|
| Content-Type          |  string    | 是     | 固定值。application/json|

#### Query参数

| 字段                | 类型     | 必选 | 描述                                      |
|-------------------|--------|----|-----------------------------------------|
| bk_scope_type     | string | 是  | 资源范围类型。可选值: biz - 业务，biz_set - 业务集      |
| bk_scope_id       | string | 是  | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |
| create_time_start | long   | 是  | 创建起始时间，Unix 时间戳，单位毫秒                    |
| create_time_end   | long   | 是  | 创建结束时间，Unix 时间戳，单位毫秒                    |
| job_instance_id   | long   | 否  | 任务实例ID。 如果传入job_instance_id，将忽略其他查询条件   |
| job_cron_id       | long   | 否  | 定时任务ID                                  |
| operator          | string | 否  | 执行人，精准匹配                                |
| name              | string | 否  | 任务名称，模糊匹配                               |
| launch_mode       | int    | 否  | 执行方式。1 - 页面执行，2 - API调用，3 - 定时执行        |
| type              | int    | 否  | 任务类型。0 - 作业执行，1 - 脚本执行，2 - 文件分发         |
| status            | int    | 否  | 任务状态。详见文末task_status描述                  |
| ip                | string | 否  | 执行目标服务器IP, 精准匹配                         |
| offset            | int    | 否  | 从第几条数据开始往前查，最大为10000，默认为0               |
| length            | int    | 否  | 单次返回最大记录数，最大200，不传默认为10                 |

### 请求参数示例

- GET

```json
/api/v4/get_job_instance_list?bk_scope_type=biz&bk_scope_id=1&type=0&launch_mode=1&status=3&operator=admin&name=test&create_time_start=1546272000000&create_time_end=1577807999999&offset=40&length=20
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
    "job_instance_list": [
      {
        "bk_scope_type": "biz",
        "bk_scope_id": "1",
        "id": 102,
        "job_template_id": 1,
        "job_plan_id": 1,
        "name": "test",
        "operator": "admin",
        "create_time": 1546272000000,
        "start_time": 1546272000000,
        "end_time": 1546272001000,
        "total_time": 1000,
        "launch_mode": 1,
        "task_status": 3,
        "task_type": 0
      },
      {
        "bk_scope_type": "biz",
        "bk_scope_id": "1",
        "id": 101,
        "job_template_id": 1,
        "job_plan_id": 1,
        "name": "test",
        "operator": "admin",
        "create_time": 1546272000000,
        "start_time": 1546272000000,
        "end_time": 1546272001000,
        "total_time": 1000,
        "launch_mode": 1,
        "task_status": 3,
        "task_type": 0
      }
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

| 字段                | 类型    | 是否一定存在 | 描述                  |
|-------------------|-------|--------|---------------------|
| job_instance_list | array | 是      | 任务执行历史列表，按从新到老的顺序排序 |

#### job_instance

| 字段              | 类型     | 是否一定存在 | 描述                                      |
|-----------------|--------|--------|-----------------------------------------|
| bk_scope_type   | string | 是      | 资源范围类型。可选值: biz - 业务，biz_set - 业务集      |
| bk_scope_id     | string | 是      | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |
| id              | long   | 是      | 执行方案 ID                                 |
| job_template_id | long   | 否      | 作业模版 ID，当任务为执行方案的时候有值                   |
| job_plan_id     | long   | 否      | 作业执行方案 ID，当任务为执行方案的时候有值                 |
| name            | string | 是      | 任务名称                                    |
| operator        | string | 是      | 操作者                                     |
| create_time     | long   | 是      | 创建时间，Unix 时间戳，单位毫秒                      |
| status          | int    | 是      | 任务状态。详见文末 task_status 描述                |
| type            | int    | 是      | 任务类型。0 - 作业执行，1 - 脚本执行，2 - 文件分发         |
| launch_mode     | int    | 是      | 执行方式。1 - 页面执行，2 - API调用，3 - 定时执行        |
| start_time      | long   | 是      | 任务启动时间，Unix 时间戳，单位毫秒                    |
| end_time        | long   | 是      | 任务结束时间，Unix 时间戳，单位毫秒                    |
| total_time      | long   | 是      | 任务执行时间，Unix 时间戳，单位毫秒                    |

#### task_status

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
