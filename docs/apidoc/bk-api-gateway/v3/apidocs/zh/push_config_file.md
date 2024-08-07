### 功能描述

分发配置文件，此接口用于分发配置文件等小的纯文本文件

### 请求参数

#### Body参数

| 字段        |  类型      | 必选   |  描述      |
|-------------|------------|--------|------------|
| bk_scope_type | string | 是     | 资源范围类型。可选值: biz - 业务，biz_set - 业务集 |
| bk_scope_id | string | 是 | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |
| bk_biz_id        |  long       | 是     | *已弃用*。业务ID。此字段已被弃用并由字段bk_scope_type+bk_scope_id替换 |
| task_name        |  string     | 否     | 自定义作业名称 |
| account_alias  |  string    | 是     | 执行账号别名 |
| file_target_path |  string    | 是     | 文件传输目标路径 |
| file_list        |  array     | 是     | 源文件对象数组，见下面file定义 |
| target_server |  object  | 是 | 目标服务器，见server定义       |

##### file

| 字段      |  类型      | 必选   |  描述      |
|-----------|------------|--------|------------|
| file_name |  string    | 是     | 文件名称 |
| content   |  string    | 是     | 文件内容Base64 |

##### server

| 字段               | 类型  | 必选 | 描述                                |
| ------------------ | ----- | ---- | ----------------------------------- |
| host_id_list       | array | 否   | 主机ID列表                                                   |
| ip_list            | array | 否   | ***不推荐使用，建议使用host_id_list参数***;如果host_id_list与ip_list同时存在，将忽略ip_list参数。主机IP 列表，定义见ip |
| dynamic_group_list | array | 否   | 动态分组列表，定义见dynamic_group   |
| topo_node_list     | array | 否   | 动态 topo 节点列表，定义见topo_node |

##### ip

| 字段        | 类型   | 必选 | 描述     |
| ----------- | ------ | ---- | -------- |
| bk_cloud_id | long   | 是   | 管控区域ID |
| ip          | string | 是   | IP地址   |

##### dynamic_group

| 字段 | 类型   | 必选 | 描述           |
| ---- | ------ | ---- | -------------- |
| id   | string | 是   | CMDB动态分组ID |

##### topo_node_list

| 字段      | 类型   | 必选 | 描述                                                         |
| --------- | ------ | ---- | ------------------------------------------------------------ |
| id        | long   | 是   | 动态topo节点ID，对应CMDB API 中的 bk_inst_id                 |
| node_type | string | 是   | 动态topo节点类型，对应CMDB API 中的 bk_obj_id,比如"module","set" |

### 请求参数示例

- POST
```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "1",
    "account": "root",
    "file_target_path": "/tmp/",
    "file_list": [
        {
            "file_name": "a.txt",
            "content": "aGVsbG8gd29ybGQh"
        }
    ],
    "target_server": {
        "dynamic_group_list": [
            {
                "id": "blo8gojho0skft7pr5q0"
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
    }
}
```

### 返回结果示例

{% include '_common_response.md.j2' %}

##### data

| 字段      | 类型      | 描述      |
|-----------|-----------|-----------|
| job_instance_id     | long      | 作业实例ID |
| job_instance_name   | long      | 作业实例名称 |
| step_instance_id    | long      | 步骤实例ID |
