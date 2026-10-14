# 报告输出格式

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

- `type`：漏洞类型，取值见同目录 `checklist.md` 的章节名（如 `missing-authorization`、`sql-injection`）
- `sensitive_paths_changed`：PR 中被修改的敏感路径（`ai_agent/pr_check/`、`.github/workflows/`、构建脚本等）
- `attack_path`：攻击者从哪个入口、以什么身份、如何到达漏洞点
- `evidence`：支撑结论的关键代码位置（`文件:行号`），不要大段粘贴代码

## report.md

面向人类 Reviewer，按严重级别降序列出发现；每条包含标题、位置、攻击路径、修复建议。无发现时输出一句明确的结论以及本次检查覆盖的风险面。
