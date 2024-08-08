### Function Description

Query the latest service version number of the Job

### Request Parameters

#### Interface parameters

null

### Example of request

- GET

```json
/api/v3/get_latest_service_version
```

### Example of response

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

### Return Result Description

#### response

| Fields     | Type   | Description                                                                 |
|------------|--------|-----------------------------------------------------------------------------|
| result     | bool   | Request success or failure. true: Request successful; false: Request failed |
| code       | int    | Error code. 0 means SUCCESS, >0 means FAIL                                  |
| message    | string | Error message                                                               |
| data       | object | Data returned by request                                                    |
| permission | object | Permission information                                                      |
| request_id | string | Request chain id                                                            |

##### data

| Fields  | Type   | Description                      |
|---------|--------|----------------------------------|
| version | string | Latest version number of service |
