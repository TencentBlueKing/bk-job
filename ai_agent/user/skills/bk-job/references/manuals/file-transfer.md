# 快速分发文件

将文件分发到目标机器的指定目录，源文件支持两种类型：**服务器文件**（业务内某台机器上的文件）、**本地文件**（先上传到作业平台再分发）。字段级说明见 [`../apidocs/fast_transfer_file.md`](../apidocs/fast_transfer_file.md) 与 [`../apidocs/generate_local_file_upload_url.md`](../apidocs/generate_local_file_upload_url.md)。

> **ℹ️ 引导用户选择分发类型时，只提供「服务器文件」与「本地文件」两种。** 第三方文件源（如 COS，`file_type=3`）相关接口目前尚未提供，技能**暂不支持**，请勿向用户提供或组装该类型；脚本对第三方文件源会直接报错拒绝。

> **⚠️ 高风险写操作**：`fast-transfer-file` 会向目标机器**真实分发文件**，属生产变更类操作。真实执行前**必须**遵守 [confirmation-and-output-protocol.md](confirmation-and-output-protocol.md) 第 1 节的确认门禁（先展示确认摘要、等用户下一条独立回复后再执行）。`--dry-run` 仅打印请求体，不视为已确认执行。
>
> `gen-local-upload-url`（生成上传地址）与 `upload-local-file`（上传文件字节）是本地文件分发的**准备步骤**，本身不向目标机器分发文件，可先执行；真正的分发（`fast-transfer-file` 非 `--dry-run`）才须过确认门禁。

## 1. 子命令

| 子命令 | 接口 | 说明 |
|--------|------|------|
| `gen-local-upload-url` | `POST /api/v3/generate_local_file_upload_url` | 本地文件分发第 1 步：按文件名生成上传地址 `upload_url` 与分发路径 `path` |
| `upload-local-file` | HTTP PUT（制品库临时地址，非 APIGW） | 本地文件分发第 2 步：以 PUT + `--data-binary` 将本地文件字节上传到 `upload_url` |
| `fast-transfer-file` | `POST /api/v3/fast_transfer_file` | 分发文件到目标机器，写操作，须过确认门禁 |

## 2. 支持的源文件类型（仅两种）

| 类型 | file_type | 说明 | 便捷参数 |
|------|-----------|------|----------|
| 服务器文件 | 1 | 分发业务内某台机器上已有的文件 | `--server-file-list` + `--source-account-*` + `--source-host-id-list`/`--source-ip-list` |
| 本地文件 | 2 | 分发运行环境本地的文件（先上传再分发） | `--local-file-list`（路径取自 `gen-local-upload-url` 的 `path`） |

> **第三方文件源（file_type=3）暂不支持**：相关接口目前未提供，脚本对含 `file_type=3` 或 `file_source_id`/`file_source_code` 的源文件会报错退出。

服务器/本地文件的滚动等复杂结构可用 `--file-source-file` 传完整 `file_source_list` JSON 数组（同样仅允许 file_type=1/2）。

## 3. 目标与账号

| 参数 | 说明 |
|------|------|
| `--file-target-path` | **必填**，目标目录，如 `/tmp/` |
| `--file-target-name` | 可选，目标文件名；不传保持源文件名 |
| `--account-alias` / `--account-id` | **目标机器**执行账号，至少一个；同时提供以 `account_id` 为准。用户未指定时用 `account-list` 引导选择，见 [account-query-and-selection.md](account-query-and-selection.md) |
| `--target-host-id-list` / `--target-ip-list` / `--target-server-file` | 目标服务器，三选一（至少一种）。用户未给主机时用 `host-search`/`host-topo-tree` 定位，见 [host-query-and-selection.md](host-query-and-selection.md) |

其它：`--transfer-mode`（1 严谨 / 2 强制，默认强制）、`--timeout`（秒，1~259200，默认 7200）、`--download-speed-limit` / `--upload-speed-limit`（MB）、`--callback-url`、`--no-start-task`。

> **⚠️ 用 IP 指定目标/源机器时，必须同时给 `bk_cloud_id` 与 `ip`**（`bk_cloud_id:ip`），只给 IP 会报「目标执行对象为空」。**优先用 `bk_host_id`**（`--target-host-id-list` / `--source-host-id-list`）避免云区域歧义。

## 4. 推荐流程

### 4.1 分发服务器文件（一步）

```
[A] 确认源机器与源账号（host-search / account-list）
        └─ --source-host-id-list（或 --source-ip-list）+ --source-account-alias/id
             ↓
[B] 确认目标机器、目标账号与目标目录
        └─ --target-host-id-list（或 --target-ip-list）+ --account-alias/id + --file-target-path
             ↓
[C] fast-transfer-file --server-file-list <源文件绝对路径,...> --dry-run 预览请求体
             ↓
[D] 展示确认摘要 → 用户下一条独立确认（G2）→ 真实分发
```

- 与本地文件的关键差异：服务器文件源（file_type=1）**必须**给源机器与源账号，`--server-file-list` 填**源机器上的绝对路径**（不是上传返回的 path）。
- 源机器与目标机器的账号是两套参数：源用 `--source-account-alias`/`--source-account-id`，目标用 `--account-alias`/`--account-id`，别混用。
- 源文件路径不存在或源账号无读取权限时，分发会在执行阶段失败而非提交阶段报错，必要时先确认路径。
- 多个源机器上分发同名文件时，目标机会按来源分目录存放，避免覆盖。

### 4.2 分发本地文件（三步）

```
[A] gen-local-upload-url --file-names a.sh,b.tar.gz
        └─ 返回 url_map：每个文件的 upload_url 与 path
             ↓
[B] 对每个文件：upload-local-file --upload-url <upload_url> --file-path <本地文件>
             ↓
[C] fast-transfer-file --local-file-list <path1>,<path2> ...
        └─ --dry-run 预览 → 确认摘要 → 用户独立确认（G2）→ 真实分发
```

- 本地文件源（file_type=2）**不需要**源账号与源服务器，只需 `path`。
- `path` 必须是 `gen-local-upload-url` 返回的原样路径；须先 `upload-local-file` 成功上传，否则分发时找不到文件。
- 两步的 HTTP 方法不同，勿混淆：**[A] 生成上传地址走 APIGW 的 POST**，**[B] 上传文件字节走制品库地址的 PUT**（`Content-Type: application/octet-stream`，等价于 `curl -X PUT --data-binary @<file> "<upload_url>"`）；`upload_url` 自带 token，不附加 APIGW 鉴权头。

## 5. 确认摘要格式（真实分发前必须展示）

```
即将执行的操作：
- 操作类型：快速分发文件
- 资源范围：{bk_scope_type} / {bk_scope_id}
- 目标机器：{共 N 台 / host_id 或 IP 片段 / 动态分组 / 拓扑节点}
- 目标路径：{file_target_path}{file_target_name 若有}
- 执行账号：{account_alias 或 account_id}
- 源文件：{服务器文件 路径 + 源机器 / 本地文件 N 个}
- 传输模式：{严谨 / 强制}
- 超时时间：{timeout 秒，默认 7200}

请确认是否立即分发。
```

## 6. 命令示例

```bash
# A. 服务器文件分发（dry-run 预览）
python scripts/job_apigw_client.py fast-transfer-file \
  --bk-scope-id <业务ID> \
  --file-target-path /tmp/ \
  --account-alias root \
  --target-host-id-list 103,104 \
  --server-file-list /data/release/app.tar.gz \
  --source-account-alias root --source-host-id-list 101 \
  --transfer-mode 1 --dry-run

# B. 本地文件分发：第 1 步生成上传地址
python scripts/job_apigw_client.py gen-local-upload-url \
  --bk-scope-id <业务ID> --file-names app.sh
# 返回 url_map["app.sh"] = {upload_url, path}

# B2. 第 2 步上传本地文件字节到 upload_url
python scripts/job_apigw_client.py upload-local-file \
  --upload-url "<url_map 中的 upload_url>" \
  --file-path /home/me/app.sh

# B3. 第 3 步用返回的 path 分发（dry-run → 确认 → 真实分发）
python scripts/job_apigw_client.py fast-transfer-file \
  --bk-scope-id <业务ID> \
  --file-target-path /tmp/ \
  --account-alias root \
  --target-host-id-list 103,104 \
  --local-file-list "<url_map 中的 path>" \
  --dry-run

# C. 服务器文件的复杂结构（如按源文件滚动）用 --file-source-file（仅 file_type=1/2）
cat > /tmp/file_sources.json << 'EOF'
[
  {"file_type": 1, "file_list": ["/data/1.txt", "/data/2.txt"], "account": {"alias": "root"}, "server": {"host_id_list": [101]}}
]
EOF
python scripts/job_apigw_client.py fast-transfer-file \
  --bk-scope-id <业务ID> \
  --file-target-path /tmp/ --account-alias root \
  --target-host-id-list 103,104 \
  --file-source-file /tmp/file_sources.json --dry-run
```

分发成功后脚本 JSON 会补充 **`job_instance_url`**（来自 `config.yaml` 的 `job_base_url`），格式为 `{job_base_url}/api_execute/{job_instance_id}`；须以可点击链接交付用户，并可用 `instance-status` / `get-instance-log` 跟进结果。

## 7. 相关手册与接口文档

- [confirmation-and-output-protocol.md](confirmation-and-output-protocol.md) — 写操作确认门禁与输出规范
- [host-query-and-selection.md](host-query-and-selection.md) — 查/搜目标机器与源机器
- [account-query-and-selection.md](account-query-and-selection.md) — 查询可用执行账号
- [scope-selection-and-onboarding.md](scope-selection-and-onboarding.md) — 首次引导选择业务/业务集
- [`../apidocs/fast_transfer_file.md`](../apidocs/fast_transfer_file.md) — 分发接口字段级文档
- [`../apidocs/generate_local_file_upload_url.md`](../apidocs/generate_local_file_upload_url.md) — 本地文件上传地址生成字段级文档
