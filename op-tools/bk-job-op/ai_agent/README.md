# AI Agent 配置

本目录用于存放在平台上快速创建 AI Agents 的相关配置、Prompt 模板和资源文件。

## 目录结构

```
ai_agent/
├── README.md                    # 本说明文件
├── agents/                      # Agent 列表目录
│   └── agent-{name}/            # 单个 Agent 的配置目录
│       ├── prompt-{name}.md     # Agent 的 Prompt 配置
│       └── sub-agent/           # 子 Agent（如有）
│           └── prompt-*.md
├── rules/                       # 可复用的 Rules 规则库
│   └── *.md                     # 规则文件
└── skills/                      # 可复用的 Skills 技能库
    └── skill-{name}/            # 单个 Skill 目录
        ├── SKILL.md             # Skill 主配置文件
        ├── references/          # 参考资料
        ├── examples/            # 示例文件
        └── scripts/             # 脚本文件（如有）
```

## 目录职能说明

### 📁 agents/

**用途**：存放 Agent 列表及其对应的 Prompt 配置

**说明**：
- 每个 Agent 以 `agent-{name}` 的形式命名一个子目录
- 目录内包含该 Agent 的 Prompt 模板（`prompt-*.md`）
- 如果 Agent 需要调用子 Agent，子 Agent 的 Prompt 放在 `sub-agent/` 子目录中
- Agent 的 Prompt 中可以引用 `rules/` 和 `skills/` 中的公共资源

**当前 Agent 列表**：

| Agent 名称 | 目录 | 描述 |
|-----------|------|------|
| alert-handle | `agent-alert-handle/` | 告警处理入口 Agent，负责接收告警、分析错误日志、调度子 Agent 进行深度分析 |
| log-analyze | `agent-log-analyze/` | 日志分析 Agent，负责日志查询和分析 |

---

### 📁 rules/

**用途**：存放可被多个 Agent 复用的 Rules 规则

**说明**：
- Rules 是一组行为规范或处理流程的定义
- 多个 Agent 可以共享同一个 Rule，避免重复定义
- 规则以 Markdown 格式存储，便于阅读和维护

**详细列表**：参见 [rules/README.md](./rules/README.md)

---

### 📁 skills/

**用途**：存放可被多个 Agent 复用的 Skills 技能

**说明**：
- Skills 是一系列指令、脚本和资源的集合
- 可以动态加载，提高 Agent 在特定任务上的性能
- 每个 Skill 以独立目录形式存在，包含 `SKILL.md` 主配置文件
- 支持包含参考资料、示例和脚本等资源

**详细列表**：参见 [skills/README.md](./skills/README.md)

---

## 快速开始

### 创建新 Agent

1. 在 `agents/` 下创建 `agent-{name}/` 目录
2. 编写 `prompt-{name}.md` Prompt 配置文件
3. 如需子 Agent，在 `sub-agent/` 下添加对应 Prompt
4. 在平台上创建 Agent 时，复制 Prompt 内容到配置中

### 创建新 Rule

1. 在 `rules/` 下创建 `{rule-name}.md` 文件
2. 更新 `rules/README.md` 中的规则列表

### 创建新 Skill

1. 使用 `bk-skill-creator` Skill 辅助创建
2. 生成的 Skill 目录放置在 `skills/` 下
3. 更新 `skills/README.md` 中的技能列表


