### 功能描述

快速执行脚本

### 请求参数

POST /open/api/scope/{scopeType}/{scopeId}/execute/fast_execute_script

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
| bk_scope_id | string | 是 | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |

#### Body参数

| 字段 | 类型 | 必选 | 描述 |
|---------------|------------|--------|------------|
| script_version_id | long | 否 | 脚本版本ID。当script_version_id不为空的时候，使用script_version_id对应的脚本版本 |
| script_id | string | 否 | 脚本ID。当传入script_id，且script_version_id为空的时候，使用脚本的上线版本 |
| script_content | string | 否 | 脚本内容Base64。如果不存在script_version_id和script_id,那么使用script_content。优先级：script_version_id>script_id>script_content |
| task_name | string | 否 | 自定义作业名称 |
| script_param | string | 否 | 脚本参数Base64。注意：如果有多个参数，比如&#34;param1 param2&#34;这种，需要对&#34;param1 param2&#34;整体进行base64编码，而不是对每个参数进行base64编码再拼接起来 |
| timeout | long | 否 | 脚本执行超时时间，秒。默认7200，取值范围1-86400 |
| account_alias | string | 否 | 执行账号别名。与account_id必须存在一个。当同时存在account_alias和account_id时，account_id优先。 |
| account_id | long | 否 | 执行账号ID。与account_alias必须存在一个。当同时存在account_alias和account_id时，account_id优先。 |
| is_param_sensitive | int | 否 | 敏感参数将会在执行详情页面上隐藏, 0:不是（默认），1:是 |
| script_language | int | 否 | 脚本语言：1 - shell, 2 - bat, 3 - perl, 4 - python, 5 - powershell。当使用script_content传入自定义脚本的时候，需要指定script_language |
| execute_target | object | 是 | 执行目标，定义见 execute_target |
| callback | object | 否 | 回调配置。当任务执行完成后，JOB会调用该URL告知任务执行结果。回调协议参考callback_protocol组件文档 ｜
| rolling_config | object | 否 | 滚动配置，见rolling_config定义 |

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

| 字段 | 类型 | 必选 | 描述 |
| ---- | ------ | ---- | -------------- |
| expression | string | 是 | 滚动策略表达式 |
| mode | int | 是 | 滚动机制,1-执行失败则暂停；2-忽略失败，自动滚动下一批；3-人工确认 |

##### callback

| 字段 | 类型 | 必选 | 描述 |
| ---- | ------ | ---- | -------------- |
| url | string | 是 | 回调URL |

### 请求参数示例

- URI
```
POST /open/api/scope/biz/1/execute/fast_execute_script
```

- Body
```json
{
    "script_version_id": 1,
    "script_content": "ZWNobyAkMQ==",
    "script_param": "aGVsbG8=",
    "timeout": 1000,
    "account_id": 1000,
    "is_param_sensitive": 0,
    "script_language": 1,
    "execute_target":
    {
        "host_dynamic_group_list":
        [
            {
                "id": "blo8gojho0skft7pr5q0"
            }
        ],
        "host_id_list":
        [
            101,
            102
        ],
        "host_topo_node_list":
        [
            {
                "id": 1000,
                "node_type": "module"
            }
        ],
        "container_id_list":
        [
            10001,
            10002
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
    }
}
```

### 返回结果示例

```json
{
    "data":
    {
        "job_instance_name": "API Quick execution script1521100521303",
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
