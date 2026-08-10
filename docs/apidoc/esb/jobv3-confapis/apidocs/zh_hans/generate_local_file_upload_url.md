### 功能描述

生成本地文件上传URL。本接口是**本地文件分发**流程的第一步：传入待分发的文件名列表，为每个文件返回带凭据的上传地址（upload_url）与分发路径（path）。

本地文件分发的完整流程分为三步：

1. 调用本接口，得到每个文件的 upload_url 与 path。
2. 用 **HTTP PUT** 方法把文件内容上传到对应的 upload_url，详见下方「上传文件到 upload_url」。
3. 调用快速分发文件接口（fast_transfer_file），源文件类型选择本地文件，并填入本接口返回的 path。

### 请求参数

{{ common_args_desc }}

#### 接口参数

| 字段                        |  类型      | 必选   |  描述       |
|----------------------------|------------|--------|------------|
| bk_scope_type | string | 是     | 资源范围类型。可选值: biz - 业务，biz_set - 业务集 |
| bk_scope_id | string | 是 | 资源范围ID, 与bk_scope_type对应, 表示业务ID或者业务集ID |
| file_name_list             |  string[]  | 是     | 要上传的文件名列表 |


### 请求参数示例

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

### 返回结果示例

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

### 返回结果参数说明

#### response
| 字段      | 类型      | 描述      |
|-----------|-----------|-----------|
| result       | bool   | 请求成功与否。true:请求成功；false请求失败 |
| code         | int    | 错误编码。 0表示success，>0表示失败错误 |
| message      | string | 请求失败返回的错误信息|
| data         | object | 请求返回的数据|
| permission   | object | 权限信息|


#### data

| 字段      | 类型      |字段是否一定存在  | 描述      |
|-----------|----------|---------------|---------|
| url_map   | map      |  是           | key为请求中传入的文件名，value为该文件的上传与分发信息 |

##### url_map 的 value

| 字段      | 类型      |字段是否一定存在  | 描述      |
|-----------|----------|---------------|---------|
| upload_url | string  |  是           | 带凭据（token）的文件上传地址，须用 HTTP PUT 方法将文件内容上传到该地址 |
| path       | string  |  是           | 分发该文件时传给快速分发文件接口（fast_transfer_file）源文件路径列表的路径 |

### 上传文件到 upload_url

取得 upload_url 后，用 **HTTP PUT** 方法将文件内容作为请求体上传，请求头 Content-Type 固定为 application/octet-stream。upload_url 中已包含鉴权凭据 token，上传请求**无需**再附加接口鉴权信息。

```bash
curl -X PUT \
  -H "Content-Type: application/octet-stream" \
  --data-binary @/path/to/file1.txt \
  "http://bkrepo.example.com/generic/temporary/upload/bkjob/localupload/1/008f821f-259b-4f62-bd84-1e89d6f05f0d/admin/file1.txt?token=xxx"
```

请注意区分两步所用的方法：调用本接口生成上传地址用 POST，上传文件内容到 upload_url 用 PUT。

上传成功后，把该文件的 path 填入快速分发文件接口的源文件路径列表，即可将其分发到目标机器。upload_url 带有有效期，请在生成后尽快完成上传。

### 上传后文件的有效期

本地文件上传后存在过期时间（当前为 7 天），过期文件会被定时清除。如果需要持久化存储，请将文件存储于业务自己的服务器或蓝鲸制品库仓库，并使用服务器文件分发或文件源文件分发。
