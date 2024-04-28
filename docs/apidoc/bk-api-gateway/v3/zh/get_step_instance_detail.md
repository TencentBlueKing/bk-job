### 功能描述

根据步骤实例 ID 查询步骤实例详情

### 请求参数说明

{{ bkapi_authorization_description }}

#### Header参数

| 字段      |  类型      | 必选   |  描述      |
|-----------|------------|--------|------------|
| X-Bkapi-Authorization       |  string    | 是     | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept       |  string    | 是     | 固定值。application/json|
| Content-Type |  string    | 是     | 固定值。application/json|

#### Query参数

| 字段                    |  类型      | 必选   |  描述      |
|------------------------|-----------|-------|------------|
| bk_scope_type          | string    | 是     | 资源范围类型。可选值: biz - 业务，biz_set - 业务集 |
| bk_scope_id            | string    | 是     | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |
| job_instance_id        | long      | 是     | 作业实例ID |
| step_instance_id       | long      | 是     | 步骤实例ID |


### 请求参数示例

- GET
```json
/api/v3/get_step_instance_detail?bk_scope_type=biz&bk_scope_id=1&job_instance_id=100&step_instance_id=100
```

### 返回结果示例

```json
{
    "result": true,
    "code": 0,
    "message": "",
    "data": {
        "id": 4,
        "name": "Step 1",
        "type": 1,
        "script_info": {
          "script_type": 1,
          "script_id": null,
          "script_version_id": null,
          "script_content": "#!/bin/bash\n\nanynowtime=\"date +'%Y-%m-%d %H:%M:%S'\"\nNOW=\"echo [\\`$anynowtime\\`][PID:$$]\"\n\n##### 可在脚本开始运行时调用，打印当时的时间戳及PID。\nfunction job_start\n{\n echo \"`eval $NOW` job_starts\"\n}\n\n##### 可在脚本执行成功的逻辑分支处调用，打印当时的时间戳及PID。 \nfunction job_success\n{\n MSG=\"$*\"\n echo \"`eval $NOW` job_success:[$MSG]\"\n exit 0\n}\n\n##### 可在脚本执行失败的逻辑分支处调用，打印当时的时间戳及PID。\nfunction job_fail\n{\n MSG=\"$*\"\n echo \"`eval $NOW` job_fail:[$MSG]\"\n exit 1\n}\n\njob_start\n\n###### 作业平台中执行脚本成功和失败的标准只取决于脚本最后一条执行语句的返回值\n###### 如果返回值为0，则认为此脚本执行成功，如果非0，则认为脚本执行失败\n###### 可在此处开始编写您的脚本逻辑代码\n\necho 1",
          "script_language": 1,
          "script_param": "1 2 3",
          "script_timeout": 1000,
          "account": {
            "id": 123,
            "name": "root"
          },
          "server": {
            "variable": null,
            "ip_list": [{
              "bk_host_id": 1,
              "bk_cloud_id": 0,
              "bk_cloud_name": "Default Area",
              "ip": "192.168.1.1",
              "ipv6": null,
              "bk_agent_id": "020000000052540079809816967513755991",
              "alive": 1
            }],
            "topo_node_list": [{
              "node_type": "set",
              "id": 123
            }],
            "dynamic_group_list": [{
              "id": "07f99504-7bcb-11eb-980b-5254008ed702"
            }]
          },
          "is_param_sensitive": 0,
          "is_ignore_error": 0
        },
        "file_info": {
          "file_source_list": [
            {
              "file_type": 1,
              "file_list": [
                "/tmp/1.txt",
                "/tmp/2.txt"
              ],
              "server": {
                "variable": null,
                "server": {
                  "ip_list": [{
                    "bk_host_id": 1,
                    "bk_cloud_id": 0,
                    "bk_cloud_name": "Default Area",
                    "ip": "192.168.1.1",
                    "ipv6": null,
                    "bk_agent_id": "020000000052540079809816967513755991",
                    "alive": 1
                  }],
                  "topo_node_list": [{
                    "node_type": "set",
                    "id": 123
                  }],
                  "dynamic_group_list": [{
                    "id": "07f99504-7bcb-11eb-980b-5254008ed702"
                  }]
                }
              },
              "account": {
                "id": 1,
                "name": "root"
              },
              "file_source_id": 1
            }
          ],
          "file_destination": {
            "path": "/tmp",
            "account": {
              "id": 1,
              "name": "root"
            },
            "server": {
              "variable": null,
              "ip_list": [{
                "bk_host_id": 1,
                "bk_cloud_id": 0,
                "bk_cloud_name": "Default Area",
                "ip": "192.168.1.1",
                "ipv6": null,
                "bk_agent_id": "020000000052540079809816967513755991",
                "alive": 1
              }],
              "topo_node_list": [{
                "node_type": "set",
                "id": 123
              }],
              "dynamic_group_list": [{
                "id": "07f99504-7bcb-11eb-980b-5254008ed702"
              }]
            }
          },
          "timeout": 1000,
          "source_speed_limit": 10,
          "destination_speed_limit": 10,
          "transfer_mode": 2,
          "is_ignore_error": 0
        },
        "approval_info": {
          "approval_message": "Pass"
        }
    }
}
```
### 返回结果说明

| 字段          | 类型    | 是否一定存在 | 描述      | 
|--------------|--------|------------|-------------------------------|
| result       | bool   |  是        |请求成功与否。true:请求成功；false请求失败 |
| code         | int    |  是        |错误编码。 0表示success，>0表示失败错误 |
| message      | string |  是        |请求失败返回的错误信息|
| data         | object |  否        |请求返回的数据|
| permission   | object |  否        |权限信息|

##### data

| 字段                  | 类型      | 是否一定存在 |描述      |
|----------------------|-----------|------------|-----------|
| id                   | long      |  是        | 作业步骤ID |
| name                 | string    |  是        | 步骤名称 |
| type                 | int       |  是        | 步骤类型：1-脚本，2-文件，3-人工确认 |
| script_info          | object    |  否        | 脚本步骤信息 |
| file_info            | object    |  否        | 文件步骤信息 |
| approval_info        | object    |  否        | 审批步骤信息 |


##### script_info

| 字段                | 类型      | 是否一定存在 | 描述      |
|--------------------|-----------|------------|-----------|
| script_type        | int       |  是        | 脚本类型：1-本地脚本，2-引用业务脚本，3-引用公共脚本 |
| script_id          | string    |  否        | 脚本ID |
| script_version_id  | long      |  否        | 脚本版本ID |
| script_content     | string    |  否        | 脚本内容 |
| script_language    | int       |  是        | 脚本语言：1-shell，2-bat，3-perl，4-python，5-powershell，6-sql |
| script_param       | string    |  是        | 脚本参数 |
| script_timeout     | int       |  是        | 脚本超时时间，单位为秒 |
| account            | object    |  是        | 执行账号，详情见account对象定义   |
| server             | object    |  是        | 执行目标机器，详情见server对象定义  |
| is_param_sensitive | int       |  是        | 参数是否为敏感参数：0-不敏感，1-敏感 |
| is_ignore_error    | int       |  是        | 是否忽略错误：0-不忽略，1-忽略 |

##### account

| 字段                | 类型      | 是否一定存在 | 描述      |
|--------------------|-----------|------------|-----------|
| id                 | long      |  是        | 账号ID   |
| name               | string    |  否        | 账号名称  |

##### server

| 字段                  | 类型                  | 是否一定存在 | 描述       |
|----------------------|----------------------|------------|-----------|
| variable             | string               |  否        | 引用的全局变量名称 |
| ip_list              | list<host>           |  否        | 主机列表，元素详情见host对象定义 |
| topo_node_list       | list<topo_node>      |  否        | 拓扑节点列表，元素详情见topo_node对象定义 |
| dynamic_group_list   | list<dynamic_group>  |  否        | 动态分组列表，元素详情见dynamic_group对象定义 |

##### host

| 字段           | 类型       | 是否一定存在 | 描述      |
|---------------|-----------|------------|-----------|
| bk_host_id    | long      |  是        | 主机ID     |
| bk_cloud_id   | long      |  否        | 管控区域ID  |
| bk_cloud_name | string    |  否        | 管控区域名称 |
| ip            | string    |  否        | IP        |
| ipv6          | string    |  否        | IPv6      |
| bk_agent_id   | string    |  否        | Agent ID  |
| alive         | int       |  否        | Agent是否正常，取值为：1-正常，0-异常 |

##### topo_node

| 字段         | 类型       | 是否一定存在 | 描述      |
|-------------|-----------|------------|-----------|
| node_type   | string    |  是        | 动态topo节点类型，对应CMDB API中的 bk_obj_id，例如module、set等     |
| id          | int       |  是        | 动态topo节点ID，对应CMDB API中的 bk_inst_id        |

##### dynamic_group

| 字段         | 类型       | 是否一定存在 | 描述          |
|-------------|-----------|------------|---------------|
| id          | string    |  是        | CMDB动态分组ID |


##### file_info

| 字段                     | 类型               | 是否一定存在 | 描述       |
|-------------------------|-------------------|------------|-----------|
| file_source_list        | list<file_source> |  是        | 源文件列表，元素详情见file_source对象定义 |
| file_destination        | object            |  是        | 目标信息，详情见file_destination对象定义 |
| timeout                 | int               |  是        | 超时，单位为秒 |
| source_speed_limit      | int               |  否        | 上传文件限速，单位为MB/s，没有值表示不限速 |
| destination_speed_limit | int               |  否        | 下载文件限速，单位为MB/s，没有值表示不限速 |
| transfer_mode           | int               |  是        | 传输模式： 1-严谨模式，2-强制模式，3-安全模式 |
| is_ignore_error         | int               |  是        | 是否忽略错误：0-不忽略，1-忽略 |

##### file_source

| 字段               | 类型         | 是否一定存在 | 描述       |
|-------------------|--------------|------------|-----------|
| file_type         | int          |  是        | 文件类型：1-服务器文件，2-本地文件，3-文件源文件 |
| file_list         | list<string> |  是        | 文件路径列表 |
| server            | object       |  否        | 源文件所在机器，详情见server对象定义  |
| account           | object       |  是        | 执行账号，详情见account对象定义  |
| file_source_id    | long         |  否        | 第三方文件源ID |

##### file_destination

| 字段             | 类型      | 是否一定存在 | 描述      |
|-----------------|-----------|------------|-----------|
| path            | string    |  是        | 目标路径   |
| account         | object    |  是        | 执行账号，详情见account对象定义  |
| server          | object    |  是        | 分发目标机器，详情见server对象定义  |


##### approval_info

| 字段               | 类型       | 是否一定存在 | 描述      |
|-------------------|-----------|------------|-----------|
| approval_message  | string    |  是        | 确认消息    |
