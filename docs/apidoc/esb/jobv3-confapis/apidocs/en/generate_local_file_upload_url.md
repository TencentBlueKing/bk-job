### Function Description

Generate the local file upload URL. This API is the first step of the **local file distribution** process: pass in the list of file names to be distributed, and it returns the upload address with credentials (upload_url) and the distribution path (path) for each file.

The complete local file distribution process consists of three steps:

1. Call this API to get the upload_url and path of each file.
2. Upload the file content to the corresponding upload_url with the **HTTP PUT** method. See "Upload the file to upload_url" below.
3. Call the fast transfer file API (fast_transfer_file), choose local file as the source file type, and fill in the path returned by this API.

### Request Parameters

{{ common_args_desc }}

#### Interface parameters

| Fields                  |  Type  | Required | Description |
|----------------------------|------------|--------|------------|
| bk_scope_type | string | yes  | Resource scope type. Optional values: biz - Business，biz_set - Business Set |
| bk_scope_id | string | yes | Resource scope ID. Corresponds to bk_scope_type, which means business ID or business set ID |
| file_name_list             | string []  | yes  |List of file names to upload|


### Example of request

```json
{
    "bk_app_code": "esb_test",
    "bk_app_secret": "xxx",
    "bk_token": "xxx",
    "bk_scope_type": "biz",
    "bk_scope_id": "1",
    "file_name_list": [
        "file1.txt",
        "file2.txt"
    ]
}
```

### Example of responses

```json
{
    "result": true,
    "code": 0,
    "message": "success",
    "data": {
        "url_map": {
            "file1.txt": {
                "upload_url": "http://bkrepo.example.com/generic/temporary/upload/bkjob/localupload/1/008f821f-259b-4f62-bd84-1e89d6f05f0d/admin/file1.txt?token=xxx",
                "path": "1/008f821f-259b-4f62-bd84-1e89d6f05f0d/admin/file1.txt"
            },
            "file2.txt": {
                "upload_url": "http://bkrepo.example.com/generic/temporary/upload/bkjob/localupload/1/008f821f-259b-4f62-bd84-1e89d6f05f0d/admin/file2.txt?token=xxx",
                "path": "1/008f821f-259b-4f62-bd84-1e89d6f05f0d/admin/file2.txt"
            }
        }
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

| Fields | Type  |Whether the field must exist  | Description |
|-----------|----------|---------------|---------|
| url_map   |  map      |   yes     | The key is the file name passed in the request, and the value is the upload and distribution information of the file|

##### The value of url_map

| Fields | Type  |Whether the field must exist  | Description |
|-----------|----------|---------------|---------|
| upload_url |  string  |   yes     | File upload address with credentials (token). Upload the file content to it with the HTTP PUT method|
| path       |  string  |   yes     | The path to fill in the source file path list of the fast transfer file API (fast_transfer_file)|

### Upload the file to upload_url

After obtaining the upload_url, upload the file content as the request body with the **HTTP PUT** method, and set the Content-Type header to application/octet-stream. The upload_url already contains the credential token, so the upload request does **not** need any additional API authentication information.

```bash
curl -X PUT \
  -H "Content-Type: application/octet-stream" \
  --data-binary @/path/to/file1.txt \
  "http://bkrepo.example.com/generic/temporary/upload/bkjob/localupload/1/008f821f-259b-4f62-bd84-1e89d6f05f0d/admin/file1.txt?token=xxx"
```

Note the difference between the methods used in the two steps: use POST to call this API to generate the upload address, and use PUT to upload the file content to the upload_url.

After the upload succeeds, fill the path of the file into the source file path list of the fast transfer file API to distribute it to the target hosts. The upload_url has an expiration time, so please complete the upload as soon as possible after generating it.

### Expiration of the uploaded file

An uploaded local file has an expiration time (currently 7 days), and expired files are removed by a scheduled cleanup task. If you need persistent storage, store the file on your own business server or in the BlueKing Repository, and distribute it with server file distribution or file source file distribution instead.
