# Skill 资源目录

本目录下每一个子目录对应 AIDEV 平台上的一个 Skill，Skill code 在 `skill.yaml` 中声明，由 `bkai-init` 同步到平台。
code 需小写英文字母开头，其余为小写英文、数字、下划线或中划线，最长 64 位。

目录结构示例：

```plain
skills/
└── job_skill/            # Skill code
    ├── skill.yaml        # Skill 定义：code、名称、版本、可见性
    ├── SKILL.md          # 必需，Skill 内容与环境变量依赖声明
    └── Dockerfile        # 可选，仅在需要自定义镜像时提供
```

## 维护约定

- 新增 Skill 后，需要在 `../bkai.yaml` 的 `resources` 中补上 `skills/<code>`，且要排在引用它的 Agent 之前，否则不会被同步或同步顺序出错；
- 版本号写成 `x.x.x` 字符串，**相同版本会直接覆盖线上内容**，不要求递增；未在 `skill.yaml` 中声明时以 `bkai.yaml` 的 `metadata.version` 兜底；
- 若需要被 Agent 引用，还需在 `../agents/bk_job_ai.yaml` 的 `spec.skills` 中登记该 code，并按需通过 `envs` 注入运行变量；
  `envs[].key` 必须已在 Skill 的环境变量依赖中声明，且 `envs: []` 不等于清空线上已有变量；
- Skill 当前不涉及自定义镜像，无需在 Helm Chart 中上报镜像信息；
- 不要在 Skill 文件中写入 Token、Secret 等敏感凭证。

> 当前尚未纳入任何 Skill，本文件仅用于占位与说明。
