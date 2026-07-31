### 功能描述

快速分发文件：将源文件分发到目标机器的指定目录。变更类/生产执行操作，真实调用前须过写操作确认门禁。

分发**本地文件**需先调用 `generate_local_file_upload_url` 获取上传地址与分发路径，将文件上传后再以 `file_type=2` 的源文件引用该路径，详见 [`generate_local_file_upload_url.md`](generate_local_file_upload_url.md)。

> **技能暂仅支持服务器文件（file_type=1）与本地文件（file_type=2）两种源文件类型。** 接口本身还支持第三方文件源（`file_type=3`，如 COS，用 `file_source_id`/`file_source_code`），但相关接口目前尚未提供，技能**暂不支持**、脚本会拒绝，下表中相应字段仅作接口参考，请勿使用。

### 请求参数

{{ bkapi_authorization_description }}

#### Header参数

| 字段                    | 类型     | 必选 | 描述                                                                                                                               |
|-----------------------|--------|----|----------------------------------------------------------------------------------------------------------------------------------|
| X-Bkapi-Authorization | string | 是  | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept                | string | 是  | 固定值。application/json                                                                                                             |
| Content-Type          | string | 是  | 固定值。application/json                                                                                                             |

#### Body参数

| 字段                   | 类型     | 必选 | 描述                                                                                                |
|----------------------|--------|----|---------------------------------------------------------------------------------------------------|
| bk_scope_type        | string | 是  | 资源范围类型。可选值：`biz`（业务）、`biz_set`（业务集）                                                                |
| bk_scope_id          | string | 是  | 资源范围 ID，与 `bk_scope_type` 对应，表示业务 ID 或业务集 ID                                                        |
| task_name            | string | 否  | 自定义作业名称，长度不可超过 512 字符                                                                              |
| account_alias        | string | 否  | 目标执行账号别名，推荐使用。与 `account_id` 至少存在一个；同时存在时 `account_id` 优先                                          |
| account_id           | long   | 否  | 目标执行账号 ID，可从 `get_account_list` 获取。与 `account_alias` 至少存在一个；同时存在时 `account_id` 优先                 |
| file_target_path     | string | 是  | 文件分发目标路径                                                                                          |
| file_target_name     | string | 否  | 目标文件名；不传保持源文件名                                                                                    |
| file_source_list     | array  | 是  | 源文件对象数组，见 file_source                                                                             |
| timeout              | int    | 否  | 任务超时时间，单位秒，默认 7200                                                                                |
| download_speed_limit | int    | 否  | 下载限速，单位 MB；不传表示不限速                                                                                |
| upload_speed_limit   | int    | 否  | 上传限速，单位 MB；不传表示不限速                                                                                |
| transfer_mode        | int    | 否  | 传输模式。可选值：`1`（严谨模式）、`2`（强制模式）；默认强制模式                                                               |
| target_server        | object | 是  | 目标服务器，见 server                                                                                    |
| callback_url         | string | 否  | 回调 URL，任务完成后 JOB 调用该 URL 告知执行结果                                                                    |
| rolling_config       | object | 否  | 滚动配置，见 rolling_config                                                                             |
| start_task           | bool   | 否  | 是否创建后立即启动，默认 `true`；置 `false` 时仅创建不启动                                                             |

##### file_source

| 字段               | 类型     | 必选 | 描述                                                                                                          |
|------------------|--------|----|-------------------------------------------------------------------------------------------------------------|
| file_list        | array  | 是  | 文件路径列表。服务器文件填源文件绝对路径；本地文件填 `generate_local_file_upload_url` 返回的 `path`                                    |
| file_type        | int    | 否  | 文件源类型。技能支持：`1`（服务器文件）、`2`（本地文件）；不传默认为 `1`。`3`（第三方文件源文件）技能暂不支持                                            |
| account          | object | 否  | 文件源账号，见 account；**服务器文件（file_type=1）必填**，本地文件无需填写                                                        |
| server           | object | 否  | 源文件服务器，见 server；**服务器文件（file_type=1）必填**                                                                    |
| file_source_id   | int    | 否  | *技能暂不支持*。`file_type=3` 时用，第三方文件源 ID                                                                       |
| file_source_code | string | 否  | *技能暂不支持*。`file_type=3` 时用，第三方文件源标识                                                                        |

##### account

| 字段    | 类型     | 必选 | 描述                                                     |
|-------|--------|----|--------------------------------------------------------|
| id    | long   | 否  | 源执行账号 ID，可从 `get_account_list` 获取。与 `alias` 至少一个，同时存在时 `id` 优先 |
| alias | string | 否  | 源执行账号别名。与 `id` 至少一个，同时存在时 `id` 优先                       |

##### server

| 字段                 | 类型    | 必选 | 描述                                       |
|--------------------|-------|----|------------------------------------------|
| host_id_list       | array | 否  | 主机 ID（bk_host_id）列表，**推荐使用**             |
| ip_list            | array | 否  | *不推荐，建议用 host_id_list*。主机 IP 列表，见 ip_list |
| dynamic_group_list | array | 否  | 动态分组列表，元素为 `{"id": "<动态分组ID>"}`          |
| topo_node_list     | array | 否  | 动态 topo 节点列表，见 topo_node_list            |

##### ip_list

| 字段          | 类型     | 必选 | 描述     |
|-------------|--------|----|--------|
| bk_cloud_id | long   | 是  | 管控区域 ID |
| ip          | string | 是  | IP 地址  |

##### topo_node_list

| 字段        | 类型     | 必选 | 描述                                                    |
|-----------|--------|----|-------------------------------------------------------|
| id        | long   | 是  | 动态 topo 节点 ID，对应 CMDB 的 bk_inst_id                    |
| node_type | string | 是  | 动态 topo 节点类型，对应 CMDB 的 bk_obj_id，如 `module`、`set`     |

##### rolling_config

| 字段         | 类型     | 必选 | 描述                                                       |
|------------|--------|----|----------------------------------------------------------|
| type       | int    | 否  | 滚动对象：`1`（传输目标）、`2`（源文件）；不填默认传输目标                        |
| mode       | int    | 是  | 滚动机制：`1`（执行失败则暂停）、`2`（忽略失败自动滚动下一批）、`3`（人工确认）           |
| expression | string | 否  | 滚动对象为传输目标时必填，滚动策略表达式                                     |
| file_source | object | 否 | 滚动对象为源文件时必填，源文件滚动配置；配置后源文件类型仅支持服务器文件                    |

### 请求参数示例

- POST `/api/v3/fast_transfer_file`

分发服务器文件到目标主机：

```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "2",
    "file_target_path": "/tmp/",
    "transfer_mode": 1,
    "account_id": 101,
    "file_source_list": [
        {
            "file_type": 1,
            "file_list": ["/data/release/app.tar.gz"],
            "account": {"alias": "root"},
            "server": {"host_id_list": [101]}
        }
    ],
    "target_server": {"host_id_list": [103, 104]}
}
```

分发本地文件（file_list 为 generate_local_file_upload_url 返回的 path）：

```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "2",
    "file_target_path": "/tmp/",
    "account_alias": "root",
    "file_source_list": [
        {
            "file_type": 2,
            "file_list": ["2/008f821f-259b-4f62-bd84-1e89d6f05f0d/admin/app.sh"]
        }
    ],
    "target_server": {"host_id_list": [103, 104]}
}
```

### 返回结果示例

```json
{
    "result": true,
    "code": 0,
    "data": {
        "job_instance_name": "API Quick Distribution File1521101427176",
        "job_instance_id": 10000,
        "step_instance_id": 10001
    },
    "job_request_id": "xxx"
}
```

### 返回结果参数说明

| 字段             | 类型     | 是否一定不为null | 描述                         |
|----------------|--------|------------|----------------------------|
| result         | bool   | 是          | 请求成功与否。true:请求成功；false请求失败 |
| code           | int    | 是          | 错误编码。 0表示success，>0表示失败错误  |
| message        | string | 否          | 请求失败返回的错误信息                |
| data           | object | 否          | 请求返回的数据                    |
| job_request_id | string | 否          | 请求ID，请求唯一标识                |
| permission     | object | 否          | 无权限返回的权限信息                 |

##### data

| 字段                | 类型   | 是否一定不为null | 描述     |
|-------------------|------|------------|--------|
| job_instance_id   | long | 是          | 作业实例 ID |
| job_instance_name | string | 是        | 作业实例名称 |
| step_instance_id  | long | 是          | 步骤实例 ID |
