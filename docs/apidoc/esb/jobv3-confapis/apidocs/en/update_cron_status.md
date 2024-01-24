### Function Description

Update Cron job status, such as started or paused

### Request Parameters

{{ common_args_desc }}

#### Interface parameters

| Fields  |  Type  | Required | Description |
|----------- |------------|--------|------------|
| bk_scope_type | string | yes  | Resource scope type. Optional values: biz - Business，biz_set - Business Set |
| bk_scope_id | string | yes | Resource scope ID. Corresponds to bk_scope_type, which means business ID or business set ID |
| id         |   long      |  yes |Cron job ID|
| status     |   int       |  yes  |State, 1. Start, 2. Paused|
| return_cron_detail |  boolean  | no | Whether to return cron details.The default value is false. |

### Example of request

```json
{
    "bk_app_code": "esb_test",
    "bk_app_secret": "xxx",
    "bk_token": "xxx",
    "bk_scope_type": "biz",
    "bk_scope_id": "1",
    "id": 2,
    "status": 1
}
```

### Example of responses

```json
{
    "result": true,
    "code": 0,
    "message": "success",
    "data": 2
}
```

### Response Description

#### response
| Fields | Type  | Description |
|-----------|-----------|-----------|
| result       |  bool   | Whether the request succeeded or not. True: request succeeded;False: request failed|
| code         |  int    | Error code. 0 indicates success, >0 indicates failure|
| message      |  string |Error message|
| data         |  object |Data returned by request|
| permission   |  object |Permission information|
| request_id   |  string |Request chain id|

#### data

| Fields | Type  | Description |
|-----------|-----------|-----------|
| job_plan_id      |  long      | Job Plan ID |
| id               |  long      | Cron job ID |
| name             |  string    | Cron job name |
| status           |  int       | Cron job status: 1. Started, 2. Paused |
| expression       |  string    | Timing rules for Cron Job crontab, required when creating, optional when modifying. The meaning of each field is: minute hour day month week, for example: 0/5 * * * * ? means execute every 5 minutes |
| global_var_list |  array     | Global variable information|
| creator          |  string    | Cron job creator|
| create_time      |  long      | Creation time, Unix timestamp|
| last_modify_user | string    | Cron job modifier|
| last_modify_time | long      | Last modified time, Unix timestamp|

#### global_var

| Fields |  Type | Description |
|-----------|-----------|------------|
| id        |   long     | Global variable id, unique identification. If the id is empty, then name is used as the unique identification|
| name      |   string   | Global variable name|
| value     |   string   | Character, password, value of global variable of array type|
| server    |   object   | Host type global variable value|

#### server
| Fields             | Type | Description |
|-----------------------|-------|------------|
| ip_list               |  array |Static IP list|
| dynamic_group_list | array |Dynamic grouping list|
| topo_node_list        |  array |Dynamic topo node list|

#### ip

| Fields   | Type | Description |
|-------------|---------|---------|
| bk_cloud_id |  int    | BK-Net ID |
| ip          |  string | IP Address |

#### topo_node
| Fields        |  Type  | Description |
|------------------|--------|------------|
| id               |  long   | Dynamic topo node ID, corresponding to bk_inst_id in CMDB API|
| node_type        |  string |Dynamic topo node type, corresponding to bk_obj_id in CMDB API, such as "module" and "set"|
