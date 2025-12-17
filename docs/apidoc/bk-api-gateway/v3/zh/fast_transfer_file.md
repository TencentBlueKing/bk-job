### 功能描述

快速分发文件

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
| bk_scope_type        | string | 是  | 资源范围类型。可选值: biz - 业务，biz_set - 业务集                                                                |
| bk_scope_id          | string | 是  | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID                                                           |
| bk_biz_id            | long   | 是  | *已弃用*。业务ID。此字段已被弃用并由字段bk_scope_type+bk_scope_id替换                                                 |
| task_name            | string | 否  | 自定义作业名称，长度不可超过512字符                                                                               |
| account_alias        | string | 否  | 目标执行账号别名，可从账号页面获取，推荐使用。与account_id必须存在一个。当同时存在account_alias和account_id时，account_id优先。             |
| account_id           | long   | 否  | 目标执行账号ID，可从get_account_list接口获取。与account_alias必须存在一个。当同时存在account_alias和account_id时，account_id优先。 |
| file_target_path     | string | 是  | 文件传输目标路径                                                                                          |
| file_source_list     | array  | 是  | 源文件对象数组，见下面file_source定义                                                                          |
| timeout              | int    | 否  | 任务超时时间，秒，默认值为7200。取值范围1-86400。                                                                    |
| download_speed_limit | int    | 否  | 下载限速，单位MB。如果未传入该参数，表示不限速                                                                          |
| upload_speed_limit   | int    | 否  | 上传限速，单位MB。如果未传入该参数，表示不限速                                                                          |
| transfer_mode        | int    | 否  | 传输模式。1-严谨模式，2-强制模式。默认使用强制模式                                                                       |
| target_server        | object | 否  | 目标服务器，见server定义                                                                                   |
| callback_url         | string | 否  | 回调URL，当任务执行完成后，JOB会调用该URL告知任务执行结果。回调协议参考callback_protocol组件文档                                     |
| rolling_config       | object | 否  | 滚动配置，见rolling_config定义                                                                            |

##### file_source

| 字段               | 类型     | 必选 | 描述                                                                                                                 |
|------------------|--------|----|--------------------------------------------------------------------------------------------------------------------|
| file_list        | array  | 是  | 支持多个文件，若文件源类型为服务器文件，填写源文件的绝对路径数组；若文件源类型为第三方文件源，COS文件源填写的路径为"bucket名称/文件路径"，例如：testbucket/test.txt                  |
| account          | object | 是  | 文件源账号，见account定义，文件源类型为服务器文件源时必填，文件源类型为第三方文件源时无需填写                                                                 |
| server           | object | 否  | 源文件服务器，见server定义                                                                                                   |
| file_type        | int    | 否  | 文件源类型，1：服务器文件，3：第三方文件源文件，不传默认为1                                                                                    |
| file_source_id   | int    | 否  | file_type为3时，file_source_id与file_source_code选择一个填写，若都填写，优先使用file_source_id，第三方文件源Id，可从get_job_detail接口返回结果中的步骤详情获取 |
| file_source_code | string | 否  | file_type为3时，file_source_id与file_source_code选择一个填写，若都填写，优先使用file_source_id，第三方文件源标识，可从作业平台的文件分发页面->选择文件源文件弹框中获取    |

##### account

| 字段    | 类型     | 必选 | 描述                                                               |
|-------|--------|----|------------------------------------------------------------------|
| id    | long   | 否  | 源执行账号ID，可从get_account_list接口获取。与alias必须存在一个。当同时存在alias和id时，id优先。 |
| alias | string | 否  | 源执行账号别名，可从账号页面获取，推荐使用。与alias必须存在一个。当同时存在alias和id时，id优先。          |

##### server

| 字段                 | 类型    | 必选 | 描述                                           |
|--------------------|-------|----|----------------------------------------------|
| host_id_list       | array | 否  | 主机ID列表                                       |
| ip_list            | array | 否  | ***不推荐使用，建议使用host_id_list参数***。主机IP 列表，定义见ip |
| dynamic_group_list | array | 否  | 动态分组列表，定义见dynamic_group                      |
| topo_node_list     | array | 否  | 动态 topo 节点列表，定义见topo_node                    |

##### ip_list

| 字段          | 类型     | 必选 | 描述     |
|-------------|--------|----|--------|
| bk_cloud_id | long   | 是  | 管控区域ID |
| ip          | string | 是  | IP地址   |

##### topo_node_list

| 字段        | 类型     | 必选 | 描述                                                  |
|-----------|--------|----|-----------------------------------------------------|
| id        | long   | 是  | 动态topo节点ID，对应CMDB API 中的 bk_inst_id                 |
| node_type | string | 是  | 动态topo节点类型，对应CMDB API 中的 bk_obj_id,比如"module","set" |

##### rolling_config

| 字段          | 类型     | 必选 | 描述                                                                                  |
|-------------|--------|----|-------------------------------------------------------------------------------------|
| type        | int    | 否  | 滚动对象：1-传输目标；2-源文件；不填写默认为传输目标。                                                       |
| mode        | int    | 是  | 滚动机制：1-执行失败则暂停；2-忽略失败，自动滚动下一批；3-人工确认                                                |
| expression  | string | 否  | 滚动对象为【传输目标】时必填，滚动策略表达式                                                              |
| file_source | object | 否  | 滚动对象为【源文件】时必填，源文件滚动配置，见rolling_config.file_source定义，不支持与传输目标同时滚动，配置该项后源文件类型仅支持服务器文件 |

##### rolling_config.file_source

| 字段                                    | 类型  | 必选 | 描述                     |
|---------------------------------------|-----|----|------------------------|
| max_execute_object_num_in_batch       | int | 否  | 单批次最大源主机/容器数，不填写表示不限制  |
| max_file_num_of_single_execute_object | int | 否  | 单主机/容器最大并发文件数，不填写表示不限制 |

**源文件滚动策略说明：**  
以单个源文件组为单位，判断其中源文件目标机器与文件数量是否超限：  
若不超限，按顺序将相邻的尽可能多的源文件组划分为一个滚动批次；  
若任一参数超限，则将该源文件组按照滚动参数拆分为多个滚动批次；  
划分滚动批次时优先将同一组机器的文件分批传输完成再开始下一组机器。

**例子1（按单机器单文件串行滚动分批）：**  
源文件配置：  
源文件组1： 文件1，文件2 | 机器1，机器2，机器3  
max_execute_object_num_in_batch=1  
max_file_num_of_single_execute_object=1  
划分出6个滚动批次：  
批次1：机器1 x 文件1  
批次2：机器1 x 文件2  
批次3：机器2 x 文件1  
批次4：机器2 x 文件2  
批次5：机器3 x 文件1  
批次6：机器3 x 文件2

**例子2（一个源文件组拆分为多个批次）：**  
源文件配置：  
源文件组1： 文件1，文件2，文件3 | 机器1，机器2，机器3  
源文件组2： 文件4 | 机器4  
max_execute_object_num_in_batch=2  
max_file_num_of_single_execute_object=2  
划分出5个滚动批次：  
批次1：（机器1，机器2） x （文件1，文件2）
批次2：（机器1，机器2） x 文件3
批次3：机器3 x （文件1，文件2）
批次4：机器3 x 文件3  
批次5：机器4 x 文件4

**例子3（滚动参数较大时，多个源文件组融合为一个批次）：**  
源文件配置：  
源文件组1： 文件1，文件2，文件3 | 机器1，机器2，机器3  
源文件组2： 文件4 | 机器4  
max_execute_object_num_in_batch=4  
max_file_num_of_single_execute_object=4  
划分出1个滚动批次：  
批次1：（（机器1，机器2，机器3） x （文件1，文件2，文件3））并行（机器4 x 文件4）

### 请求参数示例

- POST

#### 普通任务示例

```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "1",
    "file_target_path": "/tmp/",
    "transfer_mode": 1,
    "file_source_list": [
        {
            "file_list": [
                "/tmp/REGEX:[a-z]*.txt"
            ],
            "account": {
                "id": 100
            },
            "server": {
                "dynamic_group_list": [
                    {
                        "id": "blo8gojho0skft7pr5q0"
                    },
                    {
                        "id": "blo8gojho0sabc7priuy"
                    }
                ],
                "host_id_list": [
                    101,
                    102
                ],
                "topo_node_list": [
                    {
                        "id": 1000,
                        "node_type": "module"
                    }
                ]
            },
            "file_type": 1
        },
        {
            "file_list": [
                "testbucket/test.txt"
            ],
            "file_type": 3,
            "file_source_id": 1
        },
        {
            "file_list": [
                "testbucket/test2.txt"
            ],
            "file_type": 3,
            "file_source_code": "testInnerCOS"
        }
    ],
    "target_server": {
        "dynamic_group_list": [
            {
                "id": "blo8gojho0skft7pr5q0"
            },
            {
                "id": "blo8gojho0sabc7priuy"
            }
        ],
        "host_id_list": [
            103,
            104
        ],
        "topo_node_list": [
            {
                "id": 1000,
                "node_type": "module"
            }
        ]
    },
    "account_id": 101
}
```

#### 按源文件滚动任务示例

```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "1",
    "file_target_path": "/tmp/",
    "transfer_mode": 1,
    "file_source_list": [
        {
            "file_list": [
                "/tmp/1.txt",
                "/tmp/2.txt",
                "/tmp/3.txt"
            ],
            "account": {
                "id": 100
            },
            "server": {
                "host_id_list": [
                    101,
                    102
                ]
            },
            "file_type": 1
        }
    ],
    "target_server": {
        "host_id_list": [
            103,
            104
        ]
    },
    "account_id": 101,
    "rolling_config": {
        "type": 2,
        "mode": 1,
        "file_source": {
            "max_execute_object_num_in_batch": 1,
            "max_file_num_of_single_execute_object": 1
        }
    }
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
| data           | object | 否          | 请求返回的数据，删除操作可能没有值          |
| job_request_id | string | 否          | 请求ID，请求唯一标识                |
| permission     | object | 否          | 无权限返回的权限信息                 |

##### data

| 字段                | 类型   | 是否一定不为null | 描述     |
|-------------------|------|------------|--------|
| job_instance_id   | long | 是          | 作业实例ID |
| job_instance_name | long | 是          | 作业实例名称 |
| step_instance_id  | long | 是          | 步骤实例ID |
