### 功能描述

获取主机账号的密码的加密信息

### 请求参数

#### Query参数

无

### 请求参数示例

- GET

```json
/api/v3/get_account_password_encryption_metadata?bk_scope_type=biz&bk_scope_id=1
```

### 返回结果示例

```json
{
    "code": 0,
    "result": true,
    "data": {
        "public_key": "xxx",
        "encrypt_algorithm": "RSA"
    },
    "job_request_id": "xxx"
}
```

### 返回结果说明

#### response

| 字段             | 类型     | 是否一定存在 | 描述                         |
|----------------|--------|--------|----------------------------|
| result         | bool   | 是      | 请求成功与否。true:请求成功；false请求失败 |
| code           | int    | 是      | 错误编码。 0表示success，>0表示失败错误  |
| message        | string | 否      | 请求失败返回的错误信息                |
| data           | object | 否      | 请求返回的数据，删除操作可能没有值          |
| job_request_id | string | 否      | 请求ID，请求唯一标识                |
| permission     | object | 否      | 无权限返回的权限信息                 |

##### data

| 字段                | 类型     | 是否一定存在 | 描述                                       |
|-------------------|--------|--------|------------------------------------------|
| public_key        | string | 是      | 公钥（Base64编码）                             |
| encrypt_algorithm | string | 是      | 加密算法，支持经典密码算法（RSA、AES）和国家商用密码算法（SM2、SM4） |
