# Agents 列表

本目录存放所有 Agent 和 Sub Agent 的配置，包括 Prompt 模板和说明文档。

## 命名规范

- **普通 Agent**：目录以 `agent-` 为前缀，如 `agent-alert-handle/`
- **子 Agent（Sub Agent）**：目录以 `subAgent-` 为前缀，如 `subAgent-error-stack-analyze/`

## 当前 Agent 列表

| Agent 名称            | 类型        | 目录                              | 描述                                      |
|---------------------|-----------|---------------------------------|-----------------------------------------|
| alert-handle        | Agent     | `agent-alert-handle/`           | 告警分析调度中心，处理监控平台告警事件，调度子 Agent 进行深度分析    |
| log-analyze         | Agent     | `agent-log-analyze/`            | 通用日志分析专家，提供任务日志查询、耗时分析、异常排查等全方位服务       |
| error-stack-analyze | Sub Agent | `subAgent-error-stack-analyze/` | 错误堆栈分析专家，针对单个请求进行 APM + 日志 + 源码的三维度根因分析 |

> 💡 各 Agent 的详细信息（依赖、架构、工作流程等）请查看各自目录下的 `README.md`。

## 创建新 Agent

1. 在本目录下创建 `agent-{name}/` 目录（子 Agent 使用 `subAgent-{name}/` 前缀）
2. 编写 `prompt.md` Prompt 配置文件
3. 编写 `README.md` 说明文档（包含依赖声明、用途、架构、工作流程等）
4. 在平台上创建 Agent 时，复制 Prompt 内容到配置中
5. 更新本 README 中的 Agent 列表
