# 报告输出格式

输出两个文件到工作目录下的 `tmp/pr-security-check-report/`：

- `report.json`：供 CI 门禁消费
- `report.html`：供人类 Reviewer 在浏览器中查看（单文件、内联样式，不依赖外部资源）

正文必须使用中文。

## 严重级别（severity）

| 级别 | 含义 |
|------|------|
| `critical` | 无需认证或低权限即可造成远程代码执行、越权控制主机、大规模数据泄露 |
| `high` | 越权访问/操作其他业务或租户资源、凭证泄露、SQL 注入、SSRF 访问内网 |
| `medium` | 需要特定条件才可利用，或影响范围有限 |
| `low` | 纵深防御不足、信息泄露风险较低 |

## 置信度（confidence）

| 级别 | 含义 |
|------|------|
| `high` | 已在代码中确认完整的利用路径（入口可控 → 无有效防护 → 到达危险点） |
| `medium` | 主要路径已确认，但部分环节依赖推断 |
| `low` | 存在可疑模式，但无法确认可利用性 |

## report.json

```json
{
  "version": "1",
  "base_sha": "",
  "head_sha": "",
  "summary": {
    "finding_count": 0,
    "max_severity": "none",
    "sensitive_paths_changed": []
  },
  "findings": [
    {
      "id": "F-001",
      "type": "missing-authorization",
      "severity": "high",
      "confidence": "high",
      "title": "",
      "file": "",
      "start_line": 0,
      "end_line": 0,
      "description": "",
      "attack_path": "",
      "evidence": "",
      "recommendation": ""
    }
  ],
  "notes": []
}
```

字段说明：

- `base_sha` / `head_sha`：分别对应检查所用的 `lastCommitId`（合并前）与 `headCommitId`（预合并后）
- `type`：漏洞类型，取值见同目录 `checklist.md` 的章节名（如 `missing-authorization`、`sql-injection`）
- `sensitive_paths_changed`：PR 中被修改的敏感路径（`ai_agent/pr_check/`、`.github/workflows/`、构建脚本等）
- `attack_path`：攻击者从哪个入口、以什么身份、如何到达漏洞点
- `evidence`：支撑结论的关键代码位置（`文件:行号`，文字列或字符串数组均可），不要大段粘贴代码；行号必须来自 `{headCommitId}` 下完整源码的真实行号，不得使用 diff 片段的相对行号
- `recommendation`：修复建议必须引用项目中已存在的防护类与方法（如 `authorization.md`、`checklist.md` 中列出的类），不得捏造不存在的类名

## report.html

面向人类 Reviewer 的单页 HTML，内容须与 `report.json` 一致；发现按严重级别降序排列。

### 页面结构

1. **页头**：标题「BK-JOB PR 安全检查报告」、PR 链接、`base_sha` / `head_sha`、生成时间
2. **摘要**：发现总数、最高严重级别、敏感路径变更列表（若有）
3. **发现列表**（有发现时）：每条一个区块，包含：
   - ID、标题
   - 严重级别、置信度、类型（可用色标区分 severity）
   - 位置（`文件:行号`）
   - 描述、攻击路径、证据、修复建议
4. **无发现问题**（无发现时）：明确结论 + 本次覆盖的风险面
5. **备注**（`notes` 非空时）：存量问题等补充说明

### 样式与安全要求

- 使用内联 `<style>`，不引用外部 CSS/JS/字体
- 色标建议：`critical` 深红、`high` 橙红、`medium` 琥珀、`low` 灰蓝
- 所有来自 PR / 代码的文本必须做 HTML 转义（`<`、`>`、`&`、`"`），防止报告本身成为 XSS 载体
- 不嵌入外链图片；PR 链接可用普通 `<a href>` 指向 GitHub

### 骨架示例

```html
<!DOCTYPE html>
<html lang="zh-CN">
<head>
  <meta charset="UTF-8" />
  <title>BK-JOB PR 安全检查报告</title>
  <style>
    body { font-family: sans-serif; line-height: 1.5; max-width: 960px; margin: 24px auto; padding: 0 16px; color: #1f2328; }
    h1 { font-size: 1.5rem; }
    .meta, .summary { color: #656d76; margin-bottom: 1rem; }
    .finding { border: 1px solid #d0d7de; border-radius: 8px; padding: 12px 16px; margin: 12px 0; }
    .sev-critical { border-left: 4px solid #cf222e; }
    .sev-high { border-left: 4px solid #e16f24; }
    .sev-medium { border-left: 4px solid #bf8700; }
    .sev-low { border-left: 4px solid #59636e; }
    .badge { display: inline-block; padding: 0 8px; border-radius: 999px; font-size: 12px; margin-right: 6px; }
    code, pre { background: #f6f8fa; padding: 2px 6px; border-radius: 4px; }
    dt { font-weight: 600; margin-top: 8px; }
  </style>
</head>
<body>
  <h1>BK-JOB PR 安全检查报告</h1>
  <!-- 页头 meta、摘要、发现列表或无问题结论、备注 -->
</body>
</html>
```
