### 功能描述

根据主机列表批量查询作业执行日志

### 请求参数

{{ bkapi_authorization_description }}

#### Header参数

| 字段      |  类型      | 必选   |  描述      |
|-----------|------------|--------|------------|
| X-Bkapi-Authorization       |  string    | 是     | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept       |  string    | 是     | 固定值。application/json|
| Content-Type |  string    | 是     | 固定值。application/json|

#### Body参数

| 字段      |  类型      | 必选   |  描述      |
|-----------|------------|--------|------------|
| bk_scope_type | string | 是     | 资源范围类型。可选值: biz - 业务，biz_set - 业务集 |
| bk_scope_id | string | 是 | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |
| bk_biz_id        |  long       | 是     | *已弃用*。业务ID。此字段已被弃用并由字段bk_scope_type+bk_scope_id替换 |
| job_instance_id | long | 是 | 作业实例ID |
| step_instance_id |  long    | 是     | 步骤实例ID |
| host_id_list       | array | 否   | 主机ID列表         |
| ip_list            | array | 否   | ***不推荐使用，建议使用host_id_list参数***;如果host_id_list与ip_list同时存在，将忽略ip_list参数。主机IP 列表，定义见ip |

##### ip

| 字段        | 类型   | 必选 | 描述     |
| ----------- | ------ | ---- | -------- |
| bk_cloud_id | int    | 是   | 管控区域ID |
| ip          | string | 是   | IP地址   |

### 请求参数示例
- POST
```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "1",
    "job_instance_id": 100,
    "step_instance_id": 200,
    "host_id_list": [
        101,102
    ]
}	
```

### 返回结果示例

#### 脚本执行步骤
```json
{
    "result": true,
    "code": 0,
    "message": "",
    "data": {
        "log_type": 1,
        "job_instance_id": 100,
        "step_instance_id": 200,
        "script_task_logs": [
            {
                "bk_host_id": 101,
                "ip": "127.0.0.1",
                "bk_cloud_id": 0,
                "log_content": "[2018-03-15 14:39:30][PID:56875] job_start\n"
            },
            {
                "bk_host_id": 102,
                "ip": "127.0.0.2",
                "bk_cloud_id": 0,
                "log_content": "[2018-03-15 14:39:30][PID:16789] job_start\n"
            }
        ]
    }
}
```

#### 文件分发步骤

```json
{
    "result": true,
    "code": 0,
    "message": "",
    "data": {
        "log_type": 2,
        "job_instance_id": 100,
        "step_instance_id": 200,
        "file_task_logs": [
            {
                "bk_host_id": 101,
                "ip": "127.0.0.1",
                "bk_cloud_id": 0,
                "file_logs": [
                    {
                        "mode": 1, 
                        "src_ip": {
                            "bk_host_id": 102,
                            "bk_cloud_id": 0, 
                            "ip": "127.0.0.2"
                        }, 
                        "src_path": "/data/1.log", 
                        "dest_ip": {
                            "bk_host_id": 101,
                            "bk_cloud_id": 0, 
                            "ip": "127.0.0.1"
                        }, 
                        "dest_path": "/tmp/1.log", 
                        "status": 4,
                        "log_content": "[2021-06-28 11:32:16] FileName: /tmp/1.log FileSize: 9.0 Bytes State: dest agent success download file Speed: 1 KB/s Progress: 100% StatusDesc: dest agent success download file Detail: success",
                        "size": "1.0 Bytes", 
                        "speed": "0 KB/s", 
                        "process": "100%" 
                    }
                ]
            },
            {
                "bk_host_id": 102,
                "ip": "127.0.0.2",
                "bk_cloud_id": 0,
                "file_logs": [
                    {
                        "mode": 0, 
                        "src_ip": {
                            "bk_host_id": 102,
                            "bk_cloud_id": 0, 
                            "ip": "127.0.0.2"
                        }, 
                        "src_path": "/data/1.log",  
                        "status": 4,
                        "log_content": "[2021-06-28 11:32:16] FileName: /data/1.log FileSize: 9.0 Bytes State: source agent success upload file Speed: 1 KB/s Progress: 100% StatusDesc: source agent success upload file Detail: success upload",
                        "size": "1.0 Bytes", 
                        "speed": "0 KB/s", 
                        "process": "100%" 
                    }
                ]
            }
        ]
    }
}
```

**文件任务返回结果说明**

- 如果需要返回文件源的上传日志，需要在 host_id_list/ip_list添加源文件主机信息

### 返回结果说明

##### data

| 字段      | 类型      | 描述      |
|-----------|-----------|-----------|
| bk_host_id |  long    | 主机ID |
| bk_cloud_id   | int         | 目标服务器管控区域ID |
| ip            | string      | 目标服务器IP地址 |
| log_type   | int         | 日志类型。1-脚本执行任务日志;2-文件分发任务日志 |
| script_task_logs   | array      | 脚本执行任务日志。定义见script_task_log|
| file_task_logs   | array      | 文件分发任务日志。定义见file_task_log|

##### script_task_log

| 字段      |  类型     |  描述      |
|-----------|------------|--------|
| bk_host_id |  long    | 主机ID |
| bk_cloud_id |  long    | 管控区域ID |
| ip          |  string  | 目标IP地址 |
| log_content |  string  | 脚本执行日志内容   |

##### file_task_log

| 字段      |  类型     |  描述      |
|-----------|------------|--------|
| bk_host_id |  long    | 主机ID |
| bk_cloud_id |  long    | 管控区域ID |
| ip          |  string  | 源/目标IP地址 |
| file_logs   |  array  | 文件分发日志内容。定义见file_log |

##### file_log

| 字段          | 类型      | 描述      |
|--------------|-----------|-----------|
| mode         | 分发模式 | 0:上传;1:下载|
| src_ip       |  object |文件源主机IP。定义见ip |
| src_path     | string | 源文件路径 |
| dest_ip      | object | 分发目标主机IP，mode=1时有值。定义见ip |
| dest_path    | string | 目标路径，mode=1时有值 |
| status       | int | 任务状态。1-等待开始;2-上传中;3-下载中;4-成功;5-失败 |
| log_content  | string | 文件分发日志内容 |
| size         | string | 文件大小    |
| speed        | string | 文件传输速率 |
| process      | string | 文件传输进度 |

##### ip

| 字段      |  类型     |  描述      |
|-----------|------------|--------|
| bk_host_id |  long    | 主机ID |
| bk_cloud_id |  long    | 管控区域ID |
| ip          |  string  | IP地址   |
