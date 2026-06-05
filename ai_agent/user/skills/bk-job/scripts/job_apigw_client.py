#!/usr/bin/env python3
"""
蓝鲸作业平台 API 网关 Python 客户端

通过蓝鲸 API 网关调用作业平台开放接口，支持：
- 定时任务检索与最近一次定时执行的状态、日志聚合查询
- 定时任务新建/保存与启停状态更新
- 作业模板检索与详情查询（用于支撑「创建执行方案」前的步骤与变量探查）
- 执行方案检索、详情、创建与启动执行
- 作业实例状态查询与步骤执行日志获取

PowerShell 用户注意：传递 JSON 参数时，推荐用文件方式避免转义问题：
  --variables-file <文件路径>  代替  --variables <JSON>
  --global-vars-file <文件路径>  代替  --global-vars <JSON>

依赖 Python 标准库；鉴权使用用户态 access_token。

用法:
    python job_apigw_client.py <command> [options]

认证:
    --access-token 或环境变量 BK_JOB_ACCESS_TOKEN

网关与页面 URL:
    从技能根目录 config.yaml 读取 apigw_base_url、job_base_url（部署技能包时修改 config.yaml，不读环境变量）

跨平台与中文输出:
    启动时会将 stdout/stderr 设为 UTF-8，减轻 Windows 控制台中文乱码；与 macOS/Linux 兼容。
    作业执行历史查询回溯天数硬上限见常量 MAX_JOB_HISTORY_LOOKBACK_DAYS（当前 31 天）。
    列表类接口默认每页条数见 LIST_PAGE_DEFAULT（与技能「先 20 条」约定一致）。
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from pathlib import Path
from typing import Any, Dict, List, Optional, Tuple
import urllib.error
import urllib.parse
import urllib.request


# -----------------------------------------------------------------------------
# 常量
# -----------------------------------------------------------------------------

# 查询作业执行历史（v4 get_job_instance_list）时，回溯天数硬上限（含）
MAX_JOB_HISTORY_LOOKBACK_DAYS = 31

# 列表类接口（定时任务、执行方案）默认每页条数：与技能「先给最近 20 条」约定一致
LIST_PAGE_DEFAULT = 20

# 网关文档 get_cron_list：1 已启动、2 已暂停、0 已暂停；其它取值以控制台为准
CRON_RUN_STATUS_TEXT = {
    1: "已启动",
    2: "已暂停",
    0: "已暂停"
}

TASK_STATUS_LABEL = {
    1: "等待执行",
    2: "正在执行",
    3: "执行成功",
    4: "执行失败",
    5: "跳过",
    6: "忽略错误",
    7: "等待用户",
    8: "手动结束",
    9: "状态异常",
    10: "强制终止中",
    11: "强制终止成功",
    13: "确认终止",
    14: "被丢弃",
    15: "滚动等待",
}


def _task_status_label(code: Any) -> str:
    if code is None:
        return "未知"
    try:
        return TASK_STATUS_LABEL.get(int(code), str(code))
    except (TypeError, ValueError):
        return str(code)


def _cron_run_status_text(status: Any) -> str:
    """定时任务启停状态（与网关文档一致的可识别值）。"""
    if status is None:
        return "未知"
    try:
        ic = int(status)
    except (TypeError, ValueError):
        return f"未知（原始值: {status}）"
    if ic in CRON_RUN_STATUS_TEXT:
        return CRON_RUN_STATUS_TEXT[ic]
    return f"未在网关文档中定义（status={ic}），请到作业平台控制台核对"


def enrich_cron_task_for_display(row: Dict[str, Any]) -> Dict[str, Any]:
    """为展示补充「启停状态」等字段，不修改原始 status 数值。"""
    out = dict(row)
    out["启停状态"] = _cron_run_status_text(row.get("status"))
    return out


def effective_lookback_days(requested: int) -> Tuple[int, bool]:
    """
    将用户请求的回溯天数限制在 [1, MAX_JOB_HISTORY_LOOKBACK_DAYS]。
    返回 (实际使用天数, 是否曾被截断)。
    """
    try:
        r = int(requested)
    except (TypeError, ValueError):
        r = MAX_JOB_HISTORY_LOOKBACK_DAYS
    if r < 1:
        r = 1
    capped = r > MAX_JOB_HISTORY_LOOKBACK_DAYS
    return (min(r, MAX_JOB_HISTORY_LOOKBACK_DAYS), capped)


def configure_stdio_utf8() -> None:
    """
    将标准输出/错误流设为 UTF-8，避免 Windows 控制台默认代码页导致中文乱码。
    macOS / Linux 在已为 UTF-8 时 reconfigure 为幂等；无 buffer 的流会跳过。
    """
    import io

    for name in ("stdout", "stderr"):
        stream = getattr(sys, name)
        try:
            if hasattr(stream, "reconfigure"):
                stream.reconfigure(encoding="utf-8", errors="replace")
                continue
        except (OSError, ValueError, TypeError):
            pass
        try:
            enc = (getattr(stream, "encoding", None) or "").lower()
            if enc in ("utf-8", "utf8"):
                continue
            buf = getattr(stream, "buffer", None)
            if buf is None:
                continue
            wrapped = io.TextIOWrapper(
                buf,
                encoding="utf-8",
                errors="replace",
                line_buffering=name == "stdout",
            )
            setattr(sys, name, wrapped)
        except (OSError, ValueError, TypeError, AttributeError):
            continue


def print_json(data: Any) -> None:
    print(json.dumps(data, ensure_ascii=False, indent=2))


def get_access_token(cli_token: Optional[str]) -> str:
    token = cli_token or os.environ.get("BK_JOB_ACCESS_TOKEN")
    if not token:
        print(
            "错误：未提供访问令牌。请使用 --access-token 或设置环境变量 BK_JOB_ACCESS_TOKEN。",
            file=sys.stderr,
        )
        sys.exit(1)
    return token


_SKILL_CONFIG_CACHE: Optional[Dict[str, str]] = None


def skill_root_dir() -> Path:
    """技能根目录 = scripts/ 的上一级（与 SKILL.md、config.yaml 同级）。"""
    return Path(__file__).resolve().parent.parent


def skill_config_path() -> Path:
    return skill_root_dir() / "config.yaml"


def _load_flat_yaml(path: Path) -> Dict[str, str]:
    """
    解析扁平 key: value 形式的 YAML（仅支持顶层字符串键值，无嵌套）。
    不依赖 PyYAML，满足本技能 config.yaml 的结构即可。
    """
    result: Dict[str, str] = {}
    for raw_line in path.read_text(encoding="utf-8").splitlines():
        line = raw_line.strip()
        if not line or line.startswith("#"):
            continue
        if ":" not in line:
            continue
        key, _, value = line.partition(":")
        key = key.strip()
        value = value.strip()
        if not key:
            continue
        if value and value[0] in "\"'" and value[-1] == value[0]:
            value = value[1:-1]
        result[key] = value
    return result


def load_skill_config() -> Dict[str, str]:
    """从技能包 config.yaml 加载 apigw_base_url / job_base_url。"""
    global _SKILL_CONFIG_CACHE
    if _SKILL_CONFIG_CACHE is not None:
        return _SKILL_CONFIG_CACHE

    config_path = skill_config_path()
    if not config_path.is_file():
        print(
            f"错误：未找到技能配置文件 {config_path}。\n"
            "请在技能根目录创建 config.yaml，并配置 apigw_base_url、job_base_url。",
            file=sys.stderr,
        )
        sys.exit(1)

    raw = _load_flat_yaml(config_path)
    apigw_base_url = str(raw.get("apigw_base_url") or "").strip().rstrip("/")
    job_base_url = str(raw.get("job_base_url") or "").strip().rstrip("/")
    _SKILL_CONFIG_CACHE = {
        "apigw_base_url": apigw_base_url,
        "job_base_url": job_base_url,
    }
    return _SKILL_CONFIG_CACHE


def get_base_url() -> str:
    base = load_skill_config()["apigw_base_url"]
    if not base:
        print(
            "错误：config.yaml 中未配置 apigw_base_url。\n"
            "请在技能根目录 config.yaml 中设置，例如：\n"
            "  apigw_base_url: https://bkapi.example.com/api/bk-job/prod",
            file=sys.stderr,
        )
        sys.exit(1)
    return base


def get_job_base_url() -> Optional[str]:
    """作业平台 Web 根 URL（非 APIGW），用于拼接任务详情页链接；未配置时返回 None。"""
    base = load_skill_config()["job_base_url"]
    return base or None


def build_job_instance_page_url(job_base_url: str, job_instance_id: Any) -> Optional[str]:
    """
    拼接作业实例详情页链接：{job_base_url}/api_execute/{job_instance_id}
    job_instance_id 无效时返回 None。
    """
    if job_instance_id is None:
        return None
    try:
        instance_id = int(job_instance_id)
    except (TypeError, ValueError):
        return None
    if instance_id <= 0:
        return None
    return f"{job_base_url.rstrip('/')}/api_execute/{instance_id}"


def enrich_plan_execute_result(data: Dict[str, Any], job_base_url: Optional[str]) -> Dict[str, Any]:
    """plan-execute 成功后补充任务跳转链接，便于智能体直接交付给用户。"""
    out = dict(data)
    job_instance_id = out.get("job_instance_id")
    if job_base_url:
        page_url = build_job_instance_page_url(job_base_url, job_instance_id)
        if page_url:
            out["job_instance_url"] = page_url
    elif job_instance_id is not None:
        out["_note_job_instance_url"] = (
            "config.yaml 中未配置 job_base_url，无法生成任务详情页链接。"
            "链接格式：{job_base_url}/api_execute/{job_instance_id}"
        )
    return out


def build_job_plan_page_url(job_base_url: str, job_plan_id: Any) -> Optional[str]:
    """
    拼接执行方案详情页链接：{job_base_url}/api_plan/{job_plan_id}
    job_plan_id 无效时返回 None。
    """
    if job_plan_id is None:
        return None
    try:
        plan_id = int(job_plan_id)
    except (TypeError, ValueError):
        return None
    if plan_id <= 0:
        return None
    return f"{job_base_url.rstrip('/')}/api_plan/{plan_id}"


def enrich_plan_create_result(data: Dict[str, Any], job_base_url: Optional[str]) -> Dict[str, Any]:
    """plan-create 成功后补充执行方案跳转链接，便于智能体直接交付给用户。"""
    out = dict(data)
    job_plan_id = out.get("job_plan_id")
    if job_base_url:
        page_url = build_job_plan_page_url(job_base_url, job_plan_id)
        if page_url:
            out["job_plan_url"] = page_url
    elif job_plan_id is not None:
        out["_note_job_plan_url"] = (
            "config.yaml 中未配置 job_base_url，无法生成执行方案详情页链接。"
            "链接格式：{job_base_url}/api_plan/{job_plan_id}"
        )
    return out


def business_memory_dir() -> Path:
    return skill_root_dir() / "memory" / "businesses"


def load_business_memory_record(scope_type: str, scope_id: str) -> Dict[str, Any]:
    """
    按 business-memory 约定解析 memory/businesses/ 下的 Markdown 文件。
    优先 {scope_type}_{scope_id}.md，其次 {scope_id}.md。
    """
    scope_type = str(scope_type)
    scope_id = str(scope_id)
    candidates = [
        f"{scope_type}_{scope_id}.md",
        f"{scope_id}.md",
    ]
    tried_paths = [f"memory/businesses/{name}" for name in candidates]
    base = business_memory_dir()
    for name in candidates:
        path = base / name
        if path.is_file():
            return {
                "loaded": True,
                "scope_type": scope_type,
                "scope_id": scope_id,
                "path": f"memory/businesses/{name}",
                "content": path.read_text(encoding="utf-8").strip(),
            }
    return {
        "loaded": False,
        "scope_type": scope_type,
        "scope_id": scope_id,
        "tried_paths": tried_paths,
    }


def attach_business_memory(data: Any, scope_type: str, scope_id: str) -> Any:
    """在 JSON 输出中附加 _business_memory，供智能体预填 scope/方案/参数。"""
    memory = load_business_memory_record(scope_type, scope_id)
    if isinstance(data, dict):
        out = dict(data)
        out["_business_memory"] = memory
        return out
    return {"data": data, "_business_memory": memory}


def print_scope_json(
    data: Any,
    scope_type: Optional[str],
    scope_id: Optional[str],
    *,
    attach_memory: bool = True,
) -> None:
    if attach_memory and scope_type and scope_id:
        data = attach_business_memory(data, scope_type, scope_id)
    print_json(data)


def auth_header(token: str) -> Dict[str, str]:
    return {
        "Accept": "application/json",
        "Content-Type": "application/json",
        "X-Bkapi-Authorization": json.dumps({"access_token": token}, separators=(",", ":")),
    }


def http_request(
    url: str,
    method: str = "GET",
    headers: Optional[Dict[str, str]] = None,
    body: Optional[Dict[str, Any]] = None,
) -> Tuple[int, Dict[str, Any]]:
    data_bytes = None
    if body is not None and method.upper() in ("POST", "PUT", "PATCH"):
        data_bytes = json.dumps(body).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=data_bytes,
        headers=headers or {},
        method=method.upper(),
    )
    try:
        with urllib.request.urlopen(req, timeout=120) as resp:
            raw = resp.read().decode("utf-8")
            status = resp.getcode()
            return status, json.loads(raw) if raw else {}
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8", errors="replace")
        try:
            parsed = json.loads(err_body) if err_body else {}
        except json.JSONDecodeError:
            parsed = {"_raw": err_body}
        print(f"HTTP {e.code}: {err_body}", file=sys.stderr)
        sys.exit(1)
    except urllib.error.URLError as e:
        print(f"请求失败: {e.reason}", file=sys.stderr)
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"JSON 解析失败: {e}", file=sys.stderr)
        sys.exit(1)


def v3_get(
    base_url: str,
    path: str,
    params: Dict[str, Any],
    token: str,
) -> Dict[str, Any]:
    q = {k: v for k, v in params.items() if v is not None}
    url = f"{base_url}{path}?{urllib.parse.urlencode(q)}"
    _, payload = http_request(url, "GET", auth_header(token))
    if payload.get("result") is not True or payload.get("code") != 0:
        print(f"API 错误: {json.dumps(payload, ensure_ascii=False)}", file=sys.stderr)
        sys.exit(1)
    return payload.get("data") or {}


def v4_get(
    base_url: str,
    path: str,
    params: Dict[str, Any],
    token: str,
) -> Dict[str, Any]:
    q = {k: v for k, v in params.items() if v is not None}
    url = f"{base_url}{path}?{urllib.parse.urlencode(q)}"
    status, payload = http_request(url, "GET", auth_header(token))
    if status != 200 or "error" in payload:
        print(f"API 错误: {json.dumps(payload, ensure_ascii=False)}", file=sys.stderr)
        sys.exit(1)
    return payload.get("data") or {}


def v3_post_json(
    base_url: str,
    path: str,
    body: Dict[str, Any],
    token: str,
) -> Dict[str, Any]:
    url = f"{base_url}{path}"
    _, payload = http_request(url, "POST", auth_header(token), body)
    if payload.get("result") is not True or payload.get("code") != 0:
        print(f"API 错误: {json.dumps(payload, ensure_ascii=False)}", file=sys.stderr)
        sys.exit(1)
    return payload.get("data") or {}


def v4_post_json(
    base_url: str,
    path: str,
    body: Dict[str, Any],
    token: str,
) -> Dict[str, Any]:
    url = f"{base_url}{path}"
    status, payload = http_request(url, "POST", auth_header(token), body)
    if status != 200 or "error" in payload:
        print(f"API 错误: {json.dumps(payload, ensure_ascii=False)}", file=sys.stderr)
        sys.exit(1)
    return payload.get("data") or {}


def scope_params(scope_type: str, scope_id: str) -> Dict[str, str]:
    return {"bk_scope_type": scope_type, "bk_scope_id": str(scope_id)}


def _attach_memory_enabled(args: argparse.Namespace) -> bool:
    return not getattr(args, "no_business_memory", False)


def _scope_print(args: argparse.Namespace, data: Any) -> None:
    print_scope_json(
        data,
        args.bk_scope_type,
        args.bk_scope_id,
        attach_memory=_attach_memory_enabled(args),
    )


def cmd_memory_load(args: argparse.Namespace) -> None:
    """加载 memory/businesses/ 下与资源范围对应的业务记忆。"""
    print_json(load_business_memory_record(args.bk_scope_type, args.bk_scope_id))


# -----------------------------------------------------------------------------
# 子命令实现
# -----------------------------------------------------------------------------

def cmd_cron_search(args: argparse.Namespace) -> None:
    token = get_access_token(args.access_token)
    base = get_base_url()
    p = {
        **scope_params(args.bk_scope_type, args.bk_scope_id),
        "name": args.keyword,
        "start": args.start,
        "length": args.length,
    }
    data = v3_get(base, "/api/v3/get_cron_list", p, token)
    rows = data.get("data")
    if isinstance(rows, list):
        data = {**data, "data": [enrich_cron_task_for_display(r) for r in rows]}
    _scope_print(args, data)


def cmd_cron_last_run(args: argparse.Namespace) -> None:
    """按关键词定位定时任务，查询最近一次「定时触发」执行记录的状态与各步骤日志。"""
    token = get_access_token(args.access_token)
    base = get_base_url()
    scope = scope_params(args.bk_scope_type, args.bk_scope_id)

    lookback_days, lookback_capped = effective_lookback_days(args.lookback_days)
    if lookback_capped:
        print(
            f"提示：执行历史查询回溯已限制为最多 {MAX_JOB_HISTORY_LOOKBACK_DAYS} 天"
            f"（请求 {args.lookback_days} 天已截断）。",
            file=sys.stderr,
        )

    cron_id = args.cron_id
    cron_row = None
    if cron_id is None:
        if not args.keyword:
            print("错误：请指定 --keyword 或 --cron-id。", file=sys.stderr)
            sys.exit(1)
        lst = v3_get(
            base,
            "/api/v3/get_cron_list",
            {**scope, "name": args.keyword, "start": 0, "length": max(args.length, LIST_PAGE_DEFAULT)},
            token,
        )
        rows = lst.get("data") or []
        if not rows:
            _scope_print(args, {"message": "未找到匹配的定时任务", "keyword": args.keyword})
            return
        if len(rows) > 1 and not args.pick_first:
            _scope_print(
                args,
                {
                    "message": "匹配到多条定时任务，请指定 --cron-id 或追加 --pick-first 使用第一条",
                    "matches": [enrich_cron_task_for_display(r) for r in rows],
                },
            )
            return
        cron_row = enrich_cron_task_for_display(rows[0])
        cron_id = cron_row["id"]
    else:
        detail = v3_get(
            base,
            "/api/v3/get_cron_list",
            {**scope, "id": cron_id, "start": 0, "length": 1},
            token,
        )
        rows = detail.get("data") or []
        if rows:
            cron_row = enrich_cron_task_for_display(rows[0])
        else:
            cron_row = enrich_cron_task_for_display(
                {
                    "id": cron_id,
                    "status": None,
                    "_note": "列表接口未返回该定时任务元数据，仍将按 cron_id 查询执行历史",
                }
            )

    now_ms = int(time.time() * 1000)
    start_ms = now_ms - int(lookback_days * 24 * 3600 * 1000)

    inst_data = v4_get(
        base,
        "/api/v4/get_job_instance_list",
        {
            **scope,
            "create_time_start": start_ms,
            "create_time_end": now_ms,
            "job_cron_id": cron_id,
            "launch_mode": 3,
            "offset": 0,
            "length": 1,
        },
        token,
    )
    instances = inst_data.get("job_instance_list") or []
    if not instances:
        _scope_print(
            args,
            {
                "cron": cron_row,
                "query": {
                    "lookback_days_requested": args.lookback_days,
                    "lookback_days_effective": lookback_days,
                    "lookback_days_max": MAX_JOB_HISTORY_LOOKBACK_DAYS,
                },
                "message": (
                    f"在时间范围内未找到该定时任务的执行记录"
                    f"（launch_mode=3 定时，实际查询近 {lookback_days} 天）"
                ),
            },
        )
        return

    inst = instances[0]
    job_instance_id = inst.get("job_instance_id") or inst.get("id")
    if job_instance_id is None:
        _scope_print(
            args,
            {
                "cron": cron_row,
                "error": "执行历史记录缺少 job_instance_id/id 字段，无法继续查询状态",
                "raw_instance": inst,
            },
        )
        return
    inst_status_label = _task_status_label(inst.get("task_status", inst.get("status")))

    status_data = v4_get(
        base,
        "/api/v4/get_job_instance_status",
        {
            **scope,
            "job_instance_id": job_instance_id,
            "return_execute_object_result": "true",
        },
        token,
    )

    step_logs: List[Dict[str, Any]] = []
    for step in status_data.get("step_instance_list") or []:
        sid = step.get("step_instance_id")
        entry: Dict[str, Any] = {
            "step_instance_id": sid,
            "name": step.get("name"),
            "type": step.get("type"),
            "status": step.get("status"),
            "status_text": _task_status_label(step.get("status")),
        }
        objects = step.get("step_execute_object_result_list") or []
        host_ids: List[int] = []
        ip_list: List[Dict[str, Any]] = []
        container_ids: List[int] = []
        for o in objects:
            eo = o.get("execute_object") or {}
            if eo.get("type") == 1:
                h = eo.get("host") or {}
                hid = h.get("bk_host_id")
                if hid is not None:
                    host_ids.append(int(hid))
                elif h.get("ip") is not None:
                    ip_list.append(
                        {
                            "bk_cloud_id": int(h.get("bk_cloud_id", 0)),
                            "ip": h["ip"],
                        }
                    )
            elif eo.get("type") == 2:
                c = eo.get("container") or {}
                if c.get("id") is not None:
                    container_ids.append(int(c["id"]))

        log_payload: Dict[str, Any] = {
            **scope,
            "job_instance_id": job_instance_id,
            "step_instance_id": sid,
        }
        if host_ids:
            log_payload["host_id_list"] = host_ids[:50]
        elif ip_list:
            log_payload["ip_list"] = ip_list[:50]
        elif container_ids:
            log_payload["container_id_list"] = container_ids[:50]

        if len(host_ids) > 50 or len(ip_list) > 50 or len(container_ids) > 50:
            entry["log_warning"] = "执行对象超过 50 个，本请求仅拉取前 50 个的日志"

        if not any(k in log_payload for k in ("host_id_list", "ip_list", "container_id_list")):
            entry["log_content"] = None
            entry["log_note"] = "步骤无主机/容器执行对象，跳过日志拉取（如纯人工确认步骤）"
        else:
            log_data = v4_post_json(
                base,
                "/api/v4/batch_get_job_instance_execute_object_log",
                log_payload,
                token,
            )
            entry["log"] = log_data

        step_logs.append(entry)

    _scope_print(
        args,
        {
            "cron": cron_row,
            "query": {
                "lookback_days_requested": args.lookback_days,
                "lookback_days_effective": lookback_days,
                "lookback_days_max": MAX_JOB_HISTORY_LOOKBACK_DAYS,
            },
            "latest_job_instance": {
                **inst,
                "task_status_text": inst_status_label,
            },
            "job_status": status_data,
            "step_logs": step_logs,
        },
    )


def cmd_template_search(args: argparse.Namespace) -> None:
    """按名称关键词查询作业模板列表（GET /api/v3/get_job_template_list）。

    用于创建执行方案前定位目标模板：根据 `name` 模糊匹配缩小范围，再用 template-detail 查看步骤列表与全局变量。
    """
    token = get_access_token(args.access_token)
    base = get_base_url()
    p = {
        **scope_params(args.bk_scope_type, args.bk_scope_id),
        "name": args.keyword,
        "creator": args.creator,
        "start": args.start,
        "length": args.length,
    }
    data = v3_get(base, "/api/v3/get_job_template_list", p, token)
    _scope_print(args, data)


def cmd_template_detail(args: argparse.Namespace) -> None:
    """获取作业模板详情（GET /api/v4/get_job_template_detail）。

    返回模板全部步骤（含未启用步骤）与全局变量列表，是 plan-create 前的关键步骤：
    - step_list[].id 用于 plan-create 的 --enable-steps；
    - global_var_list[] 用于组装 plan-create 的 --variables（按 name 匹配）。
    """
    token = get_access_token(args.access_token)
    base = get_base_url()
    p = {
        **scope_params(args.bk_scope_type, args.bk_scope_id),
        "id": args.job_template_id,
    }
    data = v4_get(base, "/api/v4/get_job_template_detail", p, token)
    _scope_print(args, data)


def cmd_plan_search(args: argparse.Namespace) -> None:
    token = get_access_token(args.access_token)
    base = get_base_url()
    p = {
        **scope_params(args.bk_scope_type, args.bk_scope_id),
        "name": args.keyword,
        "start": args.start,
        "length": args.length,
    }
    data = v3_get(base, "/api/v3/get_job_plan_list", p, token)
    _scope_print(args, data)


def cmd_plan_detail(args: argparse.Namespace) -> None:
    token = get_access_token(args.access_token)
    base = get_base_url()
    p = {
        **scope_params(args.bk_scope_type, args.bk_scope_id),
        "job_plan_id": args.job_plan_id,
    }
    data = v3_get(base, "/api/v3/get_job_plan_detail", p, token)
    _scope_print(args, data)


def _parse_json_arg(raw: Optional[str], label: str) -> Any:
    if not raw:
        return None
    try:
        return json.loads(raw)
    except json.JSONDecodeError as e:
        print(f"{label} JSON 无效: {e}", file=sys.stderr)
        sys.exit(1)


def _read_json_file(filepath: str, label: str) -> Any:
    """从文件读取 JSON（避免命令行转义问题）。"""
    try:
        with open(filepath, "r", encoding="utf-8") as f:
            return json.load(f)
    except FileNotFoundError:
        print(f"{label}: 文件不存在: {filepath}", file=sys.stderr)
        sys.exit(1)
    except json.JSONDecodeError as e:
        print(f"{label}: JSON 解析失败: {e}", file=sys.stderr)
        sys.exit(1)


def cmd_plan_create(args: argparse.Namespace) -> None:
    token = get_access_token(args.access_token)
    base = get_base_url()
    body: Dict[str, Any] = {
        **scope_params(args.bk_scope_type, args.bk_scope_id),
        "job_template_id": int(args.job_template_id),
        "name": args.name,
    }
    enable_steps = _parse_json_arg(args.enable_steps, "--enable-steps")
    if enable_steps is not None:
        if not isinstance(enable_steps, list):
            print("--enable-steps 须为 JSON 数组", file=sys.stderr)
            sys.exit(1)
        body["enable_steps"] = enable_steps
    variables = _parse_json_arg(args.variables, "--variables")
    if args.variables_file:
        variables = _read_json_file(args.variables_file, "--variables-file")
    if variables is not None:
        if not isinstance(variables, list):
            print("--variables/--variables-file 须为 JSON 数组", file=sys.stderr)
            sys.exit(1)
        body["variables"] = variables

    if args.dry_run:
        _scope_print(args, {"dry_run": True, "request_body": body})
        return

    data = v4_post_json(base, "/api/v4/create_job_plan", body, token)
    _scope_print(args, enrich_plan_create_result(data, get_job_base_url()))


def cmd_cron_save(args: argparse.Namespace) -> None:
    token = get_access_token(args.access_token)
    base = get_base_url()
    body: Dict[str, Any] = {
        **scope_params(args.bk_scope_type, args.bk_scope_id),
        "job_plan_id": int(args.job_plan_id),
    }
    if args.cron_id is not None:
        body["id"] = int(args.cron_id)
    if args.name:
        body["name"] = args.name
    if args.expression:
        body["expression"] = args.expression
    if args.execute_time is not None:
        body["execute_time"] = int(args.execute_time)
    if args.execute_time_zone:
        body["execute_time_zone"] = args.execute_time_zone
    global_vars = _parse_json_arg(args.global_vars, "--global-vars")
    if args.global_vars_file:
        global_vars = _read_json_file(args.global_vars_file, "--global-vars-file")
    if global_vars is not None:
        if not isinstance(global_vars, list):
            print("--global-vars/--global-vars-file 须为 JSON 数组", file=sys.stderr)
            sys.exit(1)
        body["global_var_list"] = global_vars

    if args.cron_id is None:
        if not args.name:
            print("错误：新建定时任务须指定 --name。", file=sys.stderr)
            sys.exit(1)
        if not args.expression and args.execute_time is None:
            print("错误：新建定时任务须指定 --expression 或 --execute-time（二选一）。", file=sys.stderr)
            sys.exit(1)

    if args.dry_run:
        _scope_print(args, {"dry_run": True, "request_body": body})
        return

    data = v3_post_json(base, "/api/v3/save_cron", body, token)
    out = dict(data)
    out["启停状态"] = _cron_run_status_text(data.get("status"))
    out["_note"] = (
        "新建定时任务默认处于暂停状态；是否启用须由用户明确确认后再调用 cron-update-status。"
    )
    _scope_print(args, out)


def cmd_cron_update_status(args: argparse.Namespace) -> None:
    token = get_access_token(args.access_token)
    base = get_base_url()
    status = int(args.status)
    if status not in (1, 2):
        print("错误：--status 仅支持 1（启动）或 2（暂停）。", file=sys.stderr)
        sys.exit(1)
    body: Dict[str, Any] = {
        **scope_params(args.bk_scope_type, args.bk_scope_id),
        "id": int(args.cron_id),
        "status": status,
    }

    if args.dry_run:
        _scope_print(args, {"dry_run": True, "request_body": body})
        return

    data = v3_post_json(base, "/api/v3/update_cron_status", body, token)
    _scope_print(
        args,
        {
            "cron_id": data,
            "status": status,
            "启停状态": _cron_run_status_text(status),
        },
    )


def cmd_plan_execute(args: argparse.Namespace) -> None:
    token = get_access_token(args.access_token)
    base = get_base_url()
    scope = scope_params(args.bk_scope_type, args.bk_scope_id)

    job_plan_id = args.job_plan_id
    if job_plan_id is None:
        if not args.keyword:
            print("错误：请指定 --job-plan-id 或 --keyword。", file=sys.stderr)
            sys.exit(1)
        lst = v3_get(
            base,
            "/api/v3/get_job_plan_list",
            {**scope, "name": args.keyword, "start": 0, "length": max(args.length, LIST_PAGE_DEFAULT)},
            token,
        )
        plans = lst.get("data") or []
        if not plans:
            _scope_print(args, {"message": "未找到匹配的执行方案", "keyword": args.keyword})
            return
        if len(plans) > 1 and not args.pick_first:
            _scope_print(
                args,
                {
                    "message": "匹配到多条执行方案，请指定 --job-plan-id 或追加 --pick-first",
                    "matches": plans,
                },
            )
            return
        job_plan_id = plans[0]["id"]

    body: Dict[str, Any] = {
        **scope,
        "job_plan_id": int(job_plan_id),
    }
    gv = None
    if args.global_vars_file:
        gv = _read_json_file(args.global_vars_file, "--global-vars-file")
    elif args.global_vars:
        try:
            gv = json.loads(args.global_vars)
            if not isinstance(gv, list):
                raise ValueError("global_var_list 须为 JSON 数组")
        except (json.JSONDecodeError, ValueError) as e:
            print(f"--global-vars JSON 无效: {e}", file=sys.stderr)
            sys.exit(1)
    if gv is not None:
        body["global_var_list"] = gv

    if args.dry_run:
        _scope_print(args, {"dry_run": True, "request_body": body})
        return

    data = v3_post_json(base, "/api/v3/execute_job_plan", body, token)
    _scope_print(args, enrich_plan_execute_result(data, get_job_base_url()))


def cmd_instance_status(args: argparse.Namespace) -> None:
    token = get_access_token(args.access_token)
    base = get_base_url()
    p = {
        **scope_params(args.bk_scope_type, args.bk_scope_id),
        "job_instance_id": args.job_instance_id,
        "return_execute_object_result": "true" if args.with_objects else "false",
    }
    data = v4_get(base, "/api/v4/get_job_instance_status", p, token)
    _scope_print(args, data)


def cmd_get_instance_log(args: argparse.Namespace) -> None:
    """获取作业实例步骤执行日志（POST /api/v4/batch_get_job_instance_execute_object_log）"""
    token = get_access_token(args.access_token)
    base = get_base_url()
    
    # 构建请求体
    body: Dict[str, Any] = {
        **scope_params(args.bk_scope_type, args.bk_scope_id),
        "job_instance_id": args.job_instance_id,
        "step_instance_id": args.step_instance_id,
    }
    
    # 添加执行对象过滤条件（优先级：host_id_list > ip_list > container_id_list）
    if args.host_id_list:
        try:
            body["host_id_list"] = [int(hid) for hid in args.host_id_list.split(",")]
        except ValueError:
            print("错误：--host-id-list 须为逗号分隔的整数列表", file=sys.stderr)
            sys.exit(1)
    elif args.ip_list:
        try:
            ip_items = []
            for item in args.ip_list.split(","):
                parts = item.split(":")
                if len(parts) != 2:
                    raise ValueError(f"IP 格式错误: {item}，须为 bk_cloud_id:ip")
                ip_items.append({"bk_cloud_id": int(parts[0]), "ip": parts[1]})
            body["ip_list"] = ip_items
        except ValueError as e:
            print(f"错误：--ip-list 格式无效: {e}", file=sys.stderr)
            sys.exit(1)
    elif args.container_id_list:
        try:
            body["container_id_list"] = [int(cid) for cid in args.container_id_list.split(",")]
        except ValueError:
            print("错误：--container-id-list 须为逗号分隔的整数列表", file=sys.stderr)
            sys.exit(1)
    
    # 调用 API
    data = v4_post_json(base, "/api/v4/batch_get_job_instance_execute_object_log", body, token)
    _scope_print(args, data)


def main() -> None:
    configure_stdio_utf8()

    parser = argparse.ArgumentParser(
        description="蓝鲸作业平台 API 网关客户端",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--access-token", help="蓝鲸用户态 access_token（或环境变量 BK_JOB_ACCESS_TOKEN）")
    parser.add_argument(
        "--no-business-memory",
        action="store_true",
        help="不在 JSON 输出中附加 _business_memory 字段",
    )

    sub = parser.add_subparsers(dest="command", required=True)

    p_cron = sub.add_parser("cron-search", help="按名称关键词查询定时任务列表（GET /api/v3/get_cron_list）")
    p_cron.add_argument("--bk-scope-type", default="biz", help="biz 或 biz_set，默认 biz")
    p_cron.add_argument("--bk-scope-id", required=True, help="资源范围 ID，如业务 ID")
    p_cron.add_argument("--keyword", help="定时任务名称模糊匹配")
    p_cron.add_argument("--start", type=int, default=0)
    p_cron.add_argument(
        "--length",
        type=int,
        default=LIST_PAGE_DEFAULT,
        help=f"本页返回条数，默认 {LIST_PAGE_DEFAULT}；无明确全量需求勿过大（接口最大约 1000）",
    )
    p_cron.set_defaults(func=cmd_cron_search)

    p_cl = sub.add_parser(
        "cron-last-run",
        help="关键词或 cron ID 定位定时任务，输出最近一次定时执行的状态与各步骤执行日志",
    )
    p_cl.add_argument("--bk-scope-type", default="biz")
    p_cl.add_argument("--bk-scope-id", required=True)
    p_cl.add_argument("--keyword", help="定时任务名称模糊匹配（与 --cron-id 二选一）")
    p_cl.add_argument("--cron-id", type=int, help="定时任务 ID")
    p_cl.add_argument(
        "--lookback-days",
        type=int,
        default=MAX_JOB_HISTORY_LOOKBACK_DAYS,
        help=f"在多少天内查找执行历史（默认 {MAX_JOB_HISTORY_LOOKBACK_DAYS}，硬上限 {MAX_JOB_HISTORY_LOOKBACK_DAYS} 天）",
    )
    p_cl.add_argument(
        "--length",
        type=int,
        default=LIST_PAGE_DEFAULT,
        help=f"关键词匹配时拉取定时任务列表的条数，默认 {LIST_PAGE_DEFAULT}",
    )
    p_cl.add_argument(
        "--pick-first",
        action="store_true",
        help="关键词匹配多条时自动取第一条（慎用）",
    )
    p_cl.set_defaults(func=cmd_cron_last_run)

    p_ts = sub.add_parser(
        "template-search",
        help="按名称关键词搜索作业模板（GET /api/v3/get_job_template_list）",
    )
    p_ts.add_argument("--bk-scope-type", default="biz")
    p_ts.add_argument("--bk-scope-id", required=True)
    p_ts.add_argument("--keyword", help="作业模板名称模糊匹配")
    p_ts.add_argument("--creator", help="按创建人账号过滤")
    p_ts.add_argument("--start", type=int, default=0)
    p_ts.add_argument(
        "--length",
        type=int,
        default=LIST_PAGE_DEFAULT,
        help=f"本页返回条数，默认 {LIST_PAGE_DEFAULT}；无明确全量需求勿过大（接口最大约 1000）",
    )
    p_ts.set_defaults(func=cmd_template_search)

    p_td = sub.add_parser(
        "template-detail",
        help="查询作业模板详情，返回步骤列表与全局变量（GET /api/v4/get_job_template_detail）",
    )
    p_td.add_argument("--bk-scope-type", default="biz")
    p_td.add_argument("--bk-scope-id", required=True)
    p_td.add_argument("--job-template-id", type=int, required=True, help="作业模板 ID")
    p_td.set_defaults(func=cmd_template_detail)

    p_ps = sub.add_parser("plan-search", help="按名称关键词搜索执行方案（GET /api/v3/get_job_plan_list）")
    p_ps.add_argument("--bk-scope-type", default="biz")
    p_ps.add_argument("--bk-scope-id", required=True)
    p_ps.add_argument("--keyword", help="执行方案名称模糊匹配")
    p_ps.add_argument("--start", type=int, default=0)
    p_ps.add_argument(
        "--length",
        type=int,
        default=LIST_PAGE_DEFAULT,
        help=f"本页返回条数，默认 {LIST_PAGE_DEFAULT}；无明确全量需求勿过大（接口最大约 1000）",
    )
    p_ps.set_defaults(func=cmd_plan_search)

    p_pd = sub.add_parser("plan-detail", help="查询执行方案详情（GET /api/v3/get_job_plan_detail）")
    p_pd.add_argument("--bk-scope-type", default="biz")
    p_pd.add_argument("--bk-scope-id", required=True)
    p_pd.add_argument("--job-plan-id", type=int, required=True)
    p_pd.set_defaults(func=cmd_plan_detail)

    p_pc = sub.add_parser("plan-create", help="基于作业模板创建执行方案（POST /api/v4/create_job_plan）")
    p_pc.add_argument("--bk-scope-type", default="biz")
    p_pc.add_argument("--bk-scope-id", required=True)
    p_pc.add_argument("--job-template-id", type=int, required=True, help="作业模板 ID")
    p_pc.add_argument("--name", required=True, help="执行方案名称")
    p_pc.add_argument(
        "--enable-steps",
        help='启用的模板步骤 ID JSON 数组，如 \'[101,102]\'',
    )
    p_pc.add_argument(
        "--variables",
        help='模板全局变量覆盖 JSON 数组（PowerShell 用户推荐用 --variables-file）',
    )
    p_pc.add_argument(
        "--variables-file",
        help="从文件读取 --variables 的 JSON（避免命令行转义问题），如 variables.json",
    )
    p_pc.add_argument(
        "--dry-run",
        action="store_true",
        help="只打印将提交的请求体，不调用创建接口",
    )
    p_pc.set_defaults(func=cmd_plan_create)

    p_pe = sub.add_parser("plan-execute", help="启动作业执行方案（POST /api/v3/execute_job_plan）")
    p_pe.add_argument("--bk-scope-type", default="biz")
    p_pe.add_argument("--bk-scope-id", required=True)
    p_pe.add_argument("--job-plan-id", type=int, help="执行方案 ID（可与 --keyword 二选一）")
    p_pe.add_argument("--keyword", help="先按名称搜索；仅唯一或配合 --pick-first")
    p_pe.add_argument(
        "--global-vars",
        help='全局变量 JSON 数组（PowerShell 用户推荐用 --global-vars-file）',
    )
    p_pe.add_argument(
        "--global-vars-file",
        help="从文件读取 --global-vars 的 JSON（避免命令行转义问题），如 globals.json",
    )
    p_pe.add_argument(
        "--length",
        type=int,
        default=LIST_PAGE_DEFAULT,
        help=f"按关键词搜方案时的列表条数，默认 {LIST_PAGE_DEFAULT}",
    )
    p_pe.add_argument("--pick-first", action="store_true")
    p_pe.add_argument(
        "--dry-run",
        action="store_true",
        help="只打印将提交的请求体，不调用执行接口",
    )
    p_pe.set_defaults(func=cmd_plan_execute)

    p_cs = sub.add_parser("cron-save", help="新建或保存定时任务（POST /api/v3/save_cron）")
    p_cs.add_argument("--bk-scope-type", default="biz")
    p_cs.add_argument("--bk-scope-id", required=True)
    p_cs.add_argument("--job-plan-id", type=int, required=True, help="绑定的执行方案 ID")
    p_cs.add_argument("--cron-id", type=int, help="定时任务 ID，更新时必传")
    p_cs.add_argument("--name", help="定时任务名称，新建时必填")
    p_cs.add_argument("--expression", help="Crontab 表达式，新建时与 --execute-time 二选一")
    p_cs.add_argument(
        "--execute-time",
        type=int,
        help="单次执行时间，Unix 时间戳（秒）；新建时与 --expression 二选一",
    )
    p_cs.add_argument("--execute-time-zone", help="IANA 时区，如 Asia/Shanghai")
    p_cs.add_argument(
        "--global-vars",
        help='全局变量 JSON 数组（PowerShell 用户推荐用 --global-vars-file）',
    )
    p_cs.add_argument(
        "--global-vars-file",
        help="从文件读取 --global-vars 的 JSON（避免命令行转义问题），如 globals.json",
    )
    p_cs.add_argument(
        "--dry-run",
        action="store_true",
        help="只打印将提交的请求体，不调用保存接口",
    )
    p_cs.set_defaults(func=cmd_cron_save)

    p_cus = sub.add_parser(
        "cron-update-status",
        help="更新定时任务启停状态（POST /api/v3/update_cron_status）",
    )
    p_cus.add_argument("--bk-scope-type", default="biz")
    p_cus.add_argument("--bk-scope-id", required=True)
    p_cus.add_argument("--cron-id", type=int, required=True, help="定时任务 ID")
    p_cus.add_argument(
        "--status",
        type=int,
        required=True,
        help="1 启动、2 暂停",
    )
    p_cus.add_argument(
        "--dry-run",
        action="store_true",
        help="只打印将提交的请求体，不调用更新接口",
    )
    p_cus.set_defaults(func=cmd_cron_update_status)

    p_st = sub.add_parser("instance-status", help="查询作业实例状态（GET /api/v4/get_job_instance_status）")
    p_st.add_argument("--bk-scope-type", default="biz")
    p_st.add_argument("--bk-scope-id", required=True)
    p_st.add_argument("--job-instance-id", type=int, required=True)
    p_st.add_argument(
        "--with-objects",
        action="store_true",
        help="返回每主机/容器步骤结果（return_execute_object_result=true）",
    )
    p_st.set_defaults(func=cmd_instance_status)

    p_gl = sub.add_parser("get-instance-log", help="获取作业实例步骤执行日志（POST /api/v4/batch_get_job_instance_execute_object_log）")
    p_gl.add_argument("--bk-scope-type", default="biz")
    p_gl.add_argument("--bk-scope-id", required=True)
    p_gl.add_argument("--job-instance-id", type=int, required=True, help="作业实例 ID")
    p_gl.add_argument("--step-instance-id", type=int, required=True, help="步骤实例 ID")
    p_gl.add_argument(
        "--host-id-list",
        help="主机 ID 列表，逗号分隔（优先级最高，最多 50 个）",
    )
    p_gl.add_argument(
        "--ip-list",
        help="主机 IP 列表，逗号分隔，格式为 bk_cloud_id:ip（最多 50 个）",
    )
    p_gl.add_argument(
        "--container-id-list",
        help="容器 ID 列表，逗号分隔（最多 50 个）",
    )
    p_gl.set_defaults(func=cmd_get_instance_log)

    p_ml = sub.add_parser(
        "memory-load",
        help="加载 memory/businesses/ 下与资源范围对应的业务记忆 Markdown",
    )
    p_ml.add_argument("--bk-scope-type", default="biz")
    p_ml.add_argument("--bk-scope-id", required=True)
    p_ml.set_defaults(func=cmd_memory_load)

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
