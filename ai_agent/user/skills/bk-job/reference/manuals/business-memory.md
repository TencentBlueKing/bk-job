# 业务记忆协议

本技能在 **`memory/businesses/`** 下按资源范围存储**可选**知识，用于预填 `bk_scope`、推荐执行方案与参数说明。执行与查询仍通过 [`scripts/job_apigw_client.py`](../../scripts/job_apigw_client.py) 完成；资源范围为 **`bk_scope_type` + `bk_scope_id`**（业务 `biz` 或业务集 `biz_set`）。

---

## 1. 记忆存储与文件命名

### 1.1 基准路径

记忆文件存放在**本技能根目录**（与 [`SKILL.md`](../../SKILL.md) 同级）下的 `memory/` 目录。

> **基准路径**：`{SKILL_DIR}/memory/`  
> `{SKILL_DIR}` 为存放本技能 `SKILL.md` 的目录（与 IDE/智能体无关：可能是 `.cursor/skills/bk-job`、`.codebuddy/skills/bk-job`、OpenClaw 技能目录下的 `bk-job` 等，以实际同步位置为准）。

```
{SKILL_DIR}/
├── SKILL.md
├── memory/
│   └── businesses/
│       ├── _template.md
│       └── {范围标识}.md
```

### 1.2 记忆文件命名

| 场景 | 建议文件名 | 说明 |
|------|------------|------|
| 仅业务或仅业务集，且 ID 不混用 | `{bk_scope_id}.md` | 例：`309.md` |
| 同一数字 ID 可能同时出现在 biz 与 biz_set | `{bk_scope_type}_{bk_scope_id}.md` | 例：`biz_309.md`、`biz_set_9991001.md` |

选用一种规则后，在同一仓库内保持一致。

---

## 2. 记忆模板

可复制 [`memory/businesses/_template.md`](../../memory/businesses/_template.md) 为具体范围文件后填写。

```markdown
# 资源范围 {bk_scope_type} / {bk_scope_id}

## 基础信息
- 资源范围类型（biz / biz_set）：
- 资源范围 ID：
- 显示名称（可选）：

## 常用执行方案
| 方案名称 | 方案 ID | 用途 | 典型参数 |
|---------|---------|------|---------|

## 参数偏好
（该范围下常用的 global_var_list 片段或说明）

## 已知问题
（该范围历史上执行问题与规避方式）
```

---

## 3. 加载流程（强制检查点）

> **🚫 硬性规则：在需要利用「业务上下文」预填 `bk_scope`、推荐方案或参数前，应完成下列加载步骤。**

**执行步骤**：

1. **确定资源范围**  
   从用户消息提取 `bk_scope_type`、`bk_scope_id`，缺失则询问。

2. **尝试读取业务记忆文件**  
   按 **1.2** 规则解析路径，例如 `{SKILL_DIR}/memory/businesses/309.md` 或 `biz_set_9991001.md`。  
   - 文件存在 → 加载内容，`memory_biz_loaded = true`  
   - 不存在 → `memory_biz_loaded = false`（**不阻塞**查询/执行流程）

3. **记忆状态判定**  
   - `true` → 成长期/成熟期：可预填范围 ID、推荐方案、参数偏好（须在 [confirmation-and-output-protocol.md](confirmation-and-output-protocol.md) **1.4** 确认摘要中标注来源 **[业务记忆预填]**）  
   - `false` → 初识期：流程结束后可按 **5** 引导沉淀（每次最多一条，避免打扰）

**加载后的使用**：

- 预填仅限用户已确认或低歧义字段；**启动执行**仍须完整走确认门禁（G1–G4）。  
- 无记忆时完全依赖用户输入与 `plan-detail` 中的方案默认值。

---

## 4. 引导阶段模型

| 阶段 | 判断条件 | 引导策略 |
|------|---------|---------|
| **初识期** | 该范围无记忆文件 | 流程结束后引导建立基础记忆，每次只问 1～2 个关键信息 |
| **成长期** | 有文件但信息不完整 | 在操作中自然补充 |
| **成熟期** | 信息较充分 | 利用记忆提高效率；偶尔确认是否更新 |

---

## 5. 沉淀触发与写入规则

### 5.1 触发时机

| 时机 | 引导话术（示例） |
|------|------------------|
| **首次操作某范围**（`memory_biz_loaded = false`） | 「这是第一次在 {bk_scope_type}/{bk_scope_id} 上操作，要不要记一条基础信息方便下次用？」 |
| **用户填写了非默认参数** | 「`{参数}={值}` 是这个范围下的常用值吗？要写入业务记忆吗？」 |
| **重复执行同一方案** | 「这个方案最近多次执行，要记成该范围的常用操作吗？」 |

### 5.2 写入规则

1. 展示拟写入内容 → **用户确认** → 再写入文件。  
2. 若 `memory/businesses/` 不存在，可创建目录后再写。  
3. **追加或按段补充**，避免无故覆盖整文件历史；重大变更可征求用户是否替换某节。  
4. **Git**：（可选）若团队将技能记忆纳入版本库，可在用户同意后将新文件或变更 `git add`；**禁止**在未经用户同意时提交敏感参数明文。

---

## 相关手册

- 确认摘要中的 **[业务记忆预填]**：[confirmation-and-output-protocol.md](confirmation-and-output-protocol.md) **1.4**  
- 执行方案搜索与启动：[job-plans-search-and-execute.md](job-plans-search-and-execute.md)  
- 列举与 Token：[listing-and-token-efficient-analysis.md](listing-and-token-efficient-analysis.md)  
