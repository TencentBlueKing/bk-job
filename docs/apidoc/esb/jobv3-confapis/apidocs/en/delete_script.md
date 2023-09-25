### Function Description

Delete script will also delete all script versions under that script

### Request Parameters

{{ common_args_desc }}

#### Interface parameters

| Fields        | Type   | Required | Description                                                  |
| ------------- | ------ | -------- | ------------------------------------------------------------ |
| bk_scope_type | string | yes      | Resource scope type. Optional values: biz - Business，biz_set - Business Set |
| bk_scope_id   | string | yes      | Resource scope ID. Corresponds to bk_scope_type, which means business ID or business set ID |
| script_id     | long   | yes      | Script id                                                    |


### Example of request

```json
{
    "bk_app_code": "esb_test",
    "bk_app_secret": "xxx",
    "bk_token": "xxx",
    "bk_scope_type":"biz",
    "bk_scope_id":"2",
    "script_id": "4a350b0e0707450e93326f6ace921072"
}
```

### Example of responses

```json
{
    "code": 0,
    "result": true
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

null
