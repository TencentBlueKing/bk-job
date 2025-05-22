### 功能描述

此组件仅用于展示回调协议文档。

对作业执行类的请求传入的回调 callback_url 地址进行回调时所传递的报文结构描述。

### 请求头
Content-Type: application/json

兼容说明：
由于老版本Job使用了Content-Type: application/x-www-form-urlencoded执行回调请求，并且部分调用方按该方式进行解析接入，当前版本Job将优先使用Content-Type: application/json执行回调，如果执行失败，将再使用Content-Type: application/x-www-form-urlencoded重试一次，来兼容已接入的调用方。新接入的调用方请按照Content-Type: application/json进行解析。

### 请求参数

| 字段   |  类型      | 必选   |  描述      |
|-----------------|------------|--------|------------|
| job_instance_id | long   | 是     | 作业实例ID |
| status          | int    | 是     | 作业状态码: 1.等待执行; 2.正在执行; 3.执行成功; 4.执行失败; 5.跳过; 6.忽略错误; 7.等待用户; 8.强制终止; 9.状态异常; 10.强制终止中; 11.强制终止成功,13.确认终止 |
| step_instance_list | array     | 是     | 步骤块中包含的各个步骤执行状态 |

#### step_instances

| 字段   |  类型      | 必选   |  描述      |
|-----------------|------------|--------|------------|
| step_instance_id | long   | 是     | 作业步骤实例ID |
| status           | int    | 是     | 作业步骤状态码: 1.等待执行; 2.正在执行; 3.执行成功; 4.执行失败; 5.跳过; 6.忽略错误; 7.等待用户; 8.强制终止; 9.状态异常; 10.强制终止中; 11.强制终止成功,13.确认终止 |

### 请求参数示例

```json
{
    "job_instance_id": 12345,
    "status": 2,
    "step_instance_list": [
        {
            "step_instance_id": 16271,
            "status": 3
        },
        {
            "step_instance_id": 16272,
            "status": 2
        }
    ]
}
```

### 回调响应

回调成功以HTTP状态为准，如果成功，则状态码200，其他表示失败。
当前版本Job将优先使用Content-Type: application/json执行回调，如果执行成功，则回调结束，如果执行失败，将再使用Content-Type: application/x-www-form-urlencoded重试一次，来兼容按照老协议进行解析的调用方。
