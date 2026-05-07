# 列表默认条数与节省 Token 的分析方式

## 1. 列举资源：先小后大

当用户问「有哪些执行方案 / 定时任务 / …」且**未明确要求全量、导出或总数统计**时：

- **不要**一上来使用很大的 `--length`（如 1000）或反复分页把全部结果塞进对话。
- **应**使用脚本默认值或显式 **`--length 20`**（与 `cron-search` / `plan-search` 默认一致），必要时配合 `--keyword` 缩小范围。
- 若 `total` 大于本页条数，在回复中说明「当前仅展示最近 / 本页 N 条，共 M 条」，并询问用户是否需要：下一页（增大 `--start`）、更大 `length`、或指定关键词/ID。

**仅当用户明确要**「全部列出」「导出完整清单」「有多少条」**时**，再分页拉取；拉取后仍应用第 2 节方式在**本地**收敛后再摘要给用户。

## 2. 大列表分析：不要整段 JSON 进上下文

对列表类 JSON（`plan-search` / `cron-search` / 其它大响应）：

- **禁止**习惯地把完整 stdout 粘贴进模型上下文再「慢慢看」。
- **应**在运行环境中用 **命令行工具在本地过滤**，只把**过滤后的少量行或统计结果**写入回复。

### 2.1 推荐：`subprocess` 同进程拉取并裁剪（跨平台、避免管道编码问题）

在一段 `python -c` 里调用 `job_apigw_client` 同级逻辑最稳；更简单的是 **子进程执行脚本 + `capture_output` + UTF-8 解码**，再在内存里取字段：

```python
import json, subprocess, sys

def run_plan_search(scope_type, scope_id, keyword=None, length=20, start=0):
    cmd = [
        sys.executable, "scripts/job_apigw_client.py", "plan-search",
        "--bk-scope-type", scope_type,
        "--bk-scope-id", str(scope_id),
        "--start", str(start),
        "--length", str(length),
    ]
    if keyword:
        cmd += ["--keyword", keyword]
    p = subprocess.run(cmd, cwd=r"<技能根目录>", capture_output=True, text=True, encoding="utf-8")
    p.check_returncode()
    return json.loads(p.stdout)

data = run_plan_search("biz_set", "9991001", length=20)
for row in data.get("data", []):
    print(row["id"], row["name"])
```

将 `<技能根目录>` 换为 **本技能包根目录**（与 `SKILL.md` 同级，内含 `scripts/job_apigw_client.py`）的绝对路径；或先 `cd` 到该目录再执行。宿主可为 Cursor、CodeBuddy、OpenClaw 等任意部署路径。只把最后的 `print` 结果或自定义摘要发给用户。

### 2.2 使用 `jq`（已安装时）

先将脚本输出写入 **UTF-8 文件**（Windows 下避免 PowerShell 管道破坏编码，可用 `cmd /c "chcp 65001>nul && python ... > out.json"`），再：

```bash
jq '.total, (.data | length)'
jq -r '.data[] | "\(.id)\t\(.name)"' out.json
```

### 2.3 使用 `python` 读文件

```bash
python -c "import json; d=json.load(open('out.json',encoding='utf-8')); print(d['total']); [print(x['id'], x['name']) for x in d['data']]"
```

### 2.4 回复用户时的原则

- 优先 **表格或条目列表**（仅关键列：`id`、`name`、`status`/`启停状态` 等）。
- 需要细节再 **`plan-detail` / `cron-last-run`** 针对单条拉取，而不是把整页原始 JSON 重复贴出。

## 3. 与脚本参数的对应关系

| 子命令 | 默认 `--length` | 接口上限（参考） |
|--------|-----------------|------------------|
| `cron-search` | 20 | 1000 |
| `plan-search` | 20 | 1000 |
| `cron-last-run`（内部搜定时任务） | 20 | 同左 |

分页：增大 `--start`，保持合理 `--length`。

## 4. 相关手册

- [cron-tasks-and-last-execution.md](cron-tasks-and-last-execution.md)  
- [job-plans-search-and-execute.md](job-plans-search-and-execute.md)  
