# search_spans 查询示例与输出解析

## 示例 1：查询指定 trace_id 的所有 Span

### 请求

```json
{
  "body_param": {
    "app_name": "bk_job_cloud",
    "bk_biz_id": "7",
    "start_time": "1770566400",
    "end_time": "1770652800",
    "filters": [
      {
        "key": "trace_id",
        "operator": "equal",
        "value": ["506621e2295f33c8d5e77da9a61fde69"]
      }
    ],
    "limit": "10",
    "offset": "0"
  }
}
```

### 返回数据（节选）

> ⚠️ **注意**：返回 JSON 中的 `response_body.data.total` 字段**不准确**（可能为 0 即使有数据），始终以 `response_body.data.data` 数组的实际内容为准来判断查询结果。

```json
{
  "code": 200,
  "data": {
    "data": [
      {
        "resource.service.name": "job-logsvr",
        "span_name": "POST",
        "span_id": "8d6733ab8f22ef49",
        "parent_span_id": "c4df181a145794de",
        "status.code": 0,
        "elapsed_time": 692498971,
        "attributes.http.method": "POST",
        "attributes.http.path": "/service/log/batch",
        "attributes.http.status_code": 200,
        "resource.k8s.pod.name": "bk-job-logsvr-7d4b4d856d-drwbv",
        "trace_id": "506621e2295f33c8d5e77da9a61fde69",
        "kind": 2
      },
      {
        "resource.service.name": "job-logsvr",
        "span_name": "update job_log_file_2026_02_09",
        "span_id": "f096cb13ac12643e",
        "parent_span_id": "8d6733ab8f22ef49",
        "status.code": 0,
        "elapsed_time": 991691,
        "attributes.mongodb.collection": "job_log_file_2026_02_09",
        "attributes.mongodb.command": "update",
        "attributes.peer.service": "mongodb-joblog",
        "kind": 3
      }
    ]
  }
}
```

### 分析要点

1. **Span `8d6733ab8f22ef49`**（job-logsvr POST /service/log/batch）：
   - `elapsed_time` = 692498971 μs ≈ **692 秒 ≈ 11.5 分钟**，**严重耗时异常**
   - `parent_span_id` = `c4df181a145794de`（来自 job-execute 的调用方 Span）
   - `status.code` = 0（正常返回），说明虽然最终完成了，但耗时过长

2. **Span `f096cb13ac12643e`**（MongoDB update）：
   - `elapsed_time` = 991691 μs ≈ 0.99 秒，在合理范围内
   - 是 `8d6733ab8f22ef49` 的子 Span

3. **结论**：job-logsvr 处理批量日志写入请求耗时 11.5 分钟，但其子 Span（MongoDB 操作）每次只需 ~1 秒。需要查看是否有大量串行 MongoDB 操作导致总耗时过长。

---

## 示例 2：查询指定 trace_id 中的 Error Span

### 请求

```json
{
  "body_param": {
    "app_name": "bk_job_cloud",
    "bk_biz_id": "7",
    "start_time": "1770566400",
    "end_time": "1770652800",
    "filters": [
      {
        "key": "status.code",
        "operator": "equal",
        "value": [2],
        "options": { "group_relation": "AND" }
      },
      {
        "key": "trace_id",
        "operator": "equal",
        "value": ["506621e2295f33c8d5e77da9a61fde69"],
        "options": { "group_relation": "AND" }
      }
    ],
    "limit": "10"
  }
}
```

### 返回数据（节选）

```json
{
  "code": 200,
  "data": {
    "data": [
      {
        "resource.service.name": "job-execute",
        "span_name": "POST",
        "span_id": "c4df181a145794de",
        "parent_span_id": "8090b0fbf81902ab",
        "status.code": 2,
        "elapsed_time": 40126709,
        "attributes.http.method": "POST",
        "attributes.http.path": "/service/log/batch",
        "events.attributes.exception.type": "java.net.SocketTimeoutException",
        "events.attributes.exception.message": "Read timed out",
        "events.attributes.exception.stacktrace": "java.net.SocketTimeoutException: Read timed out\n\tat java.base/sun.nio.ch.NioSocketImpl.timedRead...",
        "events.name": "exception",
        "kind": 3,
        "trace_id": "506621e2295f33c8d5e77da9a61fde69"
      }
    ]
  }
}
```

### 分析要点

1. **Error Span `c4df181a145794de`**（job-execute → POST /service/log/batch）：
   - `status.code` = 2（错误）
   - 异常类型：`java.net.SocketTimeoutException`
   - 异常消息：`Read timed out`
   - 这是 job-execute 调用 job-logsvr 的 CLIENT 端 Span（kind=3）

2. **关联分析**：结合示例 1 的数据
   - Error Span `c4df181a145794de`（job-execute 侧，CLIENT）是 Span `8d6733ab8f22ef49`（job-logsvr 侧，SERVER）的调用方
   - job-execute 的读超时时间是 40 秒（elapsed_time=40126709 μs），而 job-logsvr 实际处理耗时 692 秒
   - **根因**：job-logsvr 处理批量日志请求耗时 692 秒，远超 job-execute 的 40 秒读超时配置，导致 job-execute 抛出 `SocketTimeoutException`

3. **结论**：
   - **表象**：job-execute 报 `Read timed out`（Error Span）
   - **根因**：job-logsvr 处理请求耗时异常（692 秒），导致调用方超时
   - **建议**：排查 job-logsvr 批量日志写入的性能问题（可能是大批量数据导致的 MongoDB 写入慢）

---

## 示例 3：分页查询

当调用链 Span 数量较多时，需要分页：

### 第一页

```json
{
  "body_param": {
    "app_name": "bk_job_cloud",
    "bk_biz_id": "7",
    "start_time": "1770566400",
    "end_time": "1770652800",
    "filters": [
      {
        "key": "trace_id",
        "operator": "equal",
        "value": ["506621e2295f33c8d5e77da9a61fde69"],
        "options": { "group_relation": "AND" }
      }
    ],
    "offset": "0",
    "limit": "10"
  }
}
```

### 第二页

```json
{
  "body_param": {
    "app_name": "bk_job_cloud",
    "bk_biz_id": "7",
    "start_time": "1770566400",
    "end_time": "1770652800",
    "filters": [
      {
        "key": "trace_id",
        "operator": "equal",
        "value": ["506621e2295f33c8d5e77da9a61fde69"],
        "options": { "group_relation": "AND" }
      }
    ],
    "offset": "10",
    "limit": "10"
  }
}
```

---

## 示例 4：查询指定 Span 的子 Span（逐层下钻定位根因）

当通过示例 2 找到 Error Span（`span_id=c4df181a145794de`）后，需要进一步查看它的子 Span，判断是什么原因导致了该 Error。

### 请求：查询 Error Span 的子 Span

通过 `parent_span_id` 过滤，查询 `span_id=c4df181a145794de` 的所有子 Span：

```json
{
  "body_param": {
    "app_name": "bk_job_cloud",
    "bk_biz_id": "7",
    "start_time": "1770566400",
    "end_time": "1770652800",
    "filters": [
      {
        "key": "parent_span_id",
        "operator": "equal",
        "value": ["c4df181a145794de"],
        "options": { "group_relation": "AND" }
      },
      {
        "key": "trace_id",
        "operator": "equal",
        "value": ["506621e2295f33c8d5e77da9a61fde69"],
        "options": { "group_relation": "AND" }
      }
    ],
    "limit": "10"
  }
}
```

### 返回数据（节选）

```json
{
  "code": 200,
  "data": {
    "data": [
      {
        "resource.service.name": "job-logsvr",
        "span_name": "POST",
        "span_id": "8d6733ab8f22ef49",
        "parent_span_id": "c4df181a145794de",
        "status.code": 0,
        "elapsed_time": 692498971,
        "attributes.http.method": "POST",
        "attributes.http.path": "/service/log/batch",
        "attributes.http.status_code": 200,
        "resource.k8s.pod.name": "bk-job-logsvr-7d4b4d856d-drwbv",
        "kind": 2
      }
    ]
  }
}
```

### 分析要点

1. Error Span `c4df181a145794de`（job-execute，status.code=2，Read timed out）的子 Span 是 `8d6733ab8f22ef49`（job-logsvr，status.code=0，elapsed_time=692 秒）
2. 子 Span 虽然 status.code=0（最终正常完成），但耗时 692 秒，远超父 Span 40 秒的读超时限制
3. **结论**：Error Span 报错（Read timed out）的根因是其子 Span 处理耗时过长

### 继续下钻：查询子 Span 的子 Span（孙子 Span）

既然发现 `8d6733ab8f22ef49` 耗时 692 秒，需要进一步查看它的子 Span，了解这 692 秒内具体发生了什么：

```json
{
  "body_param": {
    "app_name": "bk_job_cloud",
    "bk_biz_id": "7",
    "start_time": "1770566400",
    "end_time": "1770652800",
    "filters": [
      {
        "key": "parent_span_id",
        "operator": "equal",
        "value": ["8d6733ab8f22ef49"],
        "options": { "group_relation": "AND" }
      },
      {
        "key": "trace_id",
        "operator": "equal",
        "value": ["506621e2295f33c8d5e77da9a61fde69"],
        "options": { "group_relation": "AND" }
      }
    ],
    "limit": "10"
  }
}
```

返回大量 MongoDB update 操作的子 Span，每个耗时 ~1 秒，说明 job-logsvr 对大批量日志执行了大量串行 MongoDB 写入操作，累积导致总耗时 692 秒。

### 逐层下钻总结

```
逐层下钻路径：

① Error Span (c4df...) → status.code=2, Read timed out
   ↓ 查询 parent_span_id=c4df... 的子 Span
② 子 Span (8d67...) → status.code=0, elapsed_time=692s ⚠️
   ↓ 查询 parent_span_id=8d67... 的子 Span
③ 孙子 Span (f096...) → MongoDB update, elapsed_time=0.99s ✅
   孙子 Span (0bab...) → MongoDB update, elapsed_time=1.01s ✅
   ... (大量类似 Span)

根因：job-logsvr 批量串行执行 MongoDB update 操作，累积耗时过长
```

> 💡 **核心方法**：通过不断替换 `parent_span_id` 为下一层 Span 的 `span_id`，沿着调用树逐层深入，直到找到最底层的根因。这种「逐层下钻」是 APM 分析的核心技巧。

---

## 综合分析示例：从 APM 到根因定位

### 场景：作业任务执行超时

**输入**：request_id = `506621e2295f33c8d5e77da9a61fde69`

### 步骤 1：查所有 Span（按耗时降序）

查询返回 Span 列表，按 `elapsed_time` 降序排列，快速定位耗时最长的 Span。

### 步骤 2：查 Error Span

过滤 `status.code = 2`，找到报错的 Span。

### 步骤 3：还原调用树

```
[Root] job-execute: executeTask (parent_span_id="")
  ├── [Span A] job-execute → job-logsvr: POST /service/log/batch (span_id=c4df181a145794de)
  │     status.code=2, exception=SocketTimeoutException: Read timed out
  │     elapsed_time=40s (读超时)
  │     └── [Span B] job-logsvr: POST /service/log/batch (span_id=8d6733ab8f22ef49)
  │           status.code=0, elapsed_time=692s ⚠️ 耗时异常
  │           ├── [Span C] MongoDB: update job_log_file (elapsed_time=0.99s) ✅
  │           ├── [Span D] MongoDB: update job_log_file (elapsed_time=1.01s) ✅
  │           └── ... (大量 MongoDB 操作)
  └── [Span E] job-execute: other operations ✅
```

### 步骤 4：根因判定

| 分析维度 | Error Span (c4df...)    | 耗时异常 Span (8d67...)  |
|------|-------------------------|----------------------|
| 服务   | job-execute (CLIENT)    | job-logsvr (SERVER)  |
| 状态   | ❌ error (status.code=2) | ✅ 正常 (status.code=0) |
| 耗时   | 40s (触发读超时)             | **692s (实际处理时间)**    |
| 角色   | 表象（超时报错）                | **根因（处理过慢）**         |

**结论**：
- **根因**：job-logsvr 处理批量日志写入请求耗时 692 秒（约 11.5 分钟），每次 MongoDB update 操作耗时正常（~1s），但大量串行操作累积导致总耗时过长
- **表象**：job-execute 端在等待 40 秒后触发读超时，抛出 `SocketTimeoutException`
- **建议**：
  1. 排查本次请求的日志数据量是否异常（`attributes.http.request_content_length = 13565449` ≈ 13MB，确实较大）
  2. 优化 job-logsvr 的批量写入逻辑（如并发写入 MongoDB、减少单批次数据量）
