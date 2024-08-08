### Function Description

Quick File transfer

### Request Parameters

#### Interface parameters

| Fields               | Type   | Required | Description                                                                                                                                                                                  |
|----------------------|--------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| bk_scope_type        | string | yes      | Resource scope type. Optional values: biz - Business，biz_set - Business Set                                                                                                                  |
| bk_scope_id          | string | yes      | Resource scope ID. Corresponds to bk_scope_type, which means business ID or business set ID                                                                                                  |
| account_alias        | string | no       | Target execution account alias, available from the account page, recommended. When both account_alias and account_id exist, account_id takes precedence.                                     |
| account_id           | long   | no       | Target execution account ID, available from the get_account_List api. When both account_alias and account_id exist, account_id takes precedence.                                             |
| file_target_path     | string | yes      | File transfer destination path                                                                                                                                                               |
| file_source_list     | array  | yes      | File source object array, see file_source Definition below                                                                                                                                   |
| timeout              | int    | no       | Task timeout in seconds, default is 7200. Value range 1 86400.                                                                                                                               |
| download_speed_limit | int    | no       | Download speed limit in MB. If this parameter is not passed in, it means no speed limit                                                                                                      |
| upload_speed_limit   | int    | no       | Upload speed limit, in MB. If this parameter is not passed in, it means no speed limit                                                                                                       |
| transfer_mode        | int    | no       | Transmission mode. 1 - Strict mode, 2 - Forced mode. Force mode is used by default                                                                                                           |
| target_server        | object | no       | Target server, see server definition                                                                                                                                                         |
| callback_url         | string | no       | Callback URL, when the task execution is completed, the JOB will call this URL to inform the task execution result. Callback protocol refer to the callback_protocol component documentation |
| rolling_config       | object | no       | Rolling config，see rolling_config definition                                                                                                                                                 |

##### file_source

| Fields           | Type   | Required | Description                                                                                                                                                                                                                                                                                                            |
|------------------|--------|----------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| file_list        | array  | yes      | Support multiple files. If the file source type is a server file, fill in the absolute path array of the source file; If the file source type is a third-party file source, the path filled in for the COS file source is "bucket name/file path", for example: testbucket/test.txt                                    |
| account          | object | yes      | The file source account, as defined in the account, is required when the file source type is server file source, and does not need to be filled in when the file source type is third-party file source                                                                                                                |
| server           | object | no       | Source server，see server definition                                                                                                                                                                                                                                                                                    |
| file_type        | int    | no       | File source type, 1: server file, 3: third-party file source file, default is 1 if not transmitted                                                                                                                                                                                                                     |
| file_source_id   | int    | no       | When file_date is 3, choose one of file_stource_id and file_stource_comde to fill in. If both are filled in, priority should be given to using file_stource_id and the third-party file source ID. You can obtain the details of the steps in the returned result from the get-job_detail interface                    |
| file_source_code | string | no       | When file_date is 3, choose one of file_stource_id and file_stource_comde to fill in. If both are filled in, priority should be given to using file_stource_id, the third-party file source identifier, which can be obtained from the file distribution page of the job platform ->select file source file pop-up box |

##### account

| Fields | Type   | Required | Description                                                                                                                                                                |
|--------|--------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| id     | long   | no       | The source execution account ID can be obtained from the get-account list interface. There must be one with alias. When both alias and id exist, id takes priority.        |
| alias  | string | no       | Source execution account alias, can be obtained from the account page, recommended for use. There must be one with alias. When both alias and id exist, id takes priority. |

##### server

| Fields             | Type  | Required | Description                                                                                                                                                                                              |
|--------------------|-------|----------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| host_id_list       | array | no       | Host ID list                                                                                                                                                                                             |
| ip_list            | array | no       | Static IP list, see ip for definition. ***Deprecated, it is recommended to use the host_id_list parameter***; if host_id_list and ip_list exist at the same time, the ip_list parameter will be ignored. |
| dynamic_group_list | array | no       | Dynamic grouping list, see dynamic_group for definition                                                                                                                                                  |
| topo_node_list     | array | no       | Dynamic topo node list, see topo_node for definition                                                                                                                                                     |

##### ip

| Fields      | Type   | Required | Description |
|-------------|--------|----------|-------------|
| bk_cloud_id | long   | yes      | BK-Net ID   |
| ip          | string | yes      | IP Address  |

##### dynamic_group

| Fields | Type   | Required | Description              |
|--------|--------|----------|--------------------------|
| id     | string | yes      | CMDB dynamic grouping ID |

##### topo_node_list

| Fields    | Type   | Required | Description                                                                                |
|-----------|--------|----------|--------------------------------------------------------------------------------------------|
| id        | long   | yes      | Dynamic topo node ID, corresponding to bk_inst_id in CMDB API                              |
| node_type | string | yes      | Dynamic topo node type, corresponding to bk_obj_id in CMDB API, such as "module" and "set" |

##### rolling_config

| Fields     | Type   | Required | Description                                                                                                                         |
|------------|--------|----------|-------------------------------------------------------------------------------------------------------------------------------------|
| expression | string | yes      | Rolling strategy expression                                                                                                         |
| mode       | int    | yes      | Rolling mechanism, 1-pause if execution fails; 2- Ignore failure and automatically scroll to the next batch; 3- Manual confirmation |

### 请求参数示例

- POST

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

### 返回结果示例

```json
{
  "result": true,
  "code": 0,
  "message": "success",
  "data": {
    "job_instance_name": "API Quick Distribution File1521101427176",
    "job_instance_id": 10000,
    "step_instance_id": 10001
  }
}
```

### 返回结果参数说明

| 字段         | 类型     | 描述                         |
|------------|--------|----------------------------|
| result     | bool   | 请求成功与否。true:请求成功；false请求失败 |
| code       | int    | 错误编码。 0表示success，>0表示失败错误  |
| message    | string | 请求失败返回的错误信息                |
| data       | object | 请求返回的数据                    |
| permission | object | 权限信息                       |

##### data

| 字段                | 类型   | 描述     |
|-------------------|------|--------|
| job_instance_id   | long | 作业实例ID |
| job_instance_name | long | 作业实例名称 |
| step_instance_id  | long | 步骤实例ID |
