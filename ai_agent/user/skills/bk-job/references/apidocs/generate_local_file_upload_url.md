### 功能描述

生成本地文件上传 URL。这是**本地文件分发**流程的第一步：传入要分发的文件名列表，返回每个文件的**带凭据上传地址**（`upload_url`）与**分发路径**（`path`）。

本地文件分发完整流程：

1. 调用本接口 `generate_local_file_upload_url`，得到 `url_map[文件名] = {upload_url, path}`。
2. 将本地文件以 **HTTP PUT**（`Content-Type: application/octet-stream`，文件原始字节作为请求体，等价于 `curl -X PUT --data-binary @<file>`）上传到对应的 `upload_url`（作业平台为本次分发分配的临时上传地址）。
3. 调用 `fast_transfer_file`，在 `file_source_list` 中以 `file_type=2`（本地文件）引用步骤 1 返回的 `path`，见 [`fast_transfer_file.md`](fast_transfer_file.md)。

### 请求参数

{{ bkapi_authorization_description }}

#### Body参数

| 字段            | 类型       | 必选 | 描述                                          |
|---------------|----------|----|---------------------------------------------|
| bk_scope_type | string   | 是  | 资源范围类型。可选值：`biz`（业务）、`biz_set`（业务集）         |
| bk_scope_id   | string   | 是  | 资源范围 ID，与 `bk_scope_type` 对应，表示业务 ID 或业务集 ID |
| file_name_list | string[] | 是  | 要上传的文件名列表（仅文件名，用于生成上传地址与分发路径）              |

### 请求参数示例

- POST `/api/v3/generate_local_file_upload_url`

```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "1",
    "file_name_list": ["file1.txt", "file2.txt"]
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

| 字段         | 类型     | 是否一定不为null | 描述                         |
|------------|--------|------------|----------------------------|
| result     | bool   | 是          | 请求成功与否。true:请求成功；false请求失败 |
| code       | int    | 是          | 错误编码。 0表示success，>0表示失败错误  |
| message    | string | 否          | 请求失败返回的错误信息                |
| data       | object | 否          | 请求返回的数据                    |
| permission | object | 否          | 无权限返回的权限信息                 |

#### data

| 字段      | 类型  | 是否一定存在 | 描述                                                            |
|---------|-----|--------|---------------------------------------------------------------|
| url_map | map | 是      | key 为传入的文件名；value 含 `upload_url`（带凭据的上传地址）与 `path`（分发时传给 `fast_transfer_file` 的路径） |

##### url_map 的 value

| 字段         | 类型     | 是否一定存在 | 描述                                                     |
|------------|--------|--------|--------------------------------------------------------|
| upload_url | string | 是      | 带凭据（token）的文件上传地址，用 HTTP PUT（`--data-binary`）将本地文件字节上传到此地址   |
| path       | string | 是      | 分发该文件时传给 `fast_transfer_file` 源文件（`file_type=2`）`file_list` 的路径 |

### 后续步骤：把文件上传到该地址

拿到 `upload_url` 后，用 **HTTP PUT** 把文件原始字节作为请求体上传，`Content-Type` 固定为 `application/octet-stream`。`upload_url` 已自带 `token` 凭据，**不要**再附加 APIGW 鉴权头：

```bash
curl -X PUT \
  -H "Content-Type: application/octet-stream" \
  --data-binary @/path/to/your/file1.txt \
  "http://bkrepo.example.com/generic/temporary/upload/bkjob/localupload/1/008f821f-259b-4f62-bd84-1e89d6f05f0d/admin/file1.txt?token=xxx"
```

注意区分两步的方法：**生成上传地址用 POST**（即本接口），**上传文件字节用 PUT**。

上传成功后，取同一文件的 `path` 填入 `fast_transfer_file` 中 `file_type=2` 源文件的 `file_list`，完成分发。

### 上传后文件的有效期（回答用户时须主动告知）

本地文件上传后存在过期时间（**当前为 7 天**），过期文件会被定时清除。因此本地文件分发只适合一次性分发，**不可**当作长期存放文件的手段。

用户若需要长期反复分发同一个文件，建议把文件放到业务内某台机器上，改用**服务器文件分发**（`file_type=1`）。**不要**把「用制品库/文件源里的文件」作为可选方案给用户——本技能只有服务器文件与本地文件两种来源。
