### 功能描述

脚本检测。

### 请求参数

{{ common_args_desc }}

#### 接口参数

| 字段            | 类型   | 必选 | 描述                                                         |
| --------------- | ------ | ---- | ------------------------------------------------------------ |
| script_language | int    | 是   | 脚本语言列表:1 - shell, 2 - bat, 3 - perl, 4 - python, 5 - powershell, 6 - sql |
| content         | string | 是   | 脚本内容，需Base64编码                                                     |


### 请求参数示例

```json
{
    "bk_app_code": "esb_test",
    "bk_app_secret": "xxx",
    "bk_token": "xxx",
    "script_language": [1],
    "content": "cm0gLXJmIC8="
}
```

### 返回结果示例

```json
{
    "code": 0,
    "result": true,
    "data": [
        {
            "line": 1,
            "lineContent": "rm /tmp",
            "matchContent": "rm /tmp",
            "level": 1,
            "action": null,
            "code": "103701001",
            "description": "脚本首行没有定义合法的脚本类型，例如： #!/bin/bash"
        },
        {
            "line": 1,
            "line_content": "rm /tmp",
            "matchContent": "rm",
            "level": 3,
            "action": 2,
            "code": null,
            "description": "dangerous！！！"
        }
     ]
}
```

### 返回结果参数说明

#### response

| 字段       | 类型   | 描述                                       |
| ---------- | ------ | ------------------------------------------ |
| result     | bool   | 请求成功与否。true:请求成功；false请求失败 |
| code       | int    | 错误编码。 0表示success，>0表示失败错误    |
| message    | string | 请求失败返回的错误信息                     |
| data       | object | 请求返回的数据                             |
| permission | object | 权限信息                                   |

#### data

| 字段          | 类型   | 描述                                   |
| ------------- | ------ | -------------------------------------- |
| line          | int    | 错误所在行数                           |
| line_content  | string | 脚本所在行的内容                       |
| match_content | string | 匹配的内容                             |
| level         | int    | 错误级别：1 - 警告，2 - 错误，3 - 致命 |
| action        | int    | 处理动作：1 - 扫描，2 - 拦截           |
| description   | string | 检查项描述                             |
| code          | string | 错误代码                               |
