# 蓝鲸作业平台日志路径说明

本文档详细说明蓝鲸作业平台各服务的日志文件路径和用途。

## 日志目录结构

```
/data/logs/
├── job-execute/          # 作业执行服务
│   ├── job-execute.log   # 主日志
│   ├── gse.log          # GSE 调用日志
│   └── cmdb.log         # CMDB 调用日志
│
├── job-manage/           # 作业管理服务
│   ├── job-manage.log   # 主日志
│   └── cmdb.log         # CMDB 调用日志
│
├── job-crontab/          # 定时任务服务
│   ├── job-crontab.log  # 主日志
│   └── cmdb.log         # CMDB 调用日志
│
├── job-backup/           # 备份服务
│   ├── job-backup.log   # 主日志
│   └── cmdb.log         # CMDB 调用日志
│
├── job-file-gateway/     # 文件网关服务
│   ├── job-file-gateway.log  # 主日志
│   └── gse.log          # GSE 调用日志
│
└── job-logsvr/           # 日志服务
    └── job-logsvr.log   # 主日志
```

## 服务日志说明

### job-execute（作业执行服务）

**主要职责**：执行作业任务、脚本执行、文件分发

**日志文件**：

| 文件 | 路径 | 用途 |
|-----|------|-----|
| 主日志 | `/data/logs/job-execute/job-execute.log` | 任务执行流程、业务逻辑 |
| GSE 日志 | `/data/logs/job-execute/gse.log` | GSE API 调用请求和响应 |
| CMDB 日志 | `/data/logs/job-execute/cmdb.log` | CMDB API 调用请求和响应 |

**常见查询场景**：
- 脚本执行失败 → 查看 GSE 日志
- 主机信息获取失败 → 查看 CMDB 日志
- 任务执行流程分析 → 查看主日志

### job-manage（作业管理服务）

**主要职责**：作业模板管理、执行方案管理、业务配置

**日志文件**：

| 文件 | 路径 | 用途 |
|-----|------|-----|
| 主日志 | `/data/logs/job-manage/job-manage.log` | 模板管理、配置操作 |
| CMDB 日志 | `/data/logs/job-manage/cmdb.log` | CMDB API 调用请求和响应 |

**常见查询场景**：
- 业务信息查询失败 → 查看 CMDB 日志
- 模板操作异常 → 查看主日志

### job-crontab（定时任务服务）

**主要职责**：定时任务调度、周期性任务执行

**日志文件**：

| 文件 | 路径 | 用途 |
|-----|------|-----|
| 主日志 | `/data/logs/job-crontab/job-crontab.log` | 定时任务调度逻辑 |
| CMDB 日志 | `/data/logs/job-crontab/cmdb.log` | CMDB API 调用请求和响应 |

**常见查询场景**：
- 定时任务未触发 → 查看主日志
- 业务配置获取失败 → 查看 CMDB 日志

### job-backup（备份服务）

**主要职责**：作业数据备份、导入导出

**日志文件**：

| 文件 | 路径 | 用途 |
|-----|------|-----|
| 主日志 | `/data/logs/job-backup/job-backup.log` | 备份操作流程 |
| CMDB 日志 | `/data/logs/job-backup/cmdb.log` | CMDB API 调用请求和响应 |

**常见查询场景**：
- 备份失败 → 查看主日志
- 业务信息同步失败 → 查看 CMDB 日志

### job-file-gateway（文件网关服务）

**主要职责**：文件上传下载、文件分发中转

**日志文件**：

| 文件 | 路径 | 用途 |
|-----|------|-----|
| 主日志 | `/data/logs/job-file-gateway/job-file-gateway.log` | 文件传输流程 |
| GSE 日志 | `/data/logs/job-file-gateway/gse.log` | GSE API 调用请求和响应 |

**常见查询场景**：
- 文件传输失败 → 查看 GSE 日志
- 文件上传异常 → 查看主日志

### job-logsvr（日志服务）

**主要职责**：作业执行日志收集、存储、查询

**日志文件**：

| 文件 | 路径 | 用途 |
|-----|------|-----|
| 主日志 | `/data/logs/job-logsvr/job-logsvr.log` | 日志服务运行状态 |

**常见查询场景**：
- 日志收集异常 → 查看主日志

## 外部系统调用日志

### GSE 日志（gse.log）

**GSE（管控平台）**：负责 Agent 管理、脚本执行、文件传输

**日志内容**：
- GSE API 请求参数
- GSE API 响应结果
- 调用耗时统计
- 错误信息

**涉及服务**：
- `job-execute` - 脚本执行、文件分发
- `job-file-gateway` - 文件传输

**查询路径**：
```
path: "*gse.log*"
```

### CMDB 日志（cmdb.log）

**CMDB（配置平台）**：负责业务拓扑、主机信息、业务配置

**日志内容**：
- CMDB API 请求参数
- CMDB API 响应结果
- 调用耗时统计
- 错误信息

**涉及服务**：
- `job-execute` - 主机信息查询
- `job-manage` - 业务配置查询
- `job-crontab` - 业务信息获取
- `job-backup` - 业务数据同步

**查询路径**：
```
path: "*cmdb.log*"
```

## KQL 查询示例

### 查询特定服务的日志

```kql
# 查询 job-execute 的所有日志
path: "*job-execute*"

# 查询 GSE 调用日志
path: "*gse.log*"

# 查询 CMDB 调用日志
path: "*cmdb.log*"
```

### 查询所有服务的特定类型日志

```kql
# 查询所有服务的 GSE 调用日志
path: "*gse.log"

# 查询所有服务的 CMDB 调用日志
path: "*cmdb.log"
```

### 组合查询

```kql
# 查询某个任务的 GSE 调用日志
request_id: "b02df4aec7bd263a9b4f727eb605fad9" AND path: "*gse.log*"

# 查询某个任务的 CMDB 调用错误日志
request_id: "b02df4aec7bd263a9b4f727eb605fad9" AND path: "*cmdb.log*" AND level: ERROR
```

## 日志级别说明

| 级别 | 说明 | 使用场景 |
|-----|------|---------|
| ERROR | 错误日志 | 排查失败原因 |
| WARN | 警告日志 | 分析潜在问题 |
| INFO | 信息日志 | 了解执行流程 |
| DEBUG | 调试日志 | 详细调试分析 |

## 最佳实践

1. **优先查看错误日志**：使用 `level: ERROR` 快速定位问题
2. **根据服务选择路径**：明确问题所在服务，使用精确路径
3. **外部调用问题查专用日志**：GSE/CMDB 问题查看对应的 gse.log/cmdb.log
4. **使用通配符批量查询**：不确定具体服务时使用 `*gse.log` 或 `*cmdb.log`
5. **结合 request_id 精确定位**：始终使用 request_id 缩小查询范围
