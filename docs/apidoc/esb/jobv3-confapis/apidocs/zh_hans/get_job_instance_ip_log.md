### 功能描述

根据主机查询作业执行日志

### 请求参数

{{ common_args_desc }}

#### 接口参数

| 字段      |  类型      | 必选   |  描述      |
|-----------|------------|--------|------------|
| bk_scope_type | string | 是     | 资源范围类型。可选值: biz - 业务，biz_set - 业务集 |
| bk_scope_id | string | 是 | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |
| job_instance_id | long | 是 | 作业实例ID |
| step_instance_id |  long    | 是     | 步骤实例ID |
| bk_host_id | long | 否 | 目标主机host_id |
| bk_cloud_id | int | 否   | ***不推荐使用，建议使用host_id参数，如果存在bk_host_id将忽略该参数***。目标服务器管控区域ID |
| ip | string | 否 | ***不推荐使用，建议使用host_id参数，如果存在bk_host_id将忽略该参数***。目标主机IP |

### 请求参数示例

```json
{
    "bk_app_code": "esb_test",
    "bk_app_secret": "xxx",
    "bk_token": "xxx",
    "bk_scope_type": "biz",
    "bk_scope_id": "1",
    "job_instance_id": 50,
    "step_instance_id": 100,
    "bk_host_id": 101
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
        "bk_host_id": 101,
        "ip": "10.0.0.1",
        "bk_cloud_id": 0,
        "log_content": "[2018-03-15 14:39:30][PID:56875] job_start\n"
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
        "bk_host_id": 101,
        "ip": "10.0.0.1",
        "bk_cloud_id": 0,
        "file_logs": [
            {
                "mode": 1,
                "src_ip": {
                    "host_id": 102,
                    "bk_cloud_id": 0,
                    "ip": "10.0.0.2"
                },
                "src_path": "/data/1.log",
                "dest_ip": {
                    "bk_host_id": 101,
                    "bk_cloud_id": 0,
                    "ip": "10.0.0.1"
                },
                "dest_path": "/tmp/1.log",
                "status": 4,
                "log_content": "[2021-06-28 11:32:16] FileName: /tmp/1.log FileSize: 9.0 Bytes State: dest agent success download file Speed: 1 KB/s Progress: 100% StatusDesc: dest agent success download file Detail: success"
            },
            {
                "mode": 0,
                "src_ip": {
                    "bk_host_id": 102,
                    "bk_cloud_id": 0,
                    "ip": "10.0.0.2"
                },
                "src_path": "/data/1.log",
                "status": 4,
                "log_content": "[2021-06-28 11:32:16] FileName: /data/1.log FileSize: 9.0 Bytes State: source agent success upload file Speed: 1 KB/s Progress: 100% StatusDesc: source agent success upload file Detail: success upload"
            }
        ]
    }
}
```

**返回结果说明**

- 文件分发日志，除了目标服务器的文件下载任务日志，也会返回源服务器的文件上传任务日志(mode=0)
- dest_ip 与请求参数的bk_cloud_id/ip对应

### 返回结果参数说明

#### response
| 字段      | 类型      | 描述      |
|-----------|-----------|-----------|
| result       | bool   | 请求成功与否。true:请求成功；false请求失败 |
| code         | int    | 错误编码。 0表示success，>0表示失败错误 |
| message      | string | 请求失败返回的错误信息|
| data         | object | 请求返回的数据|
| permission   | object | 权限信息|

#### data

| 字段      | 类型      | 描述      |
|-----------|-----------|-----------|
| bk_host_id | long | 主机ID |
| bk_cloud_id   | int         | 目标服务器管控区域ID |
| ip            | string      | 目标服务器IP地址 |
| log_type   | int         | 日志类型。1-脚本执行任务日志;2-文件分发任务日志 |
| log_content   | string      | 作业脚本输出的日志内容 |
| file_logs   | array      | 文件分发任务日志。定义见file_log|

#### file_log

| 字段      | 类型      | 描述      |
|-----------|-----------|-----------|
| mode | 分发模式 | 0:上传;1:下载|
| src_ip |  object |文件源主机，定义见host |
| src_path | string | 源文件路径 |
| dest_ip | object | 分发目标主机，mode=1时有值。定义见host |
| dest_path | string | 目标路径，mode=1时有值 |
| status | int | 任务状态。1-等待开始;2-上传中;3-下载中;4-成功;5-失败 |
| log_content | string | 文件分发日志内容 |

#### Host

| 字段      |  类型     |  描述      |
|-----------|------------|--------|
| bk_host_id | long | 主机ID |
| bk_cloud_id |  long    | 管控区域ID |
| ip          |  string  | IP地址 |
