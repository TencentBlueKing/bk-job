### Function Description

Update account (currently only system accounts are supported)

### Request Parameters

{{ common_args_desc }}

#### Interface parameters

| Fields        | Type   | Required | Description                                                  |
| ------------- | ------ | -------- | ------------------------------------------------------------ |
| bk_scope_type | string | yes      | Resource scope type. Optional values: biz - Business，biz_set - Business Set |
| bk_scope_id   | string | yes      | Resource scope ID. Corresponds to bk_scope_type, which means business ID or business set ID |
| id            | long   | yes      | Account ID |
| password      | string | no       | Windows system account password |
| alias         | string | no       | Account alias |
| description   | string | no       | Account description |


### Example of request

```json
{
    "bk_app_code": "esb_test",
    "bk_app_secret": "xxx",
    "bk_token": "xxx",
    "bk_scope_type": "biz",
    "bk_scope_id": "1",
    "id": 100,
    "password": "testPwd",
    "alias": "testAlias",
    "description": "update account for windows"
}
```

### Example of responses

```json
{
    "code": 0,
    "result": true,
    "data": {
        "id": 100,
        "bk_scope_type": "biz",
        "bk_scope_id": "1",
        "account": "test",
        "alias": "testAlias",
        "category": 1,
        "type": 2,
        "os": "Windows",
        "description": "update account for windows",
        "creator": "admin",
        "create_time": 1715584657584,
        "last_modify_user": "admin",
        "last_modify_time": 1715584681063
    },
    "job_request_id": "0126bf577c82344bc8167d46764d8c83"
}
```

### Response Description

#### response

| Fields     | Type   | Description                                                  |
| ---------- | ------ | ------------------------------------------------------------ |
| result     | bool   | Whether the request succeeded or not. True: request succeeded;False: request failed |
| code       | int    | Error code. 0 indicates success, >0 indicates failure        |
| message    | string | Error message                                                |
| data       | object | Data returned by request                                     |
| permission | object | Permission information                                       |

#### data

| Fields           | Type   | Description                                                  |
| ---------------- | ------ | ------------------------------------------------------------ |
| id               | string | Account ID                                                    |
| bk_scope_type    | string | Resource scope type. Optional values: biz - Business，biz_set - Business Set |
| bk_scope_id      | string | Resource scope ID. Corresponds to bk_scope_type, which means business ID or business set ID |
| account          | string | Account name |
| alias            | string | Account alias |
| category         | int    | Account usage (1: system account) |
| type             | int    | Account type (1: Linux, 2: Windows）|
| os               | string | This field takes effect when the account is used as a system account. The OS corresponding to the account |
| description      | string | Account description  |
| creator          | string | Creator                                                      |
| create_time      | long   | Created time, Unix timestamp                                 |
| last_modify_user | string | Last modify user                                             |
| last_modify_time | long   | Last modified time, Unix timestamp                           |
| description      | string | Description                                                  |
