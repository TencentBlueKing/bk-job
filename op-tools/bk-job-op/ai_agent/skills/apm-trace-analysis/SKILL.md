---
name: apm-trace-analysis
description: 蓝鲸监控 APM 调用链分析专家。通过蓝鲸监控平台的 bkmonitor-tracing MCP 服务中的 search_spans 工具，查询和分析 APM Span 数据。当需要：(1) 通过 trace_id/request_id 查询完整调用链，(2) 定位调用链中的 Error Span 和耗时异常 Span，(3) 分析微服务间的调用关系和错误传播路径，(4) 结合 APM 数据辅助排查作业平台任务执行异常时使用。
---

# APM Trace 分析

使用蓝鲸监控平台 `bkmonitor-tracing` MCP 服务的 `search_spans` 工具，查询和分析 APM 调用链数据。

## 核心工具：search_spans

通过 `use_mcp_tool` 调用，serverName 为 `bkmonitor-tracing`，toolName 为 `search_spans`。

### 参数结构

所有参数封装在 `body_param` 对象中：

| 参数 | 必填 | 类型 | 说明 |
|------|------|------|------|
| `app_name` | ✅ | string | APM 应用名称（从上下文获取，如 `bk_job_cloud`） |
| `bk_biz_id` | ✅ | string | 蓝鲸业务 ID（从上下文获取，如 `"7"`） |
| `start_time` | ✅ | string | 开始时间戳（秒），根据上下文自行确定合适的时间。与 `end_time` 的差值 ≤ 86400 |
| `end_time` | ✅ | string | 结束时间戳（秒），根据上下文自行确定合适的时间。与 `start_time` 的差值 ≤ 86400 |
| `filters` | 否 | array | 过滤条件数组，详见下方 |
| `limit` | 否 | string | 返回最大 Span 数，**固定传 `"10"`** |
| `offset` | 否 | string | 分页偏移量，默认 `"0"` |
| `sort` | 否 | array | 排序条件，默认 `["-elapsed_time"]`（耗时降序） |

### ⚠️ 关键约束

1. **时间戳根据上下文确定**：根据上下文中的告警时间、任务执行时间等信息，自行确定合适的 `start_time` 和 `end_time`。不要固定从当前时间往前算，而应根据实际场景选择最合理的时间窗口
2. **查询时间跨度不得超过 1 天**：`end_time - start_time` **必须 ≤ 86400**（即 60×60×24 秒）。如果超过此限制，API 会报错。如果需要查询的 trace 发生在较早的时间，应将 `start_time` 和 `end_time` 设为包含该时间点的 1 天窗口内，而非从当前时间往前算
3. **limit 固定传 `"10"`**：避免返回内容过大导致 token 超限
4. **所有参数值为 string 类型**（包括 limit、offset、start_time、end_time）

### filters 语法

每个 filter 对象结构：

```json
{
  "key": "字段名",
  "operator": "操作符",
  "value": [值数组],
  "options": { "group_relation": "AND" }  // 可选，多 filter 时建议加
}
```

**常用操作符**：`equal`、`not_equal`、`like`

**常用 filter key**：

| key | 说明 | 示例 value |
|-----|------|-----------|
| `trace_id` | 链路追踪 ID | `["b0a9Ce0e-xxxx"]` |
| `status.code` | Span 状态码（0=正常, 2=错误） | `[2]` |
| `parent_span_id` | 父 Span ID，用于查询某个 Span 的子 Span | `["c4df181a145794de"]` |
| `resource.service.name` | 服务名 | `["job-execute"]` |
| `span_name` | Span 操作名 | `["POST"]` |
| `attributes.http.status_code` | HTTP 状态码 | `[500]` |
| `attributes.http.method` | HTTP 方法 | `["POST"]` |

---

## 标准查询场景

### 场景 1：查询指定 trace_id 的所有 Span

这是最核心的查询——通过 `request_id`（即 `trace_id`）获取完整调用链。

```json
{
  "serverName": "bkmonitor-tracing",
  "toolName": "search_spans",
  "arguments": "{\"body_param\":{\"app_name\":\"bk_job_cloud\",\"bk_biz_id\":\"7\",\"start_time\":\"{根据上下文确定}\",\"end_time\":\"{根据上下文确定}\",\"filters\":[{\"key\":\"trace_id\",\"operator\":\"equal\",\"value\":[\"{trace_id}\"]}],\"limit\":\"10\",\"offset\":\"0\"}}"
}
```

### 场景 2：查询指定 trace_id 中的 Error Span

当只关心出错的 Span 时，增加 `status.code = 2` 的过滤：

```json
{
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
      "value": ["{trace_id}"],
      "options": { "group_relation": "AND" }
    }
  ],
  "limit": "10"
}
```

### 场景 3：分页获取更多 Span

如果调用链较长（Span 数 > 10），通过 offset 分页：

- 第一页：`"offset": "0", "limit": "10"`
- 第二页：`"offset": "10", "limit": "10"`

### 场景 4：查询指定 Span 的子 Span（逐层下钻定位根因）

当发现某个 Error Span 或耗时异常 Span 后，可以通过 `parent_span_id` 过滤来查询它的**子 Span**，观测子 Span 的指标和行为，判断是否是导致父 Span 异常的根因。

```json
{
  "filters": [
    {
      "key": "parent_span_id",
      "operator": "equal",
      "value": ["{目标Span的span_id}"],
      "options": { "group_relation": "AND" }
    },
    {
      "key": "trace_id",
      "operator": "equal",
      "value": ["{trace_id}"],
      "options": { "group_relation": "AND" }
    }
  ],
  "limit": "10"
}
```

**逐层下钻方法**：

1. **第一层**：找到 Error Span（如 `span_id=A`，`status.code=2`），用 `parent_span_id=A` 查询其子 Span
2. **第二层**：在子 Span 中发现耗时异常的 Span（如 `span_id=B`），用 `parent_span_id=B` 继续查询其子 Span（即 Error Span 的孙子 Span）
3. **第 N 层**：以此类推，逐层深入，直到找到最底层的根因 Span

```
逐层下钻示意图：

[Error Span A] status.code=2, exception=Read timed out
  ↓ 查询 parent_span_id=A 的子 Span
  └── [Span B] status.code=0, elapsed_time=692s ⚠️ 耗时异常
        ↓ 查询 parent_span_id=B 的子 Span（孙子 Span）
        ├── [Span C] MongoDB update (elapsed_time=0.99s) ✅
        ├── [Span D] MongoDB update (elapsed_time=1.01s) ✅
        └── ... 发现大量串行 MongoDB 操作 → 根因：批量串行写入
```

> 💡 **关键技巧**：通过不断替换 `parent_span_id` 的值为下一层 Span 的 `span_id`，可以沿着调用树逐层下钻，精准定位到导致上层 Error 或耗时异常的最底层根因。

---

## 返回数据解析

> ⚠️ **重要提示：以 `response_body.data.data` 数组为准，忽略 `response_body.data.total`**
> 
> `search_spans` 返回的 JSON 结构中，`response_body.data.total` 字段**不准确**，不能反映真实查到的 Span 数量。例如实际查到 2 条 Span 数据，但 `total` 可能返回 0。
> 
> **正确做法**：始终以 `response_body.data.data` 数组中的实际元素为准来判断查询结果，不要依赖 `total` 字段做任何判断（如是否有更多数据需要分页、是否查询为空等）。

### Span 关键字段

| 字段 | 含义 | 分析要点 |
|------|------|----------|
| `trace_id` | 链路 ID | 同一请求的所有 Span 共享此 ID |
| `span_id` | 当前 Span 的唯一 ID | 用于构建调用拓扑 |
| `parent_span_id` | 父 Span ID | **核心**：通过 parent-child 关系还原调用树 |
| `span_name` | 操作名称 | 如 `POST`、`SELECT`、`update xxx` |
| `resource.service.name` | 所属服务名 | 如 `job-execute`、`job-logsvr` |
| `status.code` | 状态码 | **0=正常, 2=错误** |
| `status.message` | 状态消息 | 错误时包含简要说明 |
| `elapsed_time` | 耗时（**微秒**） | 692498971 微秒 ≈ 692 秒 ≈ 11.5 分钟 |
| `start_time` / `end_time` | 起止时间（微秒时间戳） | 用于排序和时间线分析 |
| `kind` | Span 类型 | 2=SERVER, 3=CLIENT |
| `events.attributes.exception.type` | 异常类型 | 如 `java.net.SocketTimeoutException` |
| `events.attributes.exception.message` | 异常消息 | 如 `Read timed out` |
| `events.attributes.exception.stacktrace` | 异常堆栈 | 完整的 Java Stack Trace |
| `attributes.http.method` | HTTP 方法 | GET/POST 等 |
| `attributes.http.path` | 请求路径 | 如 `/service/log/batch` |
| `attributes.http.status_code` | HTTP 状态码 | 200/500 等 |
| `resource.k8s.pod.name` | K8S Pod 名 | 定位具体实例 |

### elapsed_time 单位换算

`elapsed_time` 的单位是**微秒（μs）**：
- 1,000 μs = 1 ms（毫秒）
- 1,000,000 μs = 1 s（秒）
- 示例：`692498971` μs = **692.5 秒 ≈ 11.5 分钟**

---

## 分析工作流

拿到 Span 数据后，按以下步骤分析：

### Step 1：还原调用树

通过 `span_id` 和 `parent_span_id` 的对应关系，构建调用拓扑：

```
[Root Span] span_id=A, parent_span_id=""
  ├── [Child Span] span_id=B, parent_span_id=A
  │     ├── [Grandchild Span] span_id=C, parent_span_id=B
  │     └── [Grandchild Span] span_id=D, parent_span_id=B
  └── [Child Span] span_id=E, parent_span_id=A
```

### Step 2：识别 Error Span

筛选 `status.code = 2` 的 Span，这些是出错的环节。重点关注：
- `events.attributes.exception.type`：异常类型
- `events.attributes.exception.message`：异常消息
- `events.attributes.exception.stacktrace`：异常堆栈

### Step 3：识别耗时异常 Span

对所有 Span 按 `elapsed_time` 排序，找出耗时显著偏高的 Span。对比：
- 该 Span 的耗时 vs 其父 Span 的耗时（占比多少）
- HTTP 调用 Span 的合理耗时预期（通常应 < 数秒）

### Step 4：定位根因

**关键分析逻辑**：Error Span 不一定是根因，需要结合耗时分析：

```
示例：
[Span A] job-execute → POST /service/log/batch (status.code=2, 异常: Read timed out)
  └── [Span B] job-logsvr → POST /service/log/batch (status.code=0, elapsed_time=692秒)
```

分析：
- Span A 标记了 error（`SocketTimeoutException: Read timed out`），是**表象**
- Span B 虽然 status.code=0（正常返回），但耗时 692 秒，远超合理范围，是**根因**
- 结论：job-logsvr 处理请求耗时过长（692 秒），导致 job-execute 侧读超时

> ⚠️ **不要只看 Error Span**：error 是结果（表象），耗时异常才可能是原因（根因）。通过 `parent_span_id` 找到 Error Span 的子 Span，分析其耗时是否异常。

### Step 5：生成分析结论

输出结构化结论：

```markdown
**APM 调用链分析**：
- 调用拓扑：`{服务A}` → `{服务B}` → `{服务C/中间件}`
- Error Span：`{Span名称}`（服务：{service.name}，异常：{exception.type}: {exception.message}）
- 耗时异常 Span：`{Span名称}`（服务：{service.name}，耗时：{elapsed_time换算后}）
- 根因判定：{结合 Error Span 和耗时异常 Span 的综合分析结论}
```

---

## 降级策略

如果 `search_spans` 工具不可用或查询无结果（该请求可能未被 APM 采样到），**跳过 APM 分析步骤，直接从日志分析入手**，不影响整体分析流程。

## 更多示例

详细的查询示例和输出解析，参见 [references/search-spans-examples.md](references/search-spans-examples.md)。
