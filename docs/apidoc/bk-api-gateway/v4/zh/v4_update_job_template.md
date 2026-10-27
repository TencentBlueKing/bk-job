### 功能描述

更新指定的作业模板。

**请求体描述的是模板的期望终态，不是增量补丁。** 未出现在 `step_list` 中的既有步骤会被删除，未出现在 `global_var_list` 中的既有全局变量会被删除。推荐的用法是先调用 `get_job_template_detail` 读取当前配置，改动后整体写回。

调用前须满足：作业模板存在于该资源范围内；调用身份对该模板具备编辑权限；引用脚本、文件源、账号等资源时，须对这些资源具备相应的查看/使用权限。传入 `name` 时，同一资源范围下的作业模板名称不可重复。

步骤 `id` 的三种情况：

- **传既有步骤 ID**：保留该步骤并按请求内容更新，步骤 ID 不变
- **不传（缺省或 null）**：作为新增步骤处理，由服务端分配 ID
- **不传该步骤**：从模板中删除该步骤

不接受 `-1`、`0` 等非正数；传入不属于本模板的步骤 ID 会返回参数错误并指明具体 ID；同一请求中步骤 ID 不可重复。

全局变量按 `name` 匹配：同名视为更新既有变量，未出现的既有变量会被删除。**不支持改名**，改名等价于「删除旧变量 + 新增变量」，会丢失该变量在派生执行方案中的取值。

`step_list` 的数组顺序即为步骤执行顺序，重排数组即可调整顺序，步骤 ID 不会变化。

模板更新后，由该模板派生的执行方案会被标记为待同步（`need_update=true`），方案本身的配置不会被自动改动；如需让方案跟上模板变更，调用 `sync_job_plan`。

### 请求参数

#### Header参数

| 字段                    | 类型     | 必选 | 描述                                                                                                                               |
|-----------------------|--------|----|----------------------------------------------------------------------------------------------------------------------------------|
| X-Bkapi-Authorization | string | 是  | 认证信息。详情参考[调用网关 API](https://github.com/TencentBlueKing/BKDocs/blob/master/ZH/7.0/APIGateway/apigateway/use-api/use-apigw-api.md) |
| Accept                | string | 是  | 固定值。application/json                                                                                                             |
| Content-Type          | string | 是  | 固定值。application/json                                                                                                             |

#### Body参数

| 字段              | 类型     | 必选 | 描述                                                                                     |
|-----------------|--------|----|----------------------------------------------------------------------------------------|
| bk_scope_type   | string | 是  | 资源范围类型。可选值：`biz`（业务）、`biz_set`（业务集）                                                    |
| bk_scope_id     | string | 是  | 资源范围 ID，须与 `bk_scope_type` 对应，表示业务 ID 或业务集 ID                                          |
| id              | long   | 是  | 作业模板 ID，须大于 0，且对应模板须已存在                                                                |
| name            | string | 否  | 作业模板名称。不传或传空白表示不改名，保留模板原有名称；传入时长度 1～60 个字符，不可包含 HTML 特殊字符 `<`、`>`、`"`、`'`，且在同一资源范围下须唯一 |
| description     | string | 否  | 作业模板描述，最长 500 个字符                                                                      |
| global_var_list | array  | 否  | 全局变量列表；未出现在本列表中的既有变量会被删除；同一请求中变量名不可重复                                                  |
| step_list       | array  | 是  | 作业步骤列表，数组顺序即为步骤执行顺序，不可为空；未出现在本列表中的既有步骤会被删除                                             |

作业模板的标签不在本接口的更新范围内，模板原有标签会被原样保留。

##### global_var_list[] 元素

| 字段             | 类型     | 必选 | 描述                                                                                              |
|----------------|--------|----|-------------------------------------------------------------------------------------------------|
| name           | string | 是  | 变量名称，长度 1～60 个字符；不可包含 HTML 特殊字符 `<`、`>`、`"`、`'`；同一请求中不可重复                                       |
| type           | int    | 是  | 变量类型。可选值：`1`（字符串）、`2`（命名空间）、`3`（执行目标列表）、`4`（密文）、`5`（关联数组）、`6`（索引数组）、`7`（执行账号）                   |
| description    | string | 否  | 变量描述                                                                                            |
| required       | int    | 否  | 执行时是否必填。可选值：`0`（否，默认）、`1`（是）                                                                    |
| value          | string | 否  | 变量默认值。适用于字符串、命名空间、密文、关联数组、索引数组、执行账号类型；执行账号变量的值为账号 ID 字符串。不传即默认值为空，更新时会把原有默认值清掉                  |
| execute_target | object | 否  | 执行目标默认值，仅 `type` 为 3 时填写。结构见下文 `execute_target`，但**不接受 `variable` 字段**：变量的默认值只能是具体目标，不能再引用另一个变量 |

非执行目标类型的变量，`value` 不传就会被置为 null。写接口是全量替换语义，「不传」表示「设为空」而不是「保持原值」，要保留原有默认值请把它原样回传。

密文变量（`type=4`）在 `get_job_template_detail` 中固定返回 `******`。在 `update_job_template` 中把 `******` 原样写回、且变量名不变时，服务端视为未修改，保留该变量的原有取值；要改密文值请填写新的明文。

以下两种情况服务端**无从得知原值**，`******` 会被当作字面量存成新的密文值，请务必填写真实明文：

- 调用 `create_job_template` 时。把 `get_job_template_detail` 的响应直接用于创建副本模板尤其容易踩到。
- 调用 `update_job_template` 且同时修改了密文变量的**名称**时。全局变量按名称匹配（见 `name` 字段说明），改名会被解释为「删除旧变量 + 新增变量」，新变量没有可继承的原值。

这两种情况不会报错，接口正常返回，读回来也仍是 `******`，只有实际执行作业时才会发现取到的是字面量 `******`。


##### step_list[] 元素

| 字段            | 类型     | 必选 | 描述                                                   |
|---------------|--------|----|------------------------------------------------------|
| id            | long   | 否  | 步骤 ID。传既有步骤 ID 表示保留并更新该步骤；缺省表示新增步骤；不接受 `-1`、`0` 等非正数 |
| name          | string | 是  | 步骤名称，长度 1～60 个字符；不可包含 HTML 特殊字符 `<`、`>`、`"`、`'`      |
| type          | int    | 是  | 步骤类型。可选值：`1`（脚本执行）、`2`（文件分发）、`3`（人工确认）               |
| script_info   | object | 否  | 脚本执行步骤详情。`type` 为 1 时必填                              |
| file_info     | object | 否  | 文件分发步骤详情。`type` 为 2 时必填                              |
| approval_info | object | 否  | 人工确认步骤详情。`type` 为 3 时必填                              |

##### script_info

`type` 为 `1`（脚本执行）时必填。

| 字段                  | 类型     | 必选 | 描述                                                                                                                                                    |
|---------------------|--------|----|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| script_type         | int    | 是  | 脚本来源。可选值：`1`（本地脚本）、`2`（引用业务脚本）、`3`（引用公共脚本）                                                                                                            |
| script_id           | string | 否  | 引用的脚本 ID。`script_type` 为 2 或 3 时必填                                                                                                                    |
| script_version_id   | long   | 否  | 引用的脚本版本 ID。`script_type` 为 2 或 3 时必填                                                                                                                  |
| script_content      | string | 否  | 脚本内容，Base64 编码。`script_type` 为 1 时必填                                                                                                                  |
| script_language     | int    | 否  | 脚本语言。`script_type` 为 1 时必填；引用脚本（`script_type` 为 2、3）时无需传入，以被引用版本的语言为准，传入的值会被忽略。可选值：`1`（shell）、`2`（bat）、`3`（perl）、`4`（python）、`5`（powershell）、`6`（sql） |
| script_param        | string | 否  | 脚本参数，Base64 编码                                                                                                                                        |
| windows_interpreter | string | 否  | Windows 解释器路径，仅对 Windows 主机生效                                                                                                                         |
| script_timeout      | long   | 否  | 脚本执行超时时间，单位秒，须大于 0。默认 `7200`                                                                                                                          |
| is_param_sensitive  | int    | 否  | 脚本参数是否为敏感参数。可选值：`0`（否，默认）、`1`（是）                                                                                                                      |
| is_ignore_error     | int    | 否  | 步骤失败时是否忽略错误继续执行后续步骤。可选值：`0`（否，默认）、`1`（是）                                                                                                              |
| account             | object | 是  | 执行账号，见下文 `account`                                                                                                                                    |
| execute_target      | object | 是  | 执行目标，见下文 `execute_target`                                                                                                                             |

##### file_info

`type` 为 `2`（文件分发）时必填。

| 字段                      | 类型     | 必选 | 描述                                                                  |
|-------------------------|--------|----|---------------------------------------------------------------------|
| file_source_list        | array  | 是  | 源文件列表，不可为空                                                          |
| file_destination        | object | 是  | 分发目标                                                                |
| timeout                 | long   | 否  | 文件分发超时时间，单位秒，须大于 0。默认 `7200`                                        |
| transfer_mode           | int    | 否  | 文件传输模式。可选值：`1`（严谨模式）、`2`（强制模式，默认）、`3`（保险模式，源 IP 前缀）、`4`（保险模式，日期前缀）  |
| source_speed_limit      | long   | 否  | 源端上传限速，单位 MB/s，须大于 0。不传表示不限速                                        |
| destination_speed_limit | long   | 否  | 目标端下载限速，单位 MB/s，须大于 0。不传表示不限速                                       |
| is_ignore_error         | int    | 否  | 步骤失败时是否忽略错误继续执行后续步骤。可选值：`0`（否，默认）、`1`（是）                            |

###### file_source_list[] 元素

| 字段             | 类型     | 必选 | 描述                                               |
|----------------|--------|----|--------------------------------------------------|
| file_type      | int    | 是  | 源文件类型。可选值：`1`（服务器文件）、`2`（本地文件）、`3`（文件源文件）        |
| file_list      | array  | 是  | 源文件路径列表，元素为 string，不可为空                          |
| file_source_id | int    | 否  | 文件源 ID。`file_type` 为 3 时必填                       |
| account        | object | 否  | 执行账号，见下文 `account`。`file_type` 为 1 时必填           |
| execute_target | object | 否  | 源文件所在主机，见下文 `execute_target`。`file_type` 为 1 时必填 |

各类型的填写要求：

- **服务器文件**（`file_type=1`）：`file_list` 为源主机上的绝对路径，须同时填写 `account` 与 `execute_target`
- **本地文件**（`file_type=2`）：须先调用 v3 接口 `generate_local_file_upload_url` 上传文件，再把返回的文件路径填入 `file_list`；一个元素只能带一个路径（每个本地文件占一个 `file_source_list` 条目）；文件的大小与校验值由服务端从制品库补齐，路径不存在或不属于当前资源范围时报参数错误
- **文件源文件**（`file_type=3`）：`file_list` 为文件源内的路径；`file_source_id` 可在业务的【文件源】管理页获取，它是自增主键、在不同环境中取值不同。写入只认 `file_source_id`；`get_job_template_detail` 响应中的 `file_source_code` 为只读展示字段，写入时会被忽略而不报错，便于读—改—写闭环。引用的文件源须存在、在当前业务下可用（本业务创建或已共享给本业务）且处于启用状态，否则报错


###### file_destination

| 字段             | 类型     | 必选 | 描述                              |
|----------------|--------|----|---------------------------------|
| path           | string | 是  | 目标路径                            |
| account        | object | 是  | 执行账号，见下文 `account`              |
| execute_target | object | 是  | 分发目标主机，见下文 `execute_target`     |

##### approval_info

`type` 为 `3`（人工确认）时必填。

| 字段               | 类型     | 必选 | 描述                                                 |
|------------------|--------|----|----------------------------------------------------|
| approval_type    | int    | 否  | 审批方式。可选值：`1`（任一人通过即可，默认）、`2`（需所有人通过）               |
| approval_user    | object | 是  | 审批人                                                |
| approval_message | string | 是  | 审批说明                                               |
| notify_channel   | array  | 否  | 审批通知渠道标识列表，元素为 string，如 `weixin`、`mail`。不传则不指定通知渠道 |

###### approval_user

| 字段        | 类型    | 必选 | 描述                       |
|-----------|-------|----|--------------------------|
| user_list | array | 否  | 审批人用户名列表，元素为 string      |
| role_list | array | 否  | 审批人角色标识列表，元素为 string     |

`user_list` 与 `role_list` 至少须有一项非空。


##### account

| 字段          | 类型     | 必选 | 描述                                        |
|-------------|--------|----|-------------------------------------------|
| id          | long   | 否  | 账号 ID。与 `account_var` 二选一                 |
| account_var | string | 否  | 引用的「执行账号」类型全局变量名称。与 `id` 二选一             |

`id` 与 `account_var` 至少须填写一项。同时填写时以 `id` 为准，`account_var` 会被忽略且不会保存。

`account_var` 引用的变量必须在同一请求的 `global_var_list` 中声明，且 `type` 为 `7`（执行账号），否则返回参数错误。


##### execute_target

| 字段                    | 类型     | 必选 | 描述                                                     |
|-----------------------|--------|----|--------------------------------------------------------|
| variable              | string | 否  | 引用的「执行目标列表」类型全局变量名称                                    |
| host_list             | array  | 否  | 静态主机列表                                                 |
| dynamic_group_list    | array  | 否  | 动态分组列表                                                 |
| topo_node_list        | array  | 否  | CMDB 拓扑节点列表                                            |
| container_list        | array  | 否  | 静态容器列表                                                 |
| container_filter_list | array  | 否  | 容器动态筛选条件列表，多条之间取并集                                     |

执行目标只有「引用变量」和「直接指定」两种形态，二者**互斥**：要么只填 `variable`，要么在其余五个字段中至少填一项非空；两类同时填写会返回参数错误。两类都不填也会返回参数错误。

`variable` 引用的变量必须在**同一请求**的 `global_var_list` 中声明，且 `type` 为 `3`（执行目标列表），否则返回参数错误。注意 `global_var_list` 是全量替换的：更新时把某个变量从列表里去掉就等于删除它，若仍有步骤引用它，整个请求会被拒绝——需要连同引用它的步骤一起改。

> 全局变量的默认值（`global_var_list[].execute_target`）只支持「直接指定」形态，不接受 `variable` 字段，变量的默认值不能再去引用另一个变量。

##### host_list[] 元素

| 字段          | 类型     | 必选 | 描述                                                                      |
|-------------|--------|----|-------------------------------------------------------------------------|
| bk_host_id  | long   | 否  | 主机ID。与ip+bk_cloud_id必须存在一个。当同时存在bk_host_id和ip+bk_cloud_id时，bk_host_id优先 |
| bk_cloud_id | long   | 否  | 云区域ID。与bk_host_id必须存在一个。当同时存在bk_host_id和ip+bk_cloud_id时，bk_host_id优先    |
| ip          | string | 否  | IP地址。与bk_host_id必须存在一个。当同时存在bk_host_id和ip+bk_cloud_id时，bk_host_id优先     |


##### dynamic_group_list[] 元素

| 字段 | 类型   | 必选 | 描述     |
|----|------|----|--------|
| id | string | 是  | 动态分组 ID |

##### topo_node_list[] 元素

| 字段        | 类型   | 必选 | 描述                                |
|-----------|------|----|-----------------------------------|
| id        | long   | 是  | 拓扑节点 ID                           |
| node_type | string | 是  | 拓扑节点类型。可选值：`module`（模块）、`set`（集群） |

##### container_list[] 元素

| 字段           | 类型     | 必选 | 描述                          |
|--------------|--------|----|-----------------------------|
| container_id | long   | 是  | 容器 ID（CMDB 内部 ID）           |

写入时只取 `container_id`。读接口在此基础上还会返回 `container_uid`、`name`、`pod_name`、`namespace`、`cluster_uid`、`node_ip`，它们来自保存时的快照、写入时会被忽略，因此读接口的响应可原样回传给写接口。

容器 ID 在写入时不做校验。若容器不存在或不属于当前业务，读接口仍会返回保存时的内容，但执行该作业时会报「存在无效执行对象」并列出对应的 `container_id`。

##### container_filter_list[] 元素

| 字段              | 类型     | 必选 | 描述                                |
|-----------------|--------|----|-----------------------------------|
| name            | string | 否  | 条件名称，最长 60 个字符，仅用于区分多条筛选条件        |
| kube_topo_list  | array  | 是  | 拓扑路径列表，不能为空；多条路径取并集，且共用同一组 `prop_conditions` |
| prop_conditions | array  | 否  | 容器字段级筛选条件，条件之间取交集                 |

> 本字段的拓扑用 CMDB 内部数字 ID 表达，与「执行作业」等接口的 `kube_container_filters`（用集群 UID、namespace 名称）形态不同，两者不能互换。

###### kube_topo_list[] 元素

| 字段            | 类型     | 必选 | 描述                                  |
|---------------|--------|----|-------------------------------------|
| cluster       | object | 是  | 集群拓扑对象，形如 `{"id": 105}`             |
| namespace     | object | 否  | namespace 拓扑对象，形如 `{"id": 207}`；不填表示不限定 namespace |
| workload_list | array  | 否  | workload 拓扑对象列表，元素形如 `{"kind": "deployment", "id": 309}`；不填表示不限定 workload |

集群、namespace、workload 的 ID 均为 CMDB 内部 ID，可在页面上配置一次后通过读接口获取。这些 ID 在写入时不做校验，若不存在或不属于当前业务，执行时该步骤会因筛选不到容器而报「目标执行对象为空」。

###### prop_conditions[] 元素

| 字段    | 类型     | 必选 | 描述          |
|-------|--------|----|-------------|
| field | string | 是  | 筛选字段，取值见下表  |
| value | string | 是  | 取值，匹配方式见下表  |

**本接口不接收运算符**。每个字段的匹配方式是固定的，由服务端按字段补齐，与页面上配置动态条件时的行为完全一致，因此接口创建的条件在页面上能正常回显：

| field                     | 匹配方式  | 说明                          |
|---------------------------|-------|-----------------------------|
| `container_name`          | 包含    | 容器名称包含 `value`              |
| `container_container_uid` | 等值    | 容器运行时 UID 等于 `value`        |
| `pod_name`                | 包含    | Pod 名称包含 `value`            |
| `pod_labels`              | 标签选择器 | Pod 标签匹配 `value` 表达式        |

前三个字段的 `value` 支持英文逗号分隔的多值，多值之间取并集，例如 `nginx,redis` 表示名称包含 `nginx` 或包含 `redis`。

**`pod_labels` 的 value 是原生 K8s 标签选择器表达式**，不是普通字符串，写法见 [Kubernetes 官方文档：Label selectors](https://kubernetes.io/docs/concepts/overview/working-with-objects/labels/#label-selectors)。等值型与集合型都支持，其中的英文逗号是表达式自身的 AND 分隔符，**不会**被当作多值拆分。表达式在接口侧会被解析校验，不合法直接返回 400。例如：

- `app=nginx` —— 标签 `app` 等于 `nginx`
- `app=nginx,tier!=frontend` —— 同时满足两个条件
- `env in (prod, pre)` —— `env` 属于给定集合
- `!canary` —— 不存在 `canary` 标签


### 请求参数示例

下例把模板原有的步骤 `101`、`102`、`103` 更新为「保留 101、删除 102、保留 103 并在其后新增一个人工确认步骤」。

- POST

```json
{
    "bk_scope_type": "biz",
    "bk_scope_id": "2",
    "id": 1000,
    "name": "my-api-template",
    "description": "updated by openapi",
    "global_var_list": [
        {
            "name": "TARGET_DIR",
            "type": 1,
            "required": 1,
            "value": "/data/release-v2"
        }
    ],
    "step_list": [
        {
            "id": 101,
            "name": "run-script",
            "type": 1,
            "script_info": {
                "script_type": 2,
                "script_id": "3a1b0e8f9c2d4e5f",
                "script_version_id": 5001,
                "script_timeout": 1000,
                "account": {
                    "id": 1
                },
                "execute_target": {
                    "host_list": [
                        {
                            "bk_host_id": 10001
                        }
                    ]
                }
            }
        },
        {
            "id": 103,
            "name": "transfer-file",
            "type": 2,
            "file_info": {
                "file_source_list": [
                    {
                        "file_type": 3,
                        "file_list": [
                            "/bucket/config.yaml"
                        ],
                        "file_source_id": 7
                    }
                ],
                "file_destination": {
                    "path": "/data/release",
                    "account": {
                        "id": 1
                    },
                    "execute_target": {
                        "host_list": [
                            {
                                "bk_host_id": 10001
                            }
                        ]
                    }
                }
            }
        },
        {
            "name": "confirm",
            "type": 3,
            "approval_info": {
                "approval_user": {
                    "user_list": [
                        "admin"
                    ]
                },
                "approval_message": "确认发布"
            }
        }
    ]
}
```

### 返回结果示例

#### 失败示例

```json
# http status: 400
{
    "error": {
        "code": "INVALID_ARGUMENT",
        "message": "请求参数[bk_scope_type]不合法"
    }
}
```

```json
# http status: 403
{
    "error": {
        "code": "NO_PERMISSION",
        "message": "用户(张三)权限不足，请前往权限中心确认并申请补充后重试",
        "data": {
            "system_id": "bk_job",
            "system_name": "作业平台",
            "actions": [
                {
                    "id": "access_business",
                    "name": "业务访问",
                    "related_resource_types": [
                        {
                            "type": "biz",
                            "system_id": "bk_cmdb",
                            "system_name": "配置平台",
                            "type_name": "业务",
                            "instances": [
                                [
                                    {
                                        "id": "1",
                                        "type": "business",
                                        "name": "blueking",
                                        "type_name": "业务"
                                    }
                                ]
                            ]
                        }
                    ]
                }
            ]
        }
    }
}
```


#### 成功示例

```json
# http status=200
{
    "data": {
        "bk_scope_type": "biz",
        "bk_scope_id": "2",
        "job_template_id": 1000,
        "name": "my-api-template",
        "creator": "admin",
        "create_time": 1738220000000,
        "last_modify_time": 1738221000000
    }
}
```

### 返回结果参数说明

##### 正常响应体

| 字段   | 类型     | 是否一定存在 | 描述                           |
|------|--------|--------|------------------------------|
| data | object | 是      | 响应数据，只有在正常响应时才存在该字段，异常响应时不存在 |

##### 异常响应体

| 字段    | 类型     | 是否一定存在 | 描述                                                     |
|-------|--------|--------|--------------------------------------------------------|
| error | object | 是      | 错误信息，只有在异常响应时（HTTP状态码!=2xx）才存在该字段（权限不足、参数错误等），正常响应时不存在 |

#### error

| 字段      | 类型     | 是否一定存在 | 描述           |
|---------|--------|--------|--------------|
| code    | string | 是      | 错误码          |
| message | string | 是      | 错误信息         |
| data    | object | 否      | 错误具体内容，权限信息等 |


#### data

| 字段               | 类型     | 描述                                  |
|------------------|--------|-------------------------------------|
| bk_scope_type    | string | 资源范围类型。可选值：`biz`（业务）、`biz_set`（业务集） |
| bk_scope_id      | string | 资源范围 ID                             |
| job_template_id  | long   | 作业模板 ID                             |
| name             | string | 作业模板名称                              |
| creator          | string | 创建人                                 |
| create_time      | long   | 创建时间，Unix 时间戳，单位毫秒                  |
| last_modify_time | long   | 最近一次修改时间，Unix 时间戳，单位毫秒              |
