# AI Agent 配置（普通用户）

本目录（`ai_agent/user/`）专门用于存放**蓝鲸作业平台（bk-job）普通用户场景**相关的 AI Agent 配置、Prompt 模板和资源文件。

> **目录定位**：项目根目录下的 `ai_agent/` 按业务场景划分子目录，`user/` 子目录聚焦于普通用户使用场景，帮助用户借助 AI Agent 通过自然语言快捷使用作业平台功能。包括但不限于：搜索并执行作业方案、管理定时任务、查询执行结果等场景。

## 目录结构

```
ai_agent/user/
├── README.md                    # 本说明文件
├── agents/                      # Agent 列表目录
│   ├── agent-{name}/            # 普通 Agent 的配置目录
│   │   ├── README.md            # Agent 说明文档
│   │   └── prompt.md            # Agent 的 Prompt 配置
│   └── subAgent-{name}/         # 子 Agent 的配置目录（命名以 subAgent- 为前缀）
│       ├── README.md            # Agent 说明文档
│       └── prompt.md            # Agent 的 Prompt 配置
├── rules/                       # 可复用的 Rules 规则库
│   └── *.md                     # 规则文件
└── skills/                      # 可复用的 Skills 技能库
    └── {skill-name}/            # 单个 Skill 目录
        ├── SKILL.md             # Skill 主配置文件
        └── references/          # 参考资料（可选）
```

## 设计理念

本目录下的 Agent/Skill 采用 **Skill + MCP 混合** 架构：

- **Skill（知识层）**：提供场景引导、工作流 SOP、最佳实践，告诉 AI "什么场景该调什么 tool、参数怎么填、结果怎么解读"
- **MCP（工具层）**：提供实际可调用的 tool，封装 API 请求、认证、错误处理等

### 依赖的 MCP Server

| MCP Server | 功能 | 包含的 Tools |
|------------|------|-------------|
| `jobv3-cloud-prod-mcp-resources` | 资源查询 | `get_job_plan_list`, `get_job_plan_detail`, `get_cron_list` |
| `jobv3-cloud-prod-mcp-start` | 启动操作 | `execute_job_plan` |
| `jobv3-cloud-prod-mcp` | 任务查询 | `v4_get_job_instance_status`, `v4_batch_get_job_instance_execute_object_log` |

## 快速开始

### 创建新 Skill

1. 在 `skills/` 下创建 `{skill-name}/` 目录
2. 编写 `SKILL.md` 主配置文件
3. 更新本 README 中的技能列表

### 创建新 Agent

1. 在 `agents/` 下创建 `agent-{name}/` 目录
2. 编写 `prompt.md` 和 `README.md`

## Skills 列表

| Skill                                                                            | 描述 |
|----------------------------------------------------------------------------------|------|
| [bk-job-search-and-execute-job-plan](skills/bk-job-search-and-execute-job-plan/) | 根据关键词搜索执行方案并启动，支持查看执行结果 |
