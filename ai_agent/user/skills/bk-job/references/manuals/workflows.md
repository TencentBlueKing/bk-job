# 常用组合工作流程

下列**只是常见示例，不是能力边界**：可按用户实际需求，用 `SKILL.md`「支持的原子能力」表里的子命令自由组装出未被列举的流程。**能力上的灵活性不带来门禁上的任何豁免**——链路里只要出现写操作，就必须在真实调用前走完 G1–G4，详见 [confirmation-and-output-protocol.md](confirmation-and-output-protocol.md) 1.6.3。

各流程中的「确认」均指门禁的两轮确认：**先展示含全部参数（含默认值）的摘要，再等用户下一条独立回复**。候选项 ≤ 8 且 `ai-hub-ask-user-input` 可用时，用结构化选项让用户选，见 [interactive-choice.md](interactive-choice.md)。

## 1. 执行类

### 1.1 快速执行脚本

```
host-search 定位目标机 → account-list 选账号 → fast-execute-script --dry-run 出摘要
  → 确认 → 真实执行 → 交付 job_instance_url → 清理 tmp/ 入参文件
```

用户未给主机或账号时**先查再填**，不要凭空猜。详见 [fast-execute-script.md](fast-execute-script.md)。

### 1.2 分发本地文件

```
gen-local-upload-url → upload-local-file 上传（成功后立即清理 tmp/ 中的副本）
  → fast-transfer-file 引用返回的 path --dry-run → 确认 → 分发 → 清理剩余临时文件
```

前两步是准备动作，**不过门禁**；只有 `fast-transfer-file` 真实调用需要确认。上传后的本地副本要立即清理，见 [temp-files.md](temp-files.md)。

### 1.3 分发服务器文件

```
定位源机与源账号 → 定位目标机、目标账号与目标目录
  → fast-transfer-file --server-file-list <源机上的绝对路径> --dry-run → 确认 → 分发
```

`--server-file-list` 填的是**源机器上的绝对路径**，不是上传返回的 path；源机与源账号必填。详见 [file-transfer.md](file-transfer.md)。

### 1.4 搜方案并启动

```
plan-search → plan-detail 核对必填变量与主机变量结构 → 组装 global_var_list
  → plan-execute --dry-run → 确认 → 启动 → 交付 job_instance_url
```

主机类变量的字段结构因接口而异，组装前先查 [job-plans-search-and-execute.md](job-plans-search-and-execute.md) 的差异表。

## 2. 创建类

### 2.1 查模板建方案

```
template-search → template-detail 看步骤列表与全局变量
  → 与用户确认启用哪些步骤及变量取值 → plan-create --dry-run → 确认 → 创建
  → 交付 job_plan_url
```

详见 [job-plans-create-and-cron.md](job-plans-create-and-cron.md)。

### 2.2 建定时任务并启用

```
plan-search 定位执行方案 → 与用户确认 Crontab 表达式与时区
  → cron-save --dry-run → 确认 → 保存（新建默认暂停）
  → 再单独询问是否启用 → cron-update-status --status 1
```

**保存与启用是两个写操作**，各需一次确认；不得在保存后自动启用。见 [confirmation-and-output-protocol.md](confirmation-and-output-protocol.md) 1.7。

## 3. 查询类

### 3.1 查定时任务与执行历史

```
cron-search 找任务（展示时必须带启停状态）
  → cron-last-run 取最近一次「定时触发」执行的状态与各步骤日志
```

只统计定时触发；需要页面/API 触发或多条历史时改用 `instance-list`。见 [cron-tasks-and-last-execution.md](cron-tasks-and-last-execution.md)。

### 3.2 查执行历史并下钻

```
instance-list 按时间窗口与状态/类型/执行人筛出实例
  → instance-status 看整体与各步骤状态 → get-instance-log 取步骤日志
```

排查失败任务常用 `--status 4` 加 `--lookback-days`。见 [job-instance-status.md](job-instance-status.md)。

## 4. 组装新流程时的检查项

| 检查项 | 要求 |
|--------|------|
| 资源范围 | 已确定 `bk_scope_type` / `bk_scope_id`，切换业务后所有资源重新查询 |
| 主机与账号 | 用户未指定时先查再填，列候选让用户确认 |
| 写操作 | 每个写操作各自过一次门禁，摘要含全部参数（含默认值） |
| 只读环节 | 查询与 `--dry-run` 可在一轮内连续完成，无需逐个确认 |
| 临时文件 | 入参文件写 `tmp/`，操作触发后即清 |
| 结果交付 | 无论成败都以可点击链接交付 `job_instance_url` / `job_plan_url` |
