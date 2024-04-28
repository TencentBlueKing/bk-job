### Function Description

Query the latest service version

### Request Parameters

{{ common_args_desc }}

#### Interface parameters

null

### Example of request

```json
{
    "bk_app_code": "esb_test",
    "bk_app_secret": "xxx",
    "bk_token": "xxx",
    "bk_username": "admin"
}
```

### Example of responses

```json
{
    "result": true,
    "code": 0,
    "message": "success",
    "data": {
        "version": "3.6.4"
    }
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
|  version  |  string   |The latest version number of the service|
