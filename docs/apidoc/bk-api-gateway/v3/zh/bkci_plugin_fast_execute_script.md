### 功能描述

快速执行脚本(蓝盾作业执行插件专用，非正式公开 API)

### 请求参数

#### Header参数

| 字段 | 类型 | 必选 | 描述 |
|-----------|------------|--------|------------|
| X-Bkapi-Authorization | string | 是 | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept | string | 是 | 固定值。application/json |
| Content-Type | string | 是 | 固定值。application/json| 


#### Body参数

| 字段 | 类型 | 必选 | 描述 |
|---------------|------------|--------|------------|
| bk_scope_type | string | 是 | 资源范围类型。可选值: biz - 业务，biz_set - 业务集 |
| bk_scope_id | string | 是 | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |
| script_version_id | long | 否 | 脚本版本ID。当script_version_id不为空的时候，使用script_version_id对应的脚本版本 |
| script_id | string | 否 | 脚本ID。当传入script_id，且script_version_id为空的时候，使用脚本的上线版本 |
| script_content | string | 否 | 脚本内容Base64。如果不存在script_version_id和script_id,那么使用script_content。优先级：script_version_id>script_id>script_content |
| task_name | string | 否 | 自定义作业名称 |
| script_param | string | 否 | 脚本参数Base64。脚本参数长度不得超过 5000 个字符。注意：如果有多个参数，比如&#34;param1 param2&#34;这种，需要对&#34;param1 param2&#34;整体进行base64编码，而不是对每个参数进行base64编码再拼接起来 |
| timeout | long | 否 | 脚本执行超时时间，秒。默认7200，取值范围1-86400 |
| account_alias | string | 否 | 执行账号别名。与account_id必须存在一个。当同时存在account_alias和account_id时，account_id优先。 |
| account_id | long | 否 | 执行账号ID。与account_alias必须存在一个。当同时存在account_alias和account_id时，account_id优先。 |
| is_param_sensitive | boolean | 否 | 敏感参数将会在执行详情页面上隐藏。默认为 false|
| script_language | int | 否 | 脚本语言：1 - shell, 2 - bat, 3 - perl, 4 - python, 5 - powershell。当使用script_content传入自定义脚本的时候，需要指定script_language |
| execute_target | object | 是 | 执行目标，定义见 execute_target |
| callback | object | 否 | 回调配置。当任务执行完成后，JOB会调用该URL告知任务执行结果。回调协议参考callback_protocol组件文档 ｜
| rolling_config | object | 否 | 滚动配置，见rolling_config定义 |

##### execute_target

| 字段 | 类型 | 必选 | 描述 |
| ------------------ | ----- | ---- | ----------------------------------- |
| host_list | array | 否 | 主机列表，调用方可以选择使用 bk_host_id 或者 bk_cloud_id+ip 指定主机两种方式。 见 host 定义 |
| host_dynamic_group_list | array | 否 | 主机动态分组列表，定义见 dynamic_group |
| host_topo_node_list | array | 否 | 主机动态 topo 节点列表，定义见 host_topo_node |
| kube_container_filters | array | 否 | 容器过滤器，多个过滤器为 OR 关系，取容器并集。定义见 kube_container_filter |

##### host

| 字段 | 类型 | 必选 | 描述 |
|-----------|------------|--------|
| bk_host_id | long | 否 | 主机 ID (cmdb) |
| bk_cloud_id | long | 否 | 管控区域ID |
| ip | string | 否 | Ipv4 ｜

##### dynamic_group

| 字段 | 类型 | 必选 | 描述 |
| ---- | ------ | ---- | -------------- |
| id | string | 是 | CMDB动态分组ID |

##### host_topo_node

| 字段 | 类型 | 必选 | 描述 |
|-----------|------------|--------|------------|
| id | long | 是 | 动态topo节点ID，对应CMDB API 中的 bk_inst_id |
| node_type | string | 是 | 动态topo节点类型，对应CMDB API 中的 bk_obj_id,比如"module","set" |

##### kube_container_filter

| 字段 | 类型 | 必选 | 描述 |
|-----------|------------|--------|------------|
| kube_cluster_filter | object | 否 | 集群过滤器. 过滤器定义见 kube_cluster_filter |
| kube_namespace_filter | object | 否 |  namespace 过滤器. 过滤器定义见 kube_namespace_filter |
| kube_workload_filter | object | 否 |  workload 过滤器. 过滤器定义见 kube_workload_filter |
| kube_pod_filter | object | 否 | pod 属性过滤器，过滤器定义见 kube_pod_filter |
| kube_container_prop_filter | object | 是 | 容器属性过滤器，过滤器定义见 kube_container_prop_filter |
| is_empty_filter | boolean | 是 | 标识一个没有设置任何条件的过滤器；默认值为 false。如果为 true, 将忽略其他的条件（kube_cluster_filter/kube_namespace_filter/kube_workload_filter/kube_pod_filter/kube_container_prop_filter)，返回业务下的所有容器 |
| fetch_any_one_container | boolean | 否 | 是否从过滤结果集中选择任意一个容器作为执行对象（只有一个容器会被执行）；默认为 false |

##### kube_cluster_filter

| 字段 | 类型 | 必选 | 描述 |
|-----------|------------|--------|------------|
| cluster_uid_list | array | 是 | 集群ID列表, 例如: "BCS-K8S-00001" |

##### kube_namespace_filter

| 字段 | 类型 | 必选 | 描述 |
|-----------|------------|--------|------------|
| namespace_name_list | array | 是 | namespace 名称列表 |


##### kube_workload_filter

| 字段 | 类型 | 必选 | 描述 |
|-----------|------------|--------|------------|
| kind | string | 是 | workload类型，目前支持的workload类型有deployment、daemonSet、statefulSet、gameStatefulSet、gameDeployment、cronJob、job) |
| workload_name_list | array | 否 | workload 名称列表 |

##### kube_pod_prop_filter

| 字段 | 类型 | 必选 | 描述 |
|-----------|------------|--------|------------|
| pod_name_list | array | 否 | pod 名称列表 |
| label_selector | array | 否 | Job 定义的label selector，由多个 Requirement 构成。多个 Requirement 之间为 AND 关系。表达式 见 requirement 定义 |
| label_selector_expr | string | 否 | k8s 官方定义的 label selector 表达式，[lable selector doc](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#label-selectors) |

##### requirement

| 字段 | 类型 | 必选 | 描述 |
|-----------|------------|--------|------------|
| key | string | 是 | label key |
| operator |  string | 是 | 计算操作符, 支持 equals、not_equals、in、not_in、exists、not_exists |
| values | array | 否 | label value 列表, 当计算操作符为 in、not_in、equals、not_equals 时需要设置 |

##### kube_container_prop_filter

| 字段 | 类型 | 必选 | 描述 |
|-----------|------------|--------|------------|
| container_name_list | array | 是 | 容器名称列表 |


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

- Body
```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "2",
    "script_version_id": 1,
    "script_content": "ZWNobyAkMQ==",
    "script_param": "aGVsbG8=",
    "timeout": 1000,
    "account_id": 1000,
    "is_param_sensitive": 0,
    "script_language": 1,
    "execute_target": {
        "host_dynamic_group_list": [
            {
                "id": "blo8gojho0skft7pr5q0"
            }
        ],
        "host_list": [
            {
                "bk_host_id": 101
            },
            {
                "bk_cloud_id": 0,
                "ip": "10.0.0.2"
            }
        ],
        "host_topo_node_list": [
            {
                "id": 1000,
                "node_type": "module"
            }
        ],
        "kube_container_filters": [
            {
                "kube_cluster_filter": {
                    "cluster_uid_list": [
                        "BCS-K8S-00001",
                        "BCS-K8S-00002"
                    ]
                },
                "kube_namespace_filter": {
                    "namespace_name_list": [
                        "job-prod",
                        "job-gray"
                    ]
                },
                "kube_workload_filter": {
                    "kind": "deployment",
                    "workload_name_list": [
                        "bk-job-manage",
                        "bk-job-execute"
                    ]
                },
                "kube_pod_filter": {
                    "pod_name_list": [
                        "bk-job-execute-6fcd8cf5c7-jvctq",
                        "bk-job-manage-6fcd8cf5c7-abues"
                    ],
                    "label_selector": [
                        {
                            "label_key": "application",
                            "operator": "in",
                            "label_values": [
                                "job-execute",
                                "job-manage"
                            ]
                        },
                        {
                            "label_key": "env",
                            "operator": "equals",
                            "label_value": "prod"
                        }
                    ]
                },
                "kube_container_prop_filter": {
                    "container_name_list": [
                        "job-execute",
                        "job-manage"
                    ]
                },
                "fetch_any_one_container": true
            }
        ]
    }
}
```

### 返回结果示例

```json
{
    "result": true,
    "code": 0,
    "data": {
        "job_instance_name": "API Quick execution script1521100521303",
        "job_instance_id": 10000,
        "step_instance_id": 10001
    }
}
```

### 返回结果参数说明

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
| job_instance_id | long | 作业实例ID |
| job_instance_name | string | 作业实例名称 |
| step_instance_id | long | 步骤实例ID |
