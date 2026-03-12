---
name: kql-query-guide
description: 蓝鲸作业平台日志查询专家。当需要：(1) 编写 KQL 查询语句查询作业执行日志，(2) 排查任务执行失败问题，(3) 分析 GSE/CMDB 调用问题，(4) 查看任务完整链路日志时使用。提供 KQL 语法指导和常用查询模板。
license: Apache-2.0
---

# KQL Query Guide - 日志查询语句编写指导

帮助用户正确编写 KQL (Kibana Query Language) 查询语句，用于蓝鲸作业平台的日志查询。

## 核心原则

> **使用正确的 KQL 语法和字段名，结合业务场景选择合适的查询条件。**

## 快速开始

### 基础查询模板

```bash
# 1. 查询任务完整链路日志
request_id: "{request_id}"

# 2. 查询任务错误日志
request_id: "{request_id}" AND level: ERROR

# 3. 查询 GSE 调用日志
request_id: "{request_id}" AND path: "*gse.log*"

# 4. 查询 CMDB 调用日志
request_id: "{request_id}" AND path: "*cmdb.log*"
```

### 使用脚本生成查询语句

```bash
# 生成基础查询语句
python3 scripts/generate_kql.py --request-id "b02df4aec7bd263a9b4f727eb605fad9"

# 生成错误日志查询
python3 scripts/generate_kql.py --request-id "b02df4aec7bd263a9b4f727eb605fad9" --level ERROR

# 生成 GSE 调用日志查询
python3 scripts/generate_kql.py --request-id "b02df4aec7bd263a9b4f727eb605fad9" --service gse

# 生成 CMDB 调用日志查询
python3 scripts/generate_kql.py --request-id "b02df4aec7bd263a9b4f727eb605fad9" --service cmdb
```

## KQL 语法要点

### 1. 字段查询

```
字段名: "值"
```

**常用字段**：
- `request_id` - 请求 ID（任务执行的唯一标识）
- `level` - 日志级别（ERROR, WARN, INFO, DEBUG）
- `path` - 日志文件路径
- `message` - 日志消息内容
- `logger` - 日志记录器名称

### 2. 组合查询

使用 `AND` 连接多个条件：

```
条件1 AND 条件2 AND 条件3
```

### 3. 通配符匹配

使用 `*` 进行模糊匹配：

```
path: "*cmdb.log"
message: "*timeout*"
```

## 常见查询场景

### 场景 1：排查任务执行失败

**步骤**：
1. 先查询错误日志定位问题
2. 根据错误信息决定是否需要查看 GSE/CMDB 调用日志

**查询语句**：
```
request_id: "{request_id}" AND level: ERROR
```

### 场景 2：分析 GSE 调用问题

**适用情况**：
- 脚本执行超时
- 文件传输失败
- Agent 连接问题

**查询语句**：
```
request_id: "{request_id}" AND path: "*gse.log*"
```

### 场景 3：分析 CMDB 调用问题

**适用情况**：
- 主机信息获取失败
- 拓扑查询异常
- 业务信息不正确

**查询语句**：
```
request_id: "{request_id}" AND path: "*cmdb.log*"
```

### 场景 4：查看任务完整链路

**适用情况**：
- 了解任务执行全过程
- 分析性能问题
- 追踪调用链路

**查询语句**：
```
request_id: "{request_id}"
```

## 服务日志路径

| 服务名 | GSE 日志路径 | CMDB 日志路径 |
|-------|------------|-------------|
| job-execute | `/data/logs/job-execute/gse.log` | `/data/logs/job-execute/cmdb.log` |
| job-manage | - | `/data/logs/job-manage/cmdb.log` |
| job-backup | - | `/data/logs/job-backup/cmdb.log` |
| job-crontab | - | `/data/logs/job-crontab/cmdb.log` |
| job-file-gateway | `/data/logs/job-file-gateway/gse.log` | - |
| job-logsvr | - | - |

**详细说明**：查看 [bk-job-log-paths.md](references/bk-job-log-paths.md)

## 查询参数配置

### timeRange（时间范围）

```
1d  - 最近 1 天（默认）
1h  - 最近 1 小时
15m - 最近 15 分钟
7d  - 最近 7 天（最大）
```

### size（返回条数）

```
10   - 默认值
50   - 推荐用于详细分析
100  - 最大值（避免返回过多数据）
```

### asc（排序方式）

```
false - 降序（默认，最新日志在前）
true  - 升序（最早日志在前，适合查看任务启动阶段）
```

## 最佳实践

1. **先查错误，再查详情**：优先使用 `level: ERROR` 定位问题
2. **合理设置时间范围**：根据任务执行时间选择合适的 `timeRange`
3. **控制返回数量**：使用 `size` 参数避免返回过多日志
4. **使用升序查看启动日志**：排查启动阶段问题时设置 `asc: true`
5. **组合查询精确定位**：使用 `AND` 组合多个条件缩小范围

## 注意事项

⚠️ **重要提醒**：

1. **引号使用**：`request_id` 的值必须用双引号包裹
2. **大小写敏感**：字段名和关键字（如 `AND`、`ERROR`）需要注意大小写
3. **路径精确匹配**：`path` 字段需要完整的日志文件路径
4. **避免过大查询**：不要在没有 `request_id` 的情况下查询所有日志
5. **时间范围限制**：最大支持查询 7 天内的日志

## 示例库

查看更多查询示例：[examples/common-queries.md](examples/common-queries.md)

## 相关工具

- MCP 工具：`searchLogsByCondition` - 执行日志查询
- MCP 工具：`searchRequestIdByStepInstanceId` - 通过步骤实例 ID 查询 request_id
