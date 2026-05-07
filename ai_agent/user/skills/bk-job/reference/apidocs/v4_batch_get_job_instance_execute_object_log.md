### 功能描述

根据作业实例 ID 批量查询主机上的作业执行日志

### 请求参数

#### Header参数

| 字段                    | 类型     | 必选 | 描述                                                                                                                               |
|-----------------------|--------|----|----------------------------------------------------------------------------------------------------------------------------------|
| X-Bkapi-Authorization | string | 是  | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept                | string | 是  | 固定值。application/json                                                                                                             |
| Content-Type          | string | 是  | 固定值。application/json                                                                                                             |

#### Body参数

| 字段                | 类型     | 必选 | 描述                                                                                |
|-------------------|--------|----|-----------------------------------------------------------------------------------|
| bk_scope_type     | string | 是  | 资源范围类型。可选值: biz - 业务，biz_set - 业务集                                                |
| bk_scope_id       | string | 是  | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID                                           |
| job_instance_id   | long   | 是  | 作业实例ID                                                                            |
| step_instance_id  | long   | 是  | 步骤实例ID                                                                            |
| host_id_list      | array  | 否  | 主机ID列表，最多50个。每个元素为long。优先级host_id_list > ip_list > container_id_list              |
| ip_list           | array  | 否  | 主机IP列表，最多50个。每个元素定义详见cloud_ip。优先级host_id_list > ip_list > container_id_list       |
| container_id_list | array  | 否  | 容器ID列表(BKCC中的容器UID)，最多50个。每个元素为long。优先级host_id_list > ip_list > container_id_list |

#### cloud_ip

| 字段          | 类型     | 必选 | 描述     |
|-------------|--------|----|--------|
| bk_cloud_id | long   | 是  | 管控区域ID |
| ip          | string | 是  | 主机IP   |

### 请求参数示例

- POST

查询主机日志：
```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "1",
    "job_instance_id": 100,
    "step_instance_id": 200,
    "host_id_list": [
        1, 2
    ]
}
```

查询容器日志：
```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "1",
    "job_instance_id": 100,
    "step_instance_id": 200,
    "container_id_list": [
        1001, 1002
    ]
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

步骤为脚本执行步骤（主机）

```json
# http status=200
{
    "data": {
        "job_instance_id": 100,
        "step_instance_id": 200,
        "log_type": 1,
        "script_step_execute_object_logs": [
            {
                "execute_object": {
                    "type": 1,
                    "resource_id": "1",
                    "host": {
                        "bk_host_id": 1,
                        "ip": "127.0.0.1",
                        "bk_cloud_id": 0
                    }
                },
                "log_content": "[2018-03-15 14:39:30][PID:56875] job_start\n"
            },
            {
                "execute_object": {
                    "type": 1,
                    "resource_id": "2",
                    "host": {
                        "bk_host_id": 2,
                        "ip": "127.0.0.2",
                        "bk_cloud_id": 0
                    }
                },
                "log_content": "[2018-03-15 14:39:30][PID:16789] job_start\n"
            }
        ]
    }
}
```

步骤为脚本执行步骤（容器）

```json
# http status=200
{
    "data": {
        "job_instance_id": 100,
        "step_instance_id": 200,
        "log_type": 1,
        "script_step_execute_object_logs": [
            {
                "execute_object": {
                    "type": 2,
                    "resource_id": "1001",
                    "container": {
                        "id": 1001,
                        "container_id": "abc123def456",
                        "name": "nginx-container",
                        "node_host_id": 10
                    }
                },
                "log_content": "[2018-03-15 14:39:30][PID:56875] job_start\n"
            }
        ]
    }
}
```

步骤为文件分发步骤

```json
# http status=200
{
    "data": {
        "job_instance_id": 100,
        "step_instance_id": 200,
        "log_type": 2,
        "file_step_execute_object_logs": [
            {
                "execute_object": {
                    "type": 1,
                    "resource_id": "1",
                    "host": {
                        "bk_host_id": 1,
                        "ip": "127.0.0.1",
                        "bk_cloud_id": 0
                    }
                },
                "download_log": [
                    {
                        "mode": 1,
                        "src_execute_object": {
                            "type": 1,
                            "resource_id": "2",
                            "host": {
                                "bk_host_id": 2,
                                "bk_cloud_id": 0,
                                "ip": "127.0.0.2"
                            }
                        },
                        "src_path": "/data/1.log",
                        "dest_execute_object": {
                            "type": 1,
                            "resource_id": "1",
                            "host": {
                                "bk_host_id": 1,
                                "bk_cloud_id": 0,
                                "ip": "127.0.0.1"
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
                    "type": 1,
                    "resource_id": "2",
                    "host": {
                        "bk_host_id": 2,
                        "ip": "127.0.0.2",
                        "bk_cloud_id": 0
                    }
                },
                "upload_log": [
                    {
                        "mode": 0,
                        "src_execute_object": {
                            "type": 1,
                            "resource_id": "2",
                            "host": {
                                "bk_host_id": 2,
                                "bk_cloud_id": 0,
                                "ip": "127.0.0.2"
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

| 字段                              | 类型    | 是否一定存在 | 描述                                         |
|---------------------------------|-------|--------|--------------------------------------------|
| job_instance_id                 | long  | 是      | 作业实例ID                                     |
| step_instance_id                | long  | 是      | 步骤实例ID                                     |
| log_type                        | int   | 是      | 日志类型。1-脚本执行任务日志;2-文件分发任务日志                 |
| script_step_execute_object_logs | array | 否      | 脚本执行任务日志。定义见script_step_execute_object_log |
| file_step_execute_object_logs   | array | 否      | 文件分发任务日志。定义见file_step_execute_object_log   |

#### script_step_execute_object_log

| 字段             | 类型     | 是否一定存在 | 描述                     |
|----------------|--------|--------|------------------------|
| execute_object | object | 是      | 执行对象，见execute_object定义 |
| log_content    | string | 是      | 日志内容                   |

#### file_step_execute_object_log

| 字段             | 类型     | 是否一定存在 | 描述                                    |
|----------------|--------|--------|---------------------------------------|
| execute_object | object | 是      | 执行对象，见execute_object定义                |
| upload_log     | array  | 否      | 当执行对象是分发任务的源时，上传的日志。定义见file_task_log  |
| download_log   | array  | 否      | 当执行对象是分发任务的目标时，下载的日志。定义见file_task_log |

#### file_task_log

| 字段                  | 类型     | 是否一定存在 | 描述                                              |
|---------------------|--------|--------|-------------------------------------------------|
| mode                | int    | 是      | 文件分发模式。0-上传，1-下载                                |
| src_execute_object  | object | 是      | 源执行对象（主机/容器）。定义见execute_object                  |
| src_path            | string | 是      | 文件源路径                                           |
| dest_execute_object | object | 否      | 目标执行对象（主机/容器）。定义见execute_object。仅在mode=1时存在     |
| dest_path           | string | 否      | 文件目标路径。仅在mode=1时存在                              |
| status              | int    | 是      | 文件传输任务状态。0-从文件源拉取中，1-等待开始，2-上传中，3-下载中，4-完成，5-失败 |
| log_content         | string | 是      | 文件传输日志内容                                        |
| size                | string | 是      | 文件大小                                            |
| speed               | string | 是      | 上传/下载速度                                         |
| process             | string | 是      | 上传/下载进度                                         |

#### execute_object

| 字段          | 类型     | 是否一定存在 | 描述                                   |
|-------------|--------|--------|--------------------------------------|
| type        | int    | 是      | 执行对象类型, 1 - 主机，2 - 容器                |
| resource_id | string | 是      | 执行对象资源 ID（比如主机对应的资源 ID 为 bk_host_id) |
| host        | object | 否      | 主机详情，执行对象是主机时存在，定义见 host             |
| container   | object | 否      | 容器详情，执行对象是容器时存在，定义见 container        |

#### host

| 字段          | 类型     | 是否一定存在 | 描述     |
|-------------|--------|--------|--------|
| bk_host_id  | long   | 否      | 主机ID。  |
| bk_cloud_id | long   | 否      | 云区域ID。 |
| ip          | string | 否      | IP地址。  |

#### container

| 字段           | 类型     | 是否一定存在 | 描述                |
|--------------|--------|--------|-------------------|
| id           | long   | 是      | 容器 ID (cmdb)      |
| container_id | string | 是      | 容器ID（如docker容器ID） |
| name         | string | 是      | 容器名称              |
| node_host_id | long   | 否      | 容器所在节点主机ID        |
