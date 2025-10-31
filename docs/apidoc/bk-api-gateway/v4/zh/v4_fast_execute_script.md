### 功能描述

快速执行脚本

### 请求参数

#### Header参数

| 字段                    | 类型     | 必选 | 描述                                                                                                                               |
|-----------------------|--------|----|----------------------------------------------------------------------------------------------------------------------------------|
| X-Bkapi-Authorization | string | 是  | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept                | string | 是  | 固定值。application/json                                                                                                             |
| Content-Type          | string | 是  | 固定值。application/json                                                                                                             |

#### Body参数

| 字段                  | 类型      | 必选 | 描述                                                                                                                                                      |
|---------------------|---------|----|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| bk_scope_type       | string  | 是  | 资源范围类型。可选值: biz - 业务，biz_set - 业务集                                                                                                                      |
| bk_scope_id         | string  | 是  | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID                                                                                                                 |
| script_version_id   | long    | 否  | 脚本版本ID。当script_version_id不为空的时候，使用script_version_id对应的脚本版本                                                                                              |
| script_id           | string  | 否  | 脚本ID。当传入script_id，且script_version_id为空的时候，使用脚本的上线版本                                                                                                     |
| script_content      | string  | 否  | 脚本内容Base64。如果不存在script_version_id和script_id,那么使用script_content。优先级：script_version_id>script_id>script_content                                           |
| task_name           | string  | 否  | 自定义作业名称，长度不可超过512字符                                                                                                                                     |
| script_param        | string  | 否  | 脚本参数Base64。注意：1.如果有多个参数，比如&#34;param1 param2&#34;这种，需要对&#34;param1 param2&#34;整体进行base64编码，而不是对每个参数进行base64编码再拼接起来。2.非敏感参数编码前长度不能超过64K，敏感参数编码前长度不能超过47K |
| timeout             | int     | 否  | 脚本执行超时时间，秒。默认7200，取值范围1-259200                                                                                                                          |
| account_alias       | string  | 否  | 执行账号别名。与account_id必须存在一个。当同时存在account_alias和account_id时，account_id优先。                                                                                   |
| account_id          | long    | 否  | 执行账号ID。与account_alias必须存在一个。当同时存在account_alias和account_id时，account_id优先。                                                                                |
| param_sensitive     | boolean | 否  | 敏感参数将会在执行详情页面上隐藏，默认false                                                                                                                                |
| script_language     | int     | 否  | 脚本语言：1 - shell, 2 - bat, 3 - perl, 4 - python, 5 - powershell。当使用script_content传入自定义脚本的时候，需要指定script_language                                           |
| windows_interpreter | string  | 否  | 自定义Windows解释器路径，必须以.exe结尾，最大长度260字符                                                                                                                     |
| execute_target      | object  | 否  | 执行目标，见execute_target定义                                                                                                                                  |
| callback_url        | string  | 否  | 回调URL，当任务执行完成后，JOB会调用该URL告知任务执行结果。回调协议参考callback_protocol组件文档                                                                                           |
| rolling_config      | object  | 否  | 滚动配置，见rolling_config定义                                                                                                                                  |
| start_task          | boolean | 否  | 是否启动任务，默认true，如果是false可以通过operate_job_instance接口启动任务                                                                                                    |
| host_password_list  | array   | 否  | windows主机密码，见host_password_list定义。该字段默认情况下无需填写，默认使用在作业平台中配置的指定账号对应密码，只有在各主机账号相同密码不同的情况下才需要填写。                                                           |

##### execute_target

| 字段                     | 类型    | 必选 | 描述                               |
|------------------------|-------|----|----------------------------------|
| host_list              | array | 否  | 静态主机列表，见host定义                   |
| dynamic_group_list     | array | 否  | 动态分组ID列表，见dynamic_group定义        |
| topo_node_list         | array | 否  | 分布式拓扑节点列表，见topo_node定义           |
| kube_container_filters | array | 否  | 容器过滤器列表，见kube_container_filter定义 |

**说明：** host_list、dynamic_group_list、topo_node_list、kube_container_filters不能同时为空。主机和容器不能混合执行

##### host

| 字段          | 类型     | 必选 | 描述                                                                      |
|-------------|--------|----|-------------------------------------------------------------------------|
| bk_host_id  | long   | 否  | 主机ID。与ip+bk_cloud_id必须存在一个。当同时存在bk_host_id和ip+bk_cloud_id时，bk_host_id优先 |
| bk_cloud_id | long   | 否  | 云区域ID。与bk_host_id必须存在一个。当同时存在bk_host_id和ip+bk_cloud_id时，bk_host_id优先    |
| ip          | string | 否  | IP地址。与bk_host_id必须存在一个。当同时存在bk_host_id和ip+bk_cloud_id时，bk_host_id优先     |

##### dynamic_group

| 字段 | 类型     | 必选 | 描述     |
|----|--------|----|--------|
| id | string | 是  | 动态分组ID |

##### topo_node

| 字段        | 类型     | 必选 | 描述                            |
|-----------|--------|----|-------------------------------|
| id        | long   | 是  | 拓扑节点ID                        |
| node_type | string | 是  | 拓扑节点类型，可选值：module(模块)、set(集群) |

##### kube_container_filter

| 字段                         | 类型      | 必选 | 描述                                    |
|----------------------------|---------|----|---------------------------------------|
| kube_cluster_filter        | object  | 否  | 集群过滤器，见kube_cluster_filter定义          |
| kube_namespace_filter      | object  | 否  | namespace过滤器，见kube_namespace_filter定义 |
| kube_workload_filter       | object  | 否  | workload过滤器，见kube_workload_filter定义   |
| kube_pod_filter            | object  | 否  | pod属性过滤器，见kube_pod_filter定义           |
| kube_container_prop_filter | object  | 否  | 容器属性过滤器，见kube_container_prop_filter定义 |
| is_empty_filter            | boolean | 否  | 是否为空过滤器，默认false                       |
| fetch_any_one_container    | boolean | 否  | 是否只获取任意一个容器，默认false                   |

###### kube_cluster_filter

| 字段               | 类型    | 必选 | 描述      |
|------------------|-------|----|---------|
| cluster_uid_list | array | 是  | 集群UID列表 |

###### kube_namespace_filter

| 字段                  | 类型    | 必选 | 描述            |
|---------------------|-------|----|---------------|
| namespace_name_list | array | 是  | namespace名称列表 |

###### kube_workload_filter

| 字段                 | 类型     | 必选 | 描述                                            |
|--------------------|--------|----|-----------------------------------------------|
| kind               | string | 是  | workload类型，如deployment、statefulset、daemonset等 |
| workload_name_list | array  | 是  | workload名称列表                                  |

###### kube_pod_filter

| 字段                  | 类型    | 必选 | 描述                        |
|---------------------|-------|----|---------------------------|
| pod_name_list       | array | 否  | pod名称列表                   |
| label_selector      | array | 否  | 标签选择器列表，见label_selector定义 |
| label_selector_expr | array | 否  | label selector表达式         |

###### label_selector

| 字段       | 类型     | 必选 | 描述                                                                                                                                   |
|----------|--------|----|--------------------------------------------------------------------------------------------------------------------------------------|
| key      | string | 是  | label key                                                                                                                            |
| operator | string | 是  | 操作符，可选值：`not_exists`-标签不存在、`equals`-标签值等于、`in`-标签值在指定列表中、`not_equals`-标签值不等于、`not_in`-标签值不在指定列表中、`exists`-标签存在、`gt`-标签值大于、`lt`-标签值小于 |
| values   | array  | 否  | label value列表，当operator为in或not_in时必填                                                                                                 |

###### kube_container_prop_filter

| 字段                  | 类型    | 必选 | 描述     |
|---------------------|-------|----|--------|
| container_name_list | array | 是  | 容器名称列表 |

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

##### host_password_list

| 字段                 | 类型     | 必选 | 描述                                              |
|--------------------|--------|----|-------------------------------------------------|
| bk_cloud_id        | long   | 否  | 管控区域ID                                          |
| ip                 | string | 否  | IP地址                                            |
| host_id            | long   | 否  | 主机ID。host_id与bk_cloud_id+ip，二者至少填写其一，host_id优先。 |
| encrypted_password | String | 是  | 加密后的密码                                          |

### 请求参数示例

- POST

在主机上执行脚本：
```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "1",
    "script_version_id": 1,
    "script_content": "ZWNobyAkMQ==",
    "script_param": "aGVsbG8=",
    "timeout": 1000,
    "account_id": 1000,
    "param_sensitive": false,
    "script_language": 1,
    "execute_target": {
        "dynamic_group_list": [
            {
                "id": "asdo8gojhasdfskft7pr5"
            }
        ],
        "host_list": [
            {
                "ip": "10.0.0.1",
                "bk_cloud_id": 0
            },
            {
                "ip": "10.0.0.2",
                "bk_cloud_id": 0
            }
        ],
        "topo_node_list": [
            {
                "id": 1000,
                "node_type": "module"
            }
        ]
    }
}
```

在容器上执行脚本：
```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "1",
    "script_content": "ZWNobyAkMQ==",
    "script_param": "aGVsbG8=",
    "timeout": 1000,
    "account_id": 1000,
    "param_sensitive": false,
    "script_language": 1,
    "execute_target": {
        "kube_container_filters": [
            {
                "kube_cluster_filter": {
                    "cluster_uid_list": ["BCS-K8S-00001"]
                },
                "kube_namespace_filter": {
                    "namespace_name_list": ["bkjob"]
                },
                "kube_workload_filter": {
                    "kind": "deployment",
                    "workload_name_list": ["bk-job-execute"]
                },
                "kube_container_prop_filter": {
                    "container_name_list": ["job-execute"]
                },
                "is_empty_filter": false,
                "fetch_any_one_container": false
            }
        ]
    }
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

```json
# http status=200
{
    "data": {
        "job_instance_name": "API Quick execution script1",
        "job_instance_id": 10000,
        "step_instance_id": 10001
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

| 字段                | 类型     | 是否一定不为null | 描述     |
|-------------------|--------|------------|--------|
| job_instance_id   | long   | 是          | 作业实例ID |
| job_instance_name | string | 否          | 作业实例名称 |
| step_instance_id  | long   | 否          | 步骤实例ID |

