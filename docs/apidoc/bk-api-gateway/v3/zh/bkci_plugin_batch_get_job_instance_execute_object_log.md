### 功能描述

根据执行对象列表批量查询执行日志(蓝盾作业执行插件专用，非正式公开 API)

### 请求参数

#### Header 参数

| 字段 | 类型 | 必选 | 描述 |
|-----------|------------|--------|------------|
| X-Bkapi-Authorization | string | 是 | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept | string | 是 | 固定值。application/json |
| Content-Type | string | 是 | 固定值。application/json| 


#### Body 参数

| 字段 | 类型 | 必选 | 描述 |
|-----------|------------|--------|------------|
| bk_scope_type | string | 是 | 资源范围类型。可选值: biz - 业务，biz_set - 业务集 |
| bk_scope_id | string | 是 | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |
| job_instance_id | long | 是 | 作业实例ID |
| step_instance_id | long | 是 | 步骤实例ID |
| execute_object_list | array | 否 | 执行对象列表，定义见 exeucte_object |

##### exeucte_object

| 字段 | 类型 | 必选 | 描述 |
| ----------- | ------ | ---- | -------- |
| type | int | 是 | 执行对象类型, 1 - 主机，2 - 容器 |
| resource_id | string | 是 | 执行对象资源 ID（比如主机对应的资源 ID 为 bk_host_id) |

### 请求参数示例

- Body
```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "2",
    "job_instance_id": 100,
    "step_instance_id": 200,
    "execute_object_list": [
        {
            "type": 1,
            "resource_id": "101"
        },
        {
            "type": 2,
            "resource_id": "10002"
        }
    ]
}
```

### 返回结果示例

#### 脚本执行步骤
```json
{
    "result": true,
    "code": 0,
    "data": {
        "log_type": 1,
        "job_instance_id": 100,
        "step_instance_id": 200,
        "script_execute_object_task_logs": [
            {
                "execute_object": {
                    "type": 1,
                    "resource_id": "101",
                    "host": {
                        "bk_host_id": 101,
                        "ip": "10.0.0.1",
                        "bk_cloud_id": 0
                    }
                },
                "log_content": "[2018-03-15 14:39:30][PID:56875] job_start\n"
            },
            {
                "execute_object": {
                    "type": 2,
                    "resource_id": "10002",
                    "container": {
                        "id": 10002,
                        "container_uid": "docker://0f65a78b83d247615a696f1f8d136aa39afdc578fc8589e765ee5c3a38751858"
                    }
                },
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
    "data": {
        "log_type": 2,
        "job_instance_id": 100,
        "step_instance_id": 200,
        "file_execute_object_task_logs": [
            {
                "execute_object": {
                    "type": 1,
                    "resource_id": "101",
                    "host": {
                        "bk_host_id": 101,
                        "ip": "10.0.0.1",
                        "bk_cloud_id": 0
                    }
                },
                "file_atomic_task_logs": [
                    {
                        "mode": 1,
                        "src_execute_object": {
                            "type": 2,
                            "resource_id": "10002",
                            "container": {
                                "id": 10002,
                                "container_uid": "docker://0f65a78b83d247615a696f1f8d136aa39afdc578fc8589e765ee5c3a38751858",
                                "name": "job_test"
                            }
                        },
                        "src_path": "/data/1.log",
                        "dest_execute_object": {
                            "type": 1,
                            "resource_id": "101",
                            "host": {
                                "bk_host_id": 101,
                                "ip": "10.0.0.1",
                                "bk_cloud_id": 0
                            }
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
                "execute_object": {
                    "type": 2,
                    "resource_id": "10002",
                    "container": {
                        "id": 10002,
                        "container_uid": "docker://0f65a78b83d247615a696f1f8d136aa39afdc578fc8589e765ee5c3a38751858",
                        "name": "job_test"
                    }
                },
                "file_atomic_task_logs": [
                    {
                        "mode": 0,
                        "src_execute_object": {
                            "type": 2,
                            "resource_id": "10002",
                            "container": {
                                "id": 10002,
                                "container_uid": "docker://0f65a78b83d247615a696f1f8d136aa39afdc578fc8589e765ee5c3a38751858",
                                "name": "job_test"
                            }
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


### 返回结果说明

#### response

| 字段 | 类型 | 描述 |
|-----------|-----------|-----------|
| result       | bool   | 请求成功与否。true:请求成功；false请求失败 |
| code         | int    | 错误编码。 0表示success，>0表示失败错误 |
| message      | string | 请求失败返回的错误信息|
| data         | object | 请求返回的数据|
| permission   | object | 权限信息|

##### data

| 字段 | 类型 | 描述 |
|-----------|-----------|-----------|
| job_instance_id | long | 是 | 作业实例ID |
| step_instance_id | long | 是 | 步骤实例ID |
| log_type | int | 日志类型。1-脚本执行任务日志;2-文件分发任务日志 |
| script_execute_object_task_logs | array | 脚本执行任务日志。定义见 script_execute_object_task_log |
| file_execute_object_task_logs | array | 文件分发任务日志。定义见 file_execute_object_task_log |

##### script_execute_object_task_log

| 字段 | 类型 | 描述 |
|-----------|------------|--------|
| execute_object | object | 执行对象，见 execute_object 定义 |
| log_content | string | 脚本执行日志内容 |

##### file_execute_object_task_log

| 字段 | 类型 | 描述 |
|-----------|------------|--------|
| execute_object | object | 执行对象，见 execute_object 定义 |
| file_atomic_task_logs | array | 文件分发原子任务，指一个文件从一个源执行对象分发到一个目标执行对象的任务。定义见 file_atomic_task_log |


##### file_atomic_task_log

| 字段 | 类型 | 描述 |
|--------------|-----------|-----------|
| mode | int | 分发模式。0:上传;1:下载 |
| src_execute_object | object |源文件所在执行对象。定义见 execute_object |
| src_path | string | 源文件路径 |
| dest_execute_object | object |文件分发目标执行对象。定义见 execute_object |
| dest_path | string | 目标路径，mode=1时有值 |
| status | int | 任务状态。1-等待开始;2-上传中;3-下载中;4-成功;5-失败 |
| log_content | string | 文件分发日志内容 |
| size | string | 文件大小 |
| speed | string | 文件传输速率 |
| process | string | 文件传输进度 |

##### execute_object

| 字段 | 类型 | 描述 |
|-----------|------------|--------|
| type | int | 执行对象类型, 1 - 主机，2 - 容器 |
| resource_id | string | 执行对象资源 ID（比如主机对应的资源 ID 为 bk_host_id) |
| host | object | 主机详情，定义见 host |
| container | object | 容器详情, 定义见 container |

##### host

| 字段 | 类型 | 描述 |
|-----------|------------|--------|
| bk_host_id | long | 主机 ID (cmdb) |
| bk_cloud_id | long | 管控区域ID |
| ip | string | Ipv4 |

##### container

| 字段 | 类型 | 描述 |
|-----------|------------|--------|
| id | long | 容器 ID (cmdb) |
| container_uid | string | 容器在集群中的唯一 ID (UID) |
| name | string | 容器名称 |


