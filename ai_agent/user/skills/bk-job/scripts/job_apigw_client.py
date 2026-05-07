#!/usr/bin/env python3
"""
蓝鲸作业平台 API 网关 Python 客户端

通过蓝鲸 API 网关调用作业平台开放接口，支持：
- 定时任务检索与最近一次定时执行的状态、日志聚合查询
- 执行方案检索、详情与启动执行

依赖 Python 标准库；鉴权使用用户态 access_token。

用法:
    python job_apigw_client.py <command> [options]

认证:
    --access-token 或环境变量 BK_JOB_ACCESS_TOKEN
网关地址:
    --base-url 或环境变量 BK_JOB_APIGW_BASE_URL（需含协议与阶段路径，例如 https://xxx/stage/bk-job）

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


def get_base_url(cli_base: Optional[str]) -> str:
    base = (cli_base or os.environ.get("BK_JOB_APIGW_BASE_URL") or "").strip().rstrip("/")
    if not base:
        print(
            "错误：未配置 API 网关根路径。请使用 --base-url 或设置环境变量 BK_JOB_APIGW_BASE_URL。\n"
            "示例：https://{你的APIGW域名}/prod/bk-job",
            file=sys.stderr,
        )
        sys.exit(1)
    return base


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


# -----------------------------------------------------------------------------
# 子命令实现
# -----------------------------------------------------------------------------

def cmd_cron_search(args: argparse.Namespace) -> None:
    token = get_access_token(args.access_token)
    base = get_base_url(args.base_url)
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
    print_json(data)


def cmd_cron_last_run(args: argparse.Namespace) -> None:
    """按关键词定位定时任务，查询最近一次「定时触发」执行记录的状态与各步骤日志。"""
    token = get_access_token(args.access_token)
    base = get_base_url(args.base_url)
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
            print_json({"message": "未找到匹配的定时任务", "keyword": args.keyword})
            return
        if len(rows) > 1 and not args.pick_first:
            print_json(
                {
                    "message": "匹配到多条定时任务，请指定 --cron-id 或追加 --pick-first 使用第一条",
                    "matches": [enrich_cron_task_for_display(r) for r in rows],
                }
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
        print_json(
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
            }
        )
        return

    inst = instances[0]
    job_instance_id = inst.get("job_instance_id") or inst.get("id")
    if job_instance_id is None:
        print_json(
            {
                "cron": cron_row,
                "error": "执行历史记录缺少 job_instance_id/id 字段，无法继续查询状态",
                "raw_instance": inst,
            }
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

    print_json(
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
        }
    )


def cmd_plan_search(args: argparse.Namespace) -> None:
    token = get_access_token(args.access_token)
    base = get_base_url(args.base_url)
    p = {
        **scope_params(args.bk_scope_type, args.bk_scope_id),
        "name": args.keyword,
        "start": args.start,
        "length": args.length,
    }
    data = v3_get(base, "/api/v3/get_job_plan_list", p, token)
    print_json(data)


def cmd_plan_detail(args: argparse.Namespace) -> None:
    token = get_access_token(args.access_token)
    base = get_base_url(args.base_url)
    p = {
        **scope_params(args.bk_scope_type, args.bk_scope_id),
        "job_plan_id": args.job_plan_id,
    }
    data = v3_get(base, "/api/v3/get_job_plan_detail", p, token)
    print_json(data)


def cmd_plan_execute(args: argparse.Namespace) -> None:
    token = get_access_token(args.access_token)
    base = get_base_url(args.base_url)
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
            print_json({"message": "未找到匹配的执行方案", "keyword": args.keyword})
            return
        if len(plans) > 1 and not args.pick_first:
            print_json(
                {
                    "message": "匹配到多条执行方案，请指定 --job-plan-id 或追加 --pick-first",
                    "matches": plans,
                }
            )
            return
        job_plan_id = plans[0]["id"]

    body: Dict[str, Any] = {
        **scope,
        "job_plan_id": int(job_plan_id),
    }
    if args.global_vars:
        try:
            gv = json.loads(args.global_vars)
            if not isinstance(gv, list):
                raise ValueError("global_var_list 须为 JSON 数组")
            body["global_var_list"] = gv
        except (json.JSONDecodeError, ValueError) as e:
            print(f"--global-vars JSON 无效: {e}", file=sys.stderr)
            sys.exit(1)

    if args.dry_run:
        print_json({"dry_run": True, "request_body": body})
        return

    data = v3_post_json(base, "/api/v3/execute_job_plan", body, token)
    print_json(data)


def cmd_instance_status(args: argparse.Namespace) -> None:
    token = get_access_token(args.access_token)
    base = get_base_url(args.base_url)
    p = {
        **scope_params(args.bk_scope_type, args.bk_scope_id),
        "job_instance_id": args.job_instance_id,
        "return_execute_object_result": "true" if args.with_objects else "false",
    }
    data = v4_get(base, "/api/v4/get_job_instance_status", p, token)
    print_json(data)


def main() -> None:
    configure_stdio_utf8()

    parser = argparse.ArgumentParser(
        description="蓝鲸作业平台 API 网关客户端",
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )
    parser.add_argument("--access-token", help="蓝鲸用户态 access_token（或环境变量 BK_JOB_ACCESS_TOKEN）")
    parser.add_argument(
        "--base-url",
        help="API 网关根 URL，需包含 bk-job 服务前缀（或环境变量 BK_JOB_APIGW_BASE_URL）",
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

    p_pe = sub.add_parser("plan-execute", help="启动作业执行方案（POST /api/v3/execute_job_plan）")
    p_pe.add_argument("--bk-scope-type", default="biz")
    p_pe.add_argument("--bk-scope-id", required=True)
    p_pe.add_argument("--job-plan-id", type=int, help="执行方案 ID（可与 --keyword 二选一）")
    p_pe.add_argument("--keyword", help="先按名称搜索；仅唯一或配合 --pick-first")
    p_pe.add_argument(
        "--global-vars",
        help='全局变量 JSON 数组，如 \'[{"name":"x","value":"y"}]\'',
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

    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    main()
