# 交互式选择（ai-hub-ask-user-input）

本技能大量需要用户做选择：选业务范围、选主机、选账号、多条匹配时选目标、以及写操作的确认门禁。运行环境中若装有 **`ai-hub-ask-user-input`** 技能（imate 数字分身），应优先用它弹出结构化选择 UI，否则回退到表格。

## 1. 何时用它，何时用表格

| 条件 | 呈现方式 |
|------|---------|
| `ai-hub-ask-user-input` 可用 **且** 选项数 ≤ 8 | **用该技能**发送结构化选项 |
| 选项数 > 8 | 表格呈现，并先用 `--keyword` 等条件收敛候选；收敛到 8 项以内后可改用结构化选择 |
| 该技能不可用 | 表格呈现，让用户回复序号或名称 |
| 需要自由文本（如输入目标路径、Crontab 表达式） | 直接自然语言追问，本技能不适用 |

判断可用性：检查运行环境 skills 根目录下是否存在 `ai-hub-ask-user-input` 目录（Windows 用 `Test-Path`，Linux/macOS 用 `test -d`）。**不可用不是错误**，直接回退表格，不要向用户报错或解释。

> 选项数超过 8 时不要硬塞进选择 UI：选项过多会让用户难以辨别，此时**先缩小范围**（加关键词、限定拓扑节点、缩短时间窗口）比直接罗列更有价值。

## 2. 调用方式

以该技能安装目录下的脚本为准，`context_key` 取自会话上下文的 `[ai-hub-context]`：

**Windows**

```powershell
$script = Join-Path "<AI_HUB_SKILLS_ROOT>" "ai-hub-ask-user-input\scripts\ask-user-input.ps1"
$questions = @(
  @{ question = "请选择要执行的目标业务"; options = @("蓝鲸(2)", "测试业务(3)"); type = "single_select" }
) | ConvertTo-Json -Depth 8 -Compress
& $script -ContextKey "<context_key>" -QuestionsJson $questions
```

**Linux/macOS**

```bash
bash "<AI_HUB_SKILLS_ROOT>/ai-hub-ask-user-input/scripts/ask-user-input.sh" \
  --context-key "<context_key>" \
  --questions-json '[{"question":"请选择要执行的目标业务","options":["蓝鲸(2)","测试业务(3)"],"type":"single_select"}]'
```

类型取值：`single_select`（默认）、`multi_select`、`rank_priorities`、`confirm`。发送成功后**停止执行后续依赖该选择的步骤**，等待用户下一条消息，用户答案以 `Q: ... / A: ...` 纯文本回传，禁止猜测结果。

## 3. 本技能中的典型用法

| 场景 | 类型 | 选项建议 |
|------|------|---------|
| 选资源范围（`list-authorized-scopes`） | `single_select` | `业务名(ID)`，便于用户辨认 |
| 选执行账号（`account-list`） | `single_select` | `别名(用途)`，如 `root(系统账号)` |
| 选主机（`host-search` 候选 ≤ 8） | `single_select` / `multi_select` | `IP(主机名)`；多台目标机用多选 |
| 多条匹配选定时任务/执行方案 | `single_select` | `名称(ID)` |
| 写操作确认门禁（G2） | **`confirm`** | 见下节 |

选项文案要能唯一定位对象（**带 ID 或 IP**），不要只给名称，避免同名歧义。

## 4. 与确认门禁配合（重要）

写操作确认必须用 `type="confirm"`，且**位置语义固定**：`options[0]` 是同意执行，`options[1]` 是取消。

```json
[{"question":"即将在 2 台目标机执行脚本（详见上方参数摘要），是否确认执行？","options":["确认执行","取消"],"type":"confirm"}]
```

**门禁不因换了交互形式而放松**，务必同时满足：

1. **先展示完整参数摘要**（含默认值，见 [confirmation-and-output-protocol.md](confirmation-and-output-protocol.md) 1.4），再发确认卡片；**不能**只发一个确认按钮而不展示参数。
2. 发送后**停止**，等待用户回复；用户选中 `options[0]`（同意项）才视为确认通过。
3. 摘要与真实执行**不得在同一轮**完成。
4. 一次确认只授权一次执行；重复执行须重新展示摘要并重新发确认。
5. 用户未作答、或答案无法解析时，**按未确认处理**，不得执行。

## 5. 注意事项

- 发送选择卡片属于内部动作，**不要**向用户叙述"正在调用 xx 技能"，也不要展示脚本输出。
- 卡片发送失败（服务不可用、`context_key` 过期等）时，直接回退为表格呈现，不要因此中断任务。
- `confirm` 的两个选项文案要成对且互斥，让用户一眼看出哪个是放行、哪个是拦住；**严禁**把取消项放在第一位。
- 选择结果影响的是参数取值，**不改变**任何门禁与只读/写操作的划分。
