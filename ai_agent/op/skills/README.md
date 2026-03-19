# Skills 技能库

本目录存放可被多个 Agent 复用的 Skills 技能。

## 什么是 Skills？

Skills 是一系列指令、脚本和资源的集合，可以动态加载，从而提高 Agent 在特定任务上的性能。每个 Skill 通常包含：
- `SKILL.md`：Skill 主配置文件，定义能力和使用方式
- `references/`：参考资料目录
- `examples/`：示例文件目录
- `scripts/`：脚本文件目录（如有）

## 当前 Skills 列表

| Skill 名称 | 目录 | 描述 |
|-----------|------|------|
| apm-trace-analysis | `apm-trace-analysis/` | 蓝鲸监控 APM 调用链分析专家。通过 `search_spans` 工具查询和分析 APM Span 数据，定位调用链中的 Error Span 和耗时异常 |
| kql-query-guide | `kql-query-guide/` | 蓝鲸作业平台日志查询专家。提供 KQL 语法指导和常用查询模板，用于编写日志查询语句 |
| task-duration-analysis | `task-duration-analysis/` | 蓝鲸作业平台任务耗时分布分析专家。基于六阶段模型分析任务各阶段耗时分布，定位性能瓶颈 |

## 各 Skill 详情

### 📦 apm-trace-analysis

**能力**：
- 通过 trace_id/request_id 查询完整调用链
- 定位调用链中的 Error Span 和耗时异常 Span
- 分析微服务间的调用关系和错误传播路径

**MCP 工具**：`bkmonitor-tracing` → `search_spans`

---

### 📦 kql-query-guide

**能力**：
- 编写 KQL 查询语句查询作业执行日志
- 排查任务执行失败问题
- 分析 GSE/CMDB 调用问题
- 查看任务完整链路日志

**MCP 工具**：`v3-job-op` → `searchLogsByCondition`

---

### 📦 task-duration-analysis

**能力**：
- 分析任务各阶段耗时分布
- 定位任务执行中的性能瓶颈
- 了解任务执行链路和各阶段涉及的中间件
- 生成任务耗时分布统计报告

**依赖**：基于六阶段模型（参见 `rules/任务耗时分布几个阶段.md`）

---

## 使用方式

在平台上使用 Skill：
1. 通过 `use_skill` 工具调用对应 Skill
2. Skill 的 Prompt 会自动展开并提供专业指导

## 创建新 Skill

推荐使用 `bk-skill-creator` Skill 辅助创建：
1. 调用 `bk-skill-creator` Skill
2. 按照向导完成 Skill 配置
3. 生成的 Skill 目录自动放置在本目录下
4. 更新本 README 中的技能列表
