# KQL 常用查询示例

本文档提供蓝鲸作业平台日志查询的常用 KQL 语句示例。

## 基础查询

### 1. 查询任务完整链路日志

**场景**：了解任务执行的完整过程

```kql
request_id: "b02df4aec7bd263a9b4f727eb605fad9"
```

**推荐参数**：
- `timeRange`: `1d`
- `size`: `50`
- `asc`: `false`

---

### 2. 查询任务错误日志

**场景**：快速定位任务失败原因

```kql
request_id: "b02df4aec7bd263a9b4f727eb605fad9" AND level: ERROR
```

**推荐参数**：
- `timeRange`: `1d`
- `size`: `50`
- `asc`: `false`

---

### 3. 查询任务启动阶段日志

**场景**：排查任务启动失败问题

```kql
request_id: "b02df4aec7bd263a9b4f727eb605fad9"
```

**推荐参数**：
- `timeRange`: `1h`
- `size`: `50`
- `asc`: `true` ⚠️ 注意：使用升序查看最早的日志

---

## GSE 调用查询

### 4. 查询 GSE 调用日志

**场景**：分析脚本执行或文件传输问题

```kql
request_id: "b02df4aec7bd263a9b4f727eb605fad9" AND path: "*gse.log*"
```

**推荐参数**：
- `timeRange`: `1h`
- `size`: `100`
- `asc`: `false`

---

### 5. 查询 GSE 调用错误

**场景**：排查 GSE 调用失败原因

```kql
request_id: "b02df4aec7bd263a9b4f727eb605fad9" AND path: "*gse.log*" AND level: ERROR
```

**推荐参数**：
- `timeRange`: `1h`
- `size`: `50`
- `asc`: `false`

---

### 6. 查询所有服务的 GSE 调用日志

**场景**：不确定具体哪个服务调用 GSE 时使用

```kql
request_id: "b02df4aec7bd263a9b4f727eb605fad9" AND path: "*gse.log"
```

**推荐参数**：
- `timeRange`: `1h`
- `size`: `100`
- `asc`: `false`

---

## CMDB 调用查询

### 7. 查询 CMDB 调用日志

**场景**：分析主机信息或业务配置查询问题

```kql
request_id: "b02df4aec7bd263a9b4f727eb605fad9" AND path: "*cmdb.log*"
```

**推荐参数**：
- `timeRange`: `1h`
- `size`: `100`
- `asc`: `false`

---

### 8. 查询 CMDB 调用错误

**场景**：排查 CMDB 调用失败原因

```kql
request_id: "b02df4aec7bd263a9b4f727eb605fad9" AND path: "*cmdb.log*" AND level: ERROR
```

**推荐参数**：
- `timeRange`: `1h`
- `size`: `50`
- `asc`: `false`

---

### 9. 查询所有服务的 CMDB 调用日志

**场景**：不确定具体哪个服务调用 CMDB 时使用

```kql
request_id: "b02df4aec7bd263a9b4f727eb605fad9" AND path: "*cmdb.log"
```

**推荐参数**：
- `timeRange`: `1h`
- `size`: `100`
- `asc`: `false`

---

## 特定服务查询

### 10. 查询 job-execute 服务日志

**场景**：分析作业执行服务的问题

```kql
request_id: "b02df4aec7bd263a9b4f727eb605fad9" AND path: "*job-execute*"
```

**推荐参数**：
- `timeRange`: `1d`
- `size`: `50`
- `asc`: `false`

---

### 11. 查询 job-manage 服务日志

**场景**：分析作业管理服务的问题

```kql
request_id: "b02df4aec7bd263a9b4f727eb605fad9" AND path: "/data/logs/job-manage/*"
```

**推荐参数**：
- `timeRange`: `1d`
- `size`: `50`
- `asc`: `false`

---

## 关键词搜索

### 12. 查询包含特定关键词的日志

**场景**：搜索包含特定错误信息或关键词的日志

```kql
request_id: "b02df4aec7bd263a9b4f727eb605fad9" AND message: "*timeout*"
```

**推荐参数**：
- `timeRange`: `1d`
- `size`: `50`
- `asc`: `false`

---

### 13. 查询异常堆栈信息

**场景**：查找包含异常堆栈的日志

```kql
request_id: "b02df4aec7bd263a9b4f727eb605fad9" AND message: "*Exception*"
```

**推荐参数**：
- `timeRange`: `1d`
- `size`: `50`
- `asc`: `false`

---

## 系统级查询

### 14. 查询所有错误日志（无 request_id）

**场景**：查看系统整体错误情况

⚠️ **注意**：此查询可能返回大量数据，建议缩小时间范围

```kql
level: ERROR
```

**推荐参数**：
- `timeRange`: `15m` ⚠️ 使用较短时间范围
- `size`: `50`
- `asc`: `false`

---

### 15. 查询特定时间段的 GSE 错误

**场景**：分析 GSE 调用的整体健康状况

```kql
path: "*gse.log" AND level: ERROR
```

**推荐参数**：
- `timeRange`: `1h`
- `size`: `100`
- `asc`: `false`

---

### 16. 查询特定时间段的 CMDB 错误

**场景**：分析 CMDB 调用的整体健康状况

```kql
path: "*cmdb.log" AND level: ERROR
```

**推荐参数**：
- `timeRange`: `1h`
- `size`: `100`
- `asc`: `false`

---

## 高级查询

### 17. 查询特定 Logger 的日志

**场景**：查看特定类或模块的日志

```kql
request_id: "b02df4aec7bd263a9b4f727eb605fad9" AND logger: "*TaskExecuteService*"
```

**推荐参数**：
- `timeRange`: `1d`
- `size`: `50`
- `asc`: `false`

---

### 18. 查询警告和错误日志

**场景**：查看所有需要关注的日志

```kql
request_id: "b02df4aec7bd263a9b4f727eb605fad9" AND (level: ERROR OR level: WARN)
```

**推荐参数**：
- `timeRange`: `1d`
- `size`: `50`
- `asc`: `false`

---

## 性能分析查询

### 19. 查询慢查询日志

**场景**：分析性能问题

```kql
request_id: "b02df4aec7bd263a9b4f727eb605fad9" AND message: "*cost*"
```

**推荐参数**：
- `timeRange`: `1d`
- `size`: `50`
- `asc`: `false`

---

### 20. 查询数据库操作日志

**场景**：分析数据库操作问题

```kql
request_id: "b02df4aec7bd263a9b4f727eb605fad9" AND message: "*SQL*"
```

**推荐参数**：
- `timeRange`: `1d`
- `size`: `50`
- `asc`: `false`

---

## 查询技巧总结

### 1. 时间范围选择

| 场景 | 推荐时间范围 |
|-----|------------|
| 刚执行的任务 | `15m` 或 `1h` |
| 今天执行的任务 | `1d` |
| 历史任务 | `7d`（最大） |

### 2. 返回数量选择

| 场景 | 推荐数量 |
|-----|---------|
| 快速查看 | `10` |
| 详细分析 | `50` |
| 完整日志 | `100` |

### 3. 排序方式选择

| 场景 | 推荐排序 |
|-----|---------|
| 查看最新日志 | `asc: false`（降序） |
| 查看启动日志 | `asc: true`（升序） |
| 分析时间线 | `asc: true`（升序） |

### 4. 查询优化建议

1. **始终使用 request_id**：避免查询过多无关日志
2. **先查错误，再查详情**：使用 `level: ERROR` 快速定位
3. **使用精确路径**：明确服务时使用完整路径
4. **合理设置时间范围**：避免查询过长时间的日志
5. **控制返回数量**：避免返回过多数据影响性能
