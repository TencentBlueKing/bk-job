### 功能描述

快速分发文件

### 请求参数

POST /open/api/scope/{scopeType}/{scopeId}/execute/fast_transfer_file

#### Header参数

| 字段 | 类型 | 必选 | 描述 |
|-----------|------------|--------|------------|
| X-Bkapi-Authorization | string | 是 | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept | string | 是 | 固定值。application/json |
| Content-Type | string | 是 | 固定值。application/json| 

#### Path 参数
| 字段 | 类型 | 必选 | 描述 |
|-----------|------------|--------|------------|
| bk_scope_type | string | 是 | 资源范围类型。可选值: biz - 业务，biz_set - 业务集 |
| bk_scope_id | string | 是 | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |                                 |

#### Body参数
| 字段             |  类型      | 必选   |  描述      |
|------------------|------------|--------|------------|
| task_name      |  string    | 否     | 自定义作业名称 |
| account_alias    |  string    | 否    | 目标执行账号别名，可从账号页面获取，推荐使用。与account_id必须存在一个。当同时存在account_alias和account_id时，account_id优先。 |
| account_id | long | 否 | 目标执行账号ID，可从get_account_list接口获取。与account_alias必须存在一个。当同时存在account_alias和account_id时，account_id优先。 |
| file_target_path |  string    | 是     | 文件传输目标路径 |
| file_source_list |  array     | 是     | 源文件对象数组，见下面file_source定义 |
| timeout          |  int    | 否     | 任务超时时间，秒，默认值为7200。取值范围1-86400。|
| download_speed_limit|  int    | 否     | 下载限速，单位MB。如果未传入该参数，表示不限速|
| upload_speed_limit|  int    | 否     | 上传限速，单位MB。如果未传入该参数，表示不限速|
| transfer_mode | int | 否 | 传输模式。1-严谨模式，2-强制模式。默认使用强制模式 |
| execute_target | object | 是 | 执行目标，定义见 execute_target |
| callback | object | 否 | 回调配置。当任务执行完成后，JOB会调用该URL告知任务执行结果。回调协议参考callback_protocol组件文档 ｜
| rolling_config    | object | 否     | 滚动配置，见rolling_config定义 |

##### file_source
| 字段          |  类型      | 必选   |  描述      |
|---------------|------------|--------|------------|
| file_list     |  array     | 是     | 支持多个文件，若文件源类型为服务器文件，填写源文件的绝对路径数组；若文件源类型为第三方文件源，COS文件源填写的路径为"bucket名称/文件路径"，例如：testbucket/test.txt |
| account       |  object    | 是     | 文件源账号，见account定义，文件源类型为服务器文件源时必填，文件源类型为第三方文件源时无需填写 |
| execute_target        |  object    | 否     | 执行目标-文件源，见 execute_object 定义 |
| file_type     |  int       | 否     | 文件源类型，1：服务器文件，3：第三方文件源文件，不传默认为1 |
| file_source_id |  int      | 否     | file_type为3时，file_source_id与file_source_code选择一个填写，若都填写，优先使用file_source_id，第三方文件源Id，可从get_job_detail接口返回结果中的步骤详情获取 |
| file_source_code|  string  | 否     | file_type为3时，file_source_id与file_source_code选择一个填写，若都填写，优先使用file_source_id，第三方文件源标识，可从作业平台的文件分发页面->选择文件源文件弹框中获取 |

##### account

| 字段  | 类型   | 必选 | 描述                                                         |
| ----- | ------ | ---- | ------------------------------------------------------------ |
| id    | long   | 否   | 源执行账号ID，可从get_account_list接口获取。与alias必须存在一个。当同时存在alias和id时，id优先。 |
| alias | string | 否   | 源执行账号别名，可从账号页面获取，推荐使用。与alias必须存在一个。当同时存在alias和id时，id优先。 |

##### execute_target
| 字段 | 类型 | 必选 | 描述 |
| ------------------ | ----- | ---- | ----------------------------------- |
| host_id_list | array | 否 | 主机ID列表 |
| host_dynamic_group_list | array | 否 | 主机动态分组列表，定义见 dynamic_group |
| host_topo_node_list | array | 否 | 主机动态 topo 节点列表，定义见 host_topo_node |
| container_id_list | array | 否 | 容器ID列表 |
| container_label_selector_list | array | 否 | 容器 Label Selector 表达式列表，定义见 container_label_selector |


##### dynamic_group

| 字段 | 类型 | 必选 | 描述 |
| ---- | ------ | ---- | -------------- |
| id | string | 是 | CMDB动态分组ID |

##### host_topo_node

| 字段 | 类型 | 必选 | 描述 |
|-----------|------------|--------|------------|
| id | long | 是 | 动态topo节点ID，对应CMDB API 中的 bk_inst_id |
| node_type | string | 是 | 动态topo节点类型，对应CMDB API 中的 bk_obj_id,比如"module","set" |

##### container_label_selector

| 字段 | 类型 | 必选 | 描述 |
|-----------|------------|--------|------------|
| topo_node_id | long | 是 | 容器拓扑节点，可以从 cmdb 业务拓扑获取 |
| topo_node_type | string | 是 | 容器拓扑节点类型 (biz/cluster/namespace)，可以从 cmdb 业务拓扑获取 |
| label_selector_expr_list | array | 是 | 容器 label 选择表达式，多个表示是之间为 AND 关系。定义见 label_selector_expr |


##### label_selector_expr

| 字段 | 类型 | 必选 | 描述 |
|-----------|------------|--------|------------|
| label_key | string | 是 | 容器 label key |
| label_value | string | 否 | 容器 label value |
| operator | string | 是 | label 选择计算操作符。当前支持(equals/not_equals/in/not_in/exists) |

##### rolling_config

| 字段 | 类型   | 必选 | 描述           |
| ---- | ------ | ---- | -------------- |
| expression   | string | 是   | 滚动策略表达式 |
| mode   | int | 是   | 滚动机制,1-执行失败则暂停；2-忽略失败，自动滚动下一批；3-人工确认 |

##### callback

| 字段 | 类型 | 必选 | 描述 |
| ---- | ------ | ---- | -------------- |
| url | string | 是 | 回调URL |

### 请求参数示例

- URI
```
POST /open/api/scope/biz/1/execute/fast_transfer_file
```

- Body
```json
{
    "file_target_path": "/tmp/",
    "transfer_mode": 1,
    "file_source_list":
    [
        {
            "file_list":
            [
                "/tmp/REGEX:[a-z]*.txt"
            ],
            "account":
            {
                "id": 100
            },
            "execute_target":
            {
                "dynamic_group_list":
                [
                    {
                        "id": "blo8gojho0skft7pr5q0"
                    },
                    {
                        "id": "blo8gojho0sabc7priuy"
                    }
                ],
                "host_id_list":
                [
                    101,
                    102
                ],
                "topo_node_list":
                [
                    {
                        "id": 1000,
                        "node_type": "module"
                    }
                ]
            },
            "file_type": 1
        },
        {
            "file_list":
            [
                "testbucket/test.txt"
            ],
            "file_type": 3,
            "file_source_id": 1
        },
        {
            "file_list":
            [
                "testbucket/test2.txt"
            ],
            "file_type": 3,
            "file_source_code": "testInnerCOS"
        }
    ],
    "execute_target":
    {
        "dynamic_group_list":
        [
            {
                "id": "blo8gojho0skft7pr5q0"
            },
            {
                "id": "blo8gojho0sabc7priuy"
            }
        ],
        "host_id_list":
        [
            103,
            104
        ],
        "topo_node_list":
        [
            {
                "id": 1000,
                "node_type": "module"
            }
        ],
        "container_label_selector_list":
        [
            {
                "topo_node_type": "namespace",
                "topo_node_id": 18881,
                "label_selector_expr_list":
                [
                    {
                        "label_key": "app",
                        "label_value": "job",
                        "operator": "equals"
                    },
                    {
                        "label_key": "component",
                        "label_value": "job-execute",
                        "operator": "equals"
                    }
                ]
            }
        ]
    },
    "account_id": 101
}
```
### 返回结果示例

```json
{
    "data":
    {
        "job_instance_name": "API Quick Distribution File1521101427176",
        "job_instance_id": 10000,
        "step_instance_id": 10001
    }
}
```

### 返回结果参数说明

| 字段 | 类型 | 描述 |
|-----------|-----------|-----------|
| data | object | 正常请求返回的数据 |
| error | object | 如果响应的 http code 不为 200/201, error 用于说明错误信息 |

##### data

| 字段 | 类型 | 描述 |
|-----------|-----------|-----------|
| job_instance_id | long | 作业实例ID |
| job_instance_name | long | 作业实例名称 |
| step_instance_id | long | 步骤实例ID |

##### error

| 字段 | 类型 | 描述 |
|-----------|-----------|-----------|
| code | string | 蓝鲸 API 通用错误码 |
| message | string | 错误信息 (给用户看的) |
| data | object | 不同的 code 对应的错误 payload 信息，用于给调用方针对这个 code 做响应的处理，比如无权限返回权限申请信息。|
