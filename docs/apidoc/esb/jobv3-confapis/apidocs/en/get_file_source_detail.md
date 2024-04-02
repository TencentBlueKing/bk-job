### Function Description

Query file source details

### Request Parameters

{{ common_args_desc }}

#### Interface parameters

| Fields |  Type  | Required | Description |                                                
| ------------- | ------ | ---- | ------------------------------------------------------- |
| bk_scope_type | string | yes   | Resource scope type. Optional values: biz - Business，biz_set - Business Set      |
| bk_scope_id   | string | yes   | Resource scope ID. Corresponds to bk_scope_type, which means business ID or business set ID |
| code          | String | yes   | File source code                                              |

### Example of request

```json
{
    "bk_app_code": "esb_test",
    "bk_app_secret": "xxx",
    "bk_token": "xxx",
    "bk_scope_type": "biz",
    "bk_scope_id": "1",
    "code": "xxx"
}
```

### Example of responses

```json
{
    "result": true,
    "code": 0,
    "message": "",
    "data": {
        "bk_scope_type": "biz",
        "bk_scope_id": "2",
        "id": 0,
        "code": "xxx",
        "alias": "xxx",
        "status": 0,
        "file_source_type": 3,
        "is_public": false,
        "credential_id": "xxx",
        "enable": true,
        "creator": "admin",
        "create_time": 1712050614742,
        "last_modify_user": "admin",
        "last_modify_time": 1712050614742
    }
}
```

### Response Description

#### response

| Fields | Type  | Description |
|-----------|-----------|-----------|
| result       |  bool   | Whether the request was successful or not. True: request succeeded;False: request failed|
| code         |  int    | Error code. 0 indicates success, >0 indicates failure|
| message      |  string |Error message|
| data         |  object |Data returned by request|
| permission   |  object |Permission information|
| request_id   |  string |Request chain id|


#### data

| Fields           | Type   | Description                                                         |
| ---------------- | ------ | ------------------------------------------------------------ |
| bk_scope_type    | string | Resource scope type. Optional values: biz - Business，biz_set - Business Set |
| bk_scope_id      | string | Resource scope ID. Corresponds to bk_scope_type, which means business ID or business set ID |
| id               | int    | File source ID                                                     |
| code             | string | File source code                                                   |
| alias            | string | File source alias                                                   |
| status           | int    | Status                                                         |
| file_source_type | int    | File source type.Optional values: blueking artifactory |
| is_public        | bool   | Is it a public file source                                             |
| credential_id    | string | Credential ID                                                       |
| enable           | bool   | Enable                                                     |
| creator          | string | Creator                                               |
| create_time      | long   | Creation time Unix timestamp (ms)                                        |
| last_modify_user | string | Last modify user                                               |
| last_modify_time | long   | Last modified time Unix timestamp (ms)                                    |
