# AI Agent 配置

本目录用于存放在平台上快速创建 AI Agents 的相关配置、Prompt 模板和资源文件。

## 目录结构

```
ai_agent/
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
        ├── references/          # 参考资料
        ├── examples/            # 示例文件
        └── scripts/             # 脚本文件（如有）
```

## 目录职能说明

### 📁 agents/

**用途**：存放 Agent 列表及其对应的 Prompt 配置

**说明**：
- 每个 Agent 创建一个子目录，普通 Agent 以 `agent-` 为前缀，子 Agent 以 `subAgent-` 为前缀
- 目录内包含该 Agent 的 Prompt 配置（`prompt.md`）和说明文档（`README.md`）
- 普通 Agent 和子 Agent 在 `agents/` 目录下同级存放，通过命名前缀区分
- Agent 的 Prompt 中可以引用 `rules/` 和 `skills/` 中的公共资源

**详细列表**：参见 [agents/README.md](./agents/README.md)

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

1. 在 `agents/` 下创建 `agent-{name}/` 目录（子 Agent 使用 `subAgent-{name}/` 前缀）
2. 编写 `prompt.md` Prompt 配置文件和 `README.md` 说明文档
3. 在平台上创建 Agent 时，复制 Prompt 内容到配置中
4. 更新 `agents/README.md` 中的 Agent 列表

### 创建新 Rule

1. 在 `rules/` 下创建 `{rule-name}.md` 文件
2. 更新 `rules/README.md` 中的规则列表

### 创建新 Skill

1. 使用 `bk-skill-creator` Skill 辅助创建
2. 生成的 Skill 目录放置在 `skills/` 下
3. 更新 `skills/README.md` 中的技能列表


