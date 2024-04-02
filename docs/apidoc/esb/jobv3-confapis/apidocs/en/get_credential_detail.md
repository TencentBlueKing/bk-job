### Function Description

Query credential details

### Request Parameters

{{ common_args_desc }}

#### Interface parameters

| Fields |  Type  | Required | Description |    
| ------------- | ------ | ---- | ------------------------------------------------------- |
| bk_scope_type | string | yes   | Resource scope type. Optional values: biz - Business，biz_set - Business Set      |
| bk_scope_id   | string | yes   | Resource scope ID. Corresponds to bk_scope_type, which means business ID or business set ID |
| id          | String | yes   | Credential ID                                              |

### Example of request

```json
{
    "bk_app_code": "esb_test",
    "bk_app_secret": "xxx",
    "bk_token": "xxx",
    "bk_scope_type": "biz",
    "bk_scope_id": "1",
    "id": "xxx"
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
        "id": "xxx",
        "name": "xxx",
        "type": "xxx",
        "description": "xxx",
        "creator": "admin",
        "create_time": 1712049204719,
        "last_modify_user": "admin",
        "last_modify_time": "1712049204719"
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
| id               | string | Credential ID                                                       |
| name             | string | Credential name                                                     |
| type             | string | Type. Optional values：APP_ID_SECRET_KEY,PASSWORD,USERNAME_PASSWORD,SECRET_KEY|
| description      | string | Description                                                         |
| creator          | string | Creator                                               |
| create_time      | long   | Creation time Unix timestamp (ms)                                        |
| last_modify_user | string | Last modify user                                               |
| last_modify_time | long   | Last modified time Unix timestamp (ms)                                    |
