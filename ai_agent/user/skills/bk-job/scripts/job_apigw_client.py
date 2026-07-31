#!/usr/bin/env python3
"""
蓝鲸作业平台 API 网关 Python 客户端

通过蓝鲸 API 网关调用作业平台开放接口，支持：
- 定时任务检索与最近一次定时执行的状态、日志聚合查询
- 定时任务新建/保存与启停状态更新
- 作业模板检索与详情查询（用于支撑「创建执行方案」前的步骤与变量探查）
- 执行方案检索、详情、创建与启动执行
- 作业实例状态查询与步骤执行日志获取
- 业务主机拓扑树查询与资源范围下主机搜索（执行类操作填写主机时定位目标主机）
- 业务下执行账号列表查询（执行类操作填写账号时引导选择可用账号）
- 快速分发文件到目标机器（服务器文件/本地文件；本地文件先生成上传URL并上传）

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
import base64
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

# 快速执行脚本超时时间（秒）取值范围，与后端 JobConstants 一致（默认 7200）
FAST_EXEC_TIMEOUT_MIN = 1
FAST_EXEC_TIMEOUT_MAX = 259200

# 快速执行脚本 script_language 取值：与后端 ScriptTypeEnum（fast_execute_script 文档 1-5）一致
# 注意：SQL(6) 属于 fast_execute_sql 接口，本子命令不涉及
SCRIPT_LANGUAGE_NAME_TO_CODE = {
    "shell": 1,
    "bat": 2,
    "perl": 3,
    "python": 4,
    "powershell": 5,
}
SCRIPT_LANGUAGE_CODE_TO_NAME = {v: k for k, v in SCRIPT_LANGUAGE_NAME_TO_CODE.items()}


def resolve_script_language(raw: Optional[str]) -> Optional[int]:
    """将 --script-language 的取值（名称或数字）解析为后端脚本语言编码 1-5。

    未提供时返回 None（由脚本侧决定是否补默认值）。
    """
    if raw is None:
        return None
    value = str(raw).strip().lower()
    if value in SCRIPT_LANGUAGE_NAME_TO_CODE:
        return SCRIPT_LANGUAGE_NAME_TO_CODE[value]
    try:
        code = int(value)
    except (TypeError, ValueError):
        print(
            f"错误：--script-language 取值无效: {raw}。"
            f"支持名称 {sorted(SCRIPT_LANGUAGE_NAME_TO_CODE)} 或对应编码 1-5。",
            file=sys.stderr,
        )
        sys.exit(1)
    if code not in SCRIPT_LANGUAGE_CODE_TO_NAME:
        print(
            f"错误：--script-language 编码无效: {code}。"
            "快速执行脚本仅支持 1-shell、2-bat、3-perl、4-python、5-powershell。",
            file=sys.stderr,
        )
        sys.exit(1)
    return code


def _b64_encode_text(text: str) -> str:
    """将明文（UTF-8）Base64 编码，供 script_content / script_param 使用。"""
    return base64.b64encode(text.encode("utf-8")).decode("ascii")

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


LAUNCH_MODE_LABEL = {
    1: "页面执行",
    2: "API调用",
    3: "定时执行",
}

TASK_TYPE_LABEL = {
    0: "作业执行",
    1: "脚本执行",
    2: "文件分发",
}


def _task_status_label(code: Any) -> str:
    if code is None:
        return "未知"
    try:
        return TASK_STATUS_LABEL.get(int(code), str(code))
    except (TypeError, ValueError):
        return str(code)


def _enum_label(code: Any, mapping: Dict[int, str]) -> str:
    if code is None:
        return "未知"
    try:
        return mapping.get(int(code), str(code))
    except (TypeError, ValueError):
        return str(code)


def _ms_to_text(ms: Any) -> Optional[str]:
    """毫秒时间戳转本地可读时间；非法值原样返回。"""
    if ms in (None, 0, ""):
        return None
    try:
        return time.strftime("%Y-%m-%d %H:%M:%S", time.localtime(int(ms) / 1000))
    except (TypeError, ValueError, OSError):
        return str(ms)


def enrich_job_instance_for_display(row: Dict[str, Any]) -> Dict[str, Any]:
    """为执行历史记录补充可读的状态、类型、执行方式与时间，不修改原始数值字段。

    接口对状态/类型字段存在 task_status/status、task_type/type 两种命名，均做兼容。
    """
    out = dict(row)
    out["任务状态"] = _task_status_label(row.get("task_status", row.get("status")))
    out["任务类型"] = _enum_label(row.get("task_type", row.get("type")), TASK_TYPE_LABEL)
    out["执行方式"] = _enum_label(row.get("launch_mode"), LAUNCH_MODE_LABEL)
    for src, label in (("create_time", "创建时间"), ("start_time", "启动时间"), ("end_time", "结束时间")):
        text = _ms_to_text(row.get(src))
        if text:
            out[label] = text
    total = row.get("total_time")
    if isinstance(total, (int, float)) and total > 0:
        out["耗时秒"] = round(total / 1000, 1)
    return out


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


def http_upload_file(url: str, filepath: str, timeout: int = 600) -> Tuple[int, str]:
    """将本地文件以 HTTP PUT 上传到指定 URL（用于本地文件分发时上传到制品库临时地址）。

    等价于：curl -X PUT -H "Content-Type: application/octet-stream"
             --data-binary @<file> "<upload_url>"
    即以 PUT 方法将文件原始字节作为请求体上传；URL 自带鉴权 token
    （来自 generate_local_file_upload_url 的 upload_url），不再附加 APIGW 鉴权头。
    返回 (HTTP 状态码, 响应文本)。
    """
    path_obj = Path(filepath)
    if not path_obj.is_file():
        print(f"错误：待上传文件不存在: {filepath}", file=sys.stderr)
        sys.exit(1)
    data_bytes = path_obj.read_bytes()
    req = urllib.request.Request(url, data=data_bytes, method="PUT")
    req.add_header("Content-Type", "application/octet-stream")
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            raw = resp.read().decode("utf-8", errors="replace")
            return resp.getcode(), raw
    except urllib.error.HTTPError as e:
        err_body = e.read().decode("utf-8", errors="replace")
        print(f"上传失败 HTTP {e.code}: {err_body}", file=sys.stderr)
        sys.exit(1)
    except urllib.error.URLError as e:
        print(f"上传请求失败: {e.reason}", file=sys.stderr)
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
    """从文件读取 JSON（避免命令行转义问题）。

    使用 utf-8-sig 解码以兼容 Windows/PowerShell 常见的 UTF-8 BOM。
    """
    try:
        with open(filepath, "r", encoding="utf-8-sig") as f:
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


def cmd_instance_list(args: argparse.Namespace) -> None:
    """查询任务执行历史（GET /api/v4/get_job_instance_list）。

    按时间窗口列出资源范围下的作业实例，可按任务名、执行人、执行方式、任务类型、状态、
    目标 IP、定时任务 ID 过滤。时间窗口必填，脚本按 --lookback-days 自动换算为毫秒时间戳，
    回溯天数硬上限 31 天，与 cron-last-run 一致。
    """
    token = get_access_token(args.access_token)
    base = get_base_url()

    lookback_days, lookback_capped = effective_lookback_days(args.lookback_days)
    if lookback_capped:
        print(
            f"提示：执行历史查询回溯已限制为最多 {MAX_JOB_HISTORY_LOOKBACK_DAYS} 天"
            f"（请求 {args.lookback_days} 天已截断）。",
            file=sys.stderr,
        )

    now_ms = int(time.time() * 1000)
    start_ms = now_ms - int(lookback_days * 24 * 3600 * 1000)

    p: Dict[str, Any] = {
        **scope_params(args.bk_scope_type, args.bk_scope_id),
        "create_time_start": start_ms,
        "create_time_end": now_ms,
        "offset": args.offset,
        "length": args.length,
    }
    # 传入 job_instance_id 时接口会忽略其余过滤条件
    for key, value in (
        ("job_instance_id", args.job_instance_id),
        ("job_cron_id", args.cron_id),
        ("operator", args.operator),
        ("name", args.keyword),
        ("launch_mode", args.launch_mode),
        ("type", args.type),
        ("status", args.status),
        ("ip", args.ip),
    ):
        if value is not None:
            p[key] = value

    data = v4_get(base, "/api/v4/get_job_instance_list", p, token)
    rows = data.get("job_instance_list") or []
    _scope_print(
        args,
        {
            "query": {
                "lookback_days_requested": args.lookback_days,
                "lookback_days_effective": lookback_days,
                "lookback_days_max": MAX_JOB_HISTORY_LOOKBACK_DAYS,
                "create_time_start": start_ms,
                "create_time_end": now_ms,
                "offset": args.offset,
                "length": args.length,
            },
            "returned": len(rows),
            "job_instance_list": [enrich_job_instance_for_display(r) for r in rows],
            "_note": (
                "本页按创建时间从新到老返回；接口不返回总数，若本页条数等于 length，"
                "可能还有更早记录，可增大 --offset 继续翻页或缩小时间窗口。"
            ),
        },
    )


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


def _build_fast_exec_target(args: argparse.Namespace) -> Dict[str, Any]:
    """根据 CLI 参数组装 execute_target（快速执行脚本）。

    支持三种目标来源（可组合，但至少提供一种）：
    - --host-id-list：逗号分隔的 bk_host_id → host_list[{bk_host_id}]
    - --ip-list：逗号分隔的 bk_cloud_id:ip → host_list[{bk_cloud_id, ip}]
    - --execute-target-file：直接读取完整 execute_target JSON（支持动态分组/拓扑/容器等复杂结构）
    """
    if args.execute_target_file:
        target = _read_json_file(args.execute_target_file, "--execute-target-file")
        if not isinstance(target, dict):
            print("--execute-target-file 内容须为 JSON 对象（execute_target 结构）", file=sys.stderr)
            sys.exit(1)
        return target

    target: Dict[str, Any] = {}
    host_list: List[Dict[str, Any]] = []
    if args.host_id_list:
        try:
            host_list.extend({"bk_host_id": int(h)} for h in args.host_id_list.split(",") if h.strip())
        except ValueError:
            print("错误：--host-id-list 须为逗号分隔的整数列表", file=sys.stderr)
            sys.exit(1)
    if args.ip_list:
        for item in args.ip_list.split(","):
            item = item.strip()
            if not item:
                continue
            parts = item.split(":")
            if len(parts) != 2:
                print(f"错误：--ip-list 格式无效: {item}，须为 bk_cloud_id:ip", file=sys.stderr)
                sys.exit(1)
            try:
                host_list.append({"bk_cloud_id": int(parts[0]), "ip": parts[1]})
            except ValueError:
                print(f"错误：--ip-list 中 bk_cloud_id 须为整数: {item}", file=sys.stderr)
                sys.exit(1)
    if host_list:
        target["host_list"] = host_list

    if not target:
        print(
            "错误：未指定执行目标。请提供 --host-id-list、--ip-list 或 --execute-target-file 之一"
            "（动态分组/拓扑节点/容器等复杂目标请用 --execute-target-file）。",
            file=sys.stderr,
        )
        sys.exit(1)
    return target


def cmd_fast_execute_script(args: argparse.Namespace) -> None:
    """快速执行脚本（POST /api/v4/fast_execute_script）。

    变更类/生产执行操作：真实调用前须遵守确认门禁（G1–G4），支持 --dry-run 预览请求体。
    """
    token = get_access_token(args.access_token)
    base = get_base_url()

    body: Dict[str, Any] = {
        **scope_params(args.bk_scope_type, args.bk_scope_id),
    }

    # 脚本来源优先级：script_version_id > script_id > script_content
    has_ref = args.script_id is not None or args.script_version_id is not None
    script_content: Optional[str] = None
    if args.script_content_file:
        script_content = Path(args.script_content_file).read_text(encoding="utf-8")
    elif args.script_content is not None:
        script_content = args.script_content
    if not has_ref and (script_content is None or script_content == ""):
        print(
            "错误：请提供脚本内容（--script-content / --script-content-file）"
            "或引用已有脚本（--script-id / --script-version-id）。",
            file=sys.stderr,
        )
        sys.exit(1)

    # 账号：account_id 与 account_alias 至少一个（同时存在时 account_id 优先）
    if args.account_id is None and not args.account_alias:
        print("错误：请提供 --account-alias 或 --account-id（执行账号）。", file=sys.stderr)
        sys.exit(1)
    if args.account_id is not None:
        body["account_id"] = int(args.account_id)
    if args.account_alias:
        body["account_alias"] = args.account_alias

    if args.script_version_id is not None:
        body["script_version_id"] = int(args.script_version_id)
    if args.script_id is not None:
        body["script_id"] = args.script_id
    if script_content:
        body["script_content"] = _b64_encode_text(script_content)
        lang = resolve_script_language(args.script_language)
        if lang is None:
            lang = SCRIPT_LANGUAGE_NAME_TO_CODE["shell"]
        body["script_language"] = lang
    elif args.script_language is not None:
        # 引用脚本时脚本语言以脚本自身为准，忽略该参数
        pass

    if args.script_param is not None:
        body["script_param"] = _b64_encode_text(args.script_param)
    if args.param_sensitive:
        body["param_sensitive"] = True
    if args.name:
        body["task_name"] = args.name
    if args.windows_interpreter:
        body["windows_interpreter"] = args.windows_interpreter
    if args.timeout is not None:
        timeout = int(args.timeout)
        if timeout < FAST_EXEC_TIMEOUT_MIN or timeout > FAST_EXEC_TIMEOUT_MAX:
            print(
                f"错误：--timeout 取值须在 {FAST_EXEC_TIMEOUT_MIN}-{FAST_EXEC_TIMEOUT_MAX} 秒之间。",
                file=sys.stderr,
            )
            sys.exit(1)
        body["timeout"] = timeout
    if args.callback_url:
        body["callback_url"] = args.callback_url
    if args.no_start_task:
        body["start_task"] = False

    body["execute_target"] = _build_fast_exec_target(args)

    if args.dry_run:
        _scope_print(args, {"dry_run": True, "request_body": body})
        return

    data = v4_post_json(base, "/api/v4/fast_execute_script", body, token)
    _scope_print(args, enrich_plan_execute_result(data, get_job_base_url()))


def cmd_list_authorized_scopes(args: argparse.Namespace) -> None:
    """查询当前用户有权限的资源范围列表（GET /api/v4/get_user_authorized_scopes）。

    用于首次使用、尚无 bk_scope 上下文时列出可选业务(biz)/业务集(biz_set)并引导用户选择。
    """
    token = get_access_token(args.access_token)
    base = get_base_url()
    p = {
        "offset": args.offset,
        "length": args.length,
    }
    data = v4_get(base, "/api/v4/get_user_authorized_scopes", p, token)
    print_json(data)


def _split_comma_keywords(raw: Optional[str]) -> Optional[List[str]]:
    """将逗号分隔的关键字字符串拆为列表（去空白与空项）；无有效项返回 None。"""
    if not raw:
        return None
    items = [seg.strip() for seg in raw.split(",")]
    items = [seg for seg in items if seg]
    return items or None


def cmd_host_topo_tree(args: argparse.Namespace) -> None:
    """查询业务主机拓扑树（POST /api/v4/get_biz_host_topo_tree）。

    仅支持业务(biz)：默认全部展开，返回 业务→集群(set)→模块(module) 的层级与各节点主机数量，
    供用户按拓扑节点圈定执行目标（配合 host-search 的 --topo-nodes 精确取主机）。
    业务集(biz_set)/租户集(tenant_set) 不受支持，接口会返回参数错误。
    """
    token = get_access_token(args.access_token)
    base = get_base_url()
    if args.bk_scope_type != "biz":
        print(
            "错误：host-topo-tree 仅支持业务(biz)，业务集/租户集请改用 host-search"
            "（可传关键字过滤，无拓扑树）。",
            file=sys.stderr,
        )
        sys.exit(1)
    body = {**scope_params(args.bk_scope_type, args.bk_scope_id)}
    data = v4_post_json(base, "/api/v4/get_biz_host_topo_tree", body, token)
    _scope_print(args, data)


def cmd_host_search(args: argparse.Namespace) -> None:
    """按条件搜索资源范围下的主机（POST /api/v4/search_scope_host）。

    用于执行类操作填写主机时，帮用户根据 IP/主机名/操作系统/Agent 状态/拓扑节点定位目标主机，
    返回 bk_host_id、ip、bk_cloud_id 等，可直接用于 fast-execute-script / plan-execute 的主机参数。
    支持业务(biz)、业务集(biz_set)、租户集(tenant_set)；拓扑节点(topo_node_list)仅业务(biz)生效。
    """
    token = get_access_token(args.access_token)
    base = get_base_url()
    body: Dict[str, Any] = {
        **scope_params(args.bk_scope_type, args.bk_scope_id),
        "offset": args.offset,
        "length": args.length,
    }

    ipv4 = _split_comma_keywords(args.ipv4)
    if ipv4:
        body["ipv4_key_list"] = ipv4
    ipv6 = _split_comma_keywords(args.ipv6)
    if ipv6:
        body["ipv6_key_list"] = ipv6
    host_name = _split_comma_keywords(args.host_name)
    if host_name:
        body["host_name_key_list"] = host_name
    os_name = _split_comma_keywords(args.os_name)
    if os_name:
        body["os_name_key_list"] = os_name

    if args.alive is not None:
        if int(args.alive) not in (0, 1):
            print("错误：--alive 仅支持 0（异常）或 1（正常）。", file=sys.stderr)
            sys.exit(1)
        body["alive"] = int(args.alive)

    topo_nodes = None
    if args.topo_nodes_file:
        topo_nodes = _read_json_file(args.topo_nodes_file, "--topo-nodes-file")
    elif args.topo_nodes:
        topo_nodes = _parse_json_arg(args.topo_nodes, "--topo-nodes")
    if topo_nodes is not None:
        if not isinstance(topo_nodes, list):
            print("--topo-nodes/--topo-nodes-file 须为 JSON 数组，元素含 object_id 与 instance_id", file=sys.stderr)
            sys.exit(1)
        if topo_nodes and args.bk_scope_type != "biz":
            print(
                "提示：拓扑节点仅在业务(biz)下生效，业务集/租户集会被忽略并按整个资源范围搜索。",
                file=sys.stderr,
            )
        body["topo_node_list"] = topo_nodes

    data = v4_post_json(base, "/api/v4/search_scope_host", body, token)
    _scope_print(args, data)


# 账号用途（category）：与后端 AccountCategoryEnum 一致
ACCOUNT_CATEGORY_NAME_TO_CODE = {
    "system": 1,
    "db": 2,
}
ACCOUNT_CATEGORY_CODE_TO_NAME = {v: k for k, v in ACCOUNT_CATEGORY_NAME_TO_CODE.items()}


def resolve_account_category(raw: Optional[str]) -> Optional[int]:
    """将 --category 的取值（名称 system/db 或编码 1/2）解析为后端账号用途编码。"""
    if raw is None:
        return None
    value = str(raw).strip().lower()
    if value in ACCOUNT_CATEGORY_NAME_TO_CODE:
        return ACCOUNT_CATEGORY_NAME_TO_CODE[value]
    try:
        code = int(value)
    except (TypeError, ValueError):
        print(
            f"错误：--category 取值无效: {raw}。支持名称 system/db 或编码 1（系统账号）、2（DB账号）。",
            file=sys.stderr,
        )
        sys.exit(1)
    if code not in ACCOUNT_CATEGORY_CODE_TO_NAME:
        print("错误：--category 编码无效，仅支持 1（系统账号）或 2（DB账号）。", file=sys.stderr)
        sys.exit(1)
    return code


def cmd_account_list(args: argparse.Namespace) -> None:
    """查询业务下的执行账号列表（GET /api/v3/get_account_list）。

    用于执行类操作（如 fast-execute-script）需要用户填写执行账号、但用户未指定时，
    列出该资源范围下可用账号供选择：返回账号 id（用于 --account-id）与 alias（用于 --account-alias）。
    """
    token = get_access_token(args.access_token)
    base = get_base_url()
    p = {
        **scope_params(args.bk_scope_type, args.bk_scope_id),
        "category": resolve_account_category(args.category),
        "account": args.account,
        "alias": args.alias,
        "start": args.start,
        "length": args.length,
    }
    data = v3_get(base, "/api/v3/get_account_list", p, token)
    _scope_print(args, data)


# 文件分发传输模式：与后端一致（1 严谨模式、2 强制模式）
TRANSFER_MODE_TEXT = {1: "严谨模式", 2: "强制模式"}


def _parse_host_id_list(raw: Optional[str], label: str) -> List[int]:
    """将逗号分隔的主机 ID 字符串解析为整数列表。"""
    if not raw:
        return []
    try:
        return [int(h.strip()) for h in raw.split(",") if h.strip()]
    except ValueError:
        print(f"错误：{label} 须为逗号分隔的整数列表", file=sys.stderr)
        sys.exit(1)


def _parse_ip_list(raw: Optional[str], label: str) -> List[Dict[str, Any]]:
    """将逗号分隔的 bk_cloud_id:ip 字符串解析为 [{bk_cloud_id, ip}] 列表。"""
    if not raw:
        return []
    result: List[Dict[str, Any]] = []
    for item in raw.split(","):
        item = item.strip()
        if not item:
            continue
        parts = item.split(":")
        if len(parts) != 2:
            print(f"错误：{label} 格式无效: {item}，须为 bk_cloud_id:ip", file=sys.stderr)
            sys.exit(1)
        try:
            result.append({"bk_cloud_id": int(parts[0]), "ip": parts[1]})
        except ValueError:
            print(f"错误：{label} 中 bk_cloud_id 须为整数: {item}", file=sys.stderr)
            sys.exit(1)
    return result


def _build_transfer_server(
    host_id_list: Optional[str],
    ip_list: Optional[str],
    server_file: Optional[str],
    label: str,
) -> Optional[Dict[str, Any]]:
    """组装文件分发的 server 结构（目标服务器或服务器文件源的源服务器）。

    优先使用 server_file（完整 JSON，支持动态分组/拓扑节点等复杂结构）；
    否则用 host_id_list / ip_list 组装 host_id_list / ip_list[{bk_cloud_id, ip}]。
    无任何目标时返回 None。
    """
    if server_file:
        srv = _read_json_file(server_file, label)
        if not isinstance(srv, dict):
            print(f"{label} 内容须为 JSON 对象（server 结构）", file=sys.stderr)
            sys.exit(1)
        return srv
    srv: Dict[str, Any] = {}
    hosts = _parse_host_id_list(host_id_list, label + " 的 host-id-list")
    if hosts:
        srv["host_id_list"] = hosts
    ips = _parse_ip_list(ip_list, label + " 的 ip-list")
    if ips:
        srv["ip_list"] = ips
    return srv or None


def cmd_gen_local_upload_url(args: argparse.Namespace) -> None:
    """生成本地文件上传 URL（POST /api/v3/generate_local_file_upload_url）。

    本地文件分发第一步：传入文件名列表，返回 url_map[文件名]={upload_url, path}。
    upload_url 用于 upload-local-file 上传文件字节；path 用于 fast-transfer-file 的本地文件源(file_type=2)。
    """
    token = get_access_token(args.access_token)
    base = get_base_url()
    file_names = _split_comma_keywords(args.file_names)
    if not file_names:
        print("错误：请用 --file-names 指定要上传的文件名（逗号分隔）。", file=sys.stderr)
        sys.exit(1)
    body = {
        **scope_params(args.bk_scope_type, args.bk_scope_id),
        "file_name_list": file_names,
    }
    data = v3_post_json(base, "/api/v3/generate_local_file_upload_url", body, token)
    _scope_print(args, data)


def cmd_upload_local_file(args: argparse.Namespace) -> None:
    """上传本地文件到制品库临时地址（HTTP PUT，非 APIGW）。

    本地文件分发第二步：将 --file-path 指向的本地文件 PUT 到 gen-local-upload-url 返回的 upload_url。
    """
    status, resp_text = http_upload_file(args.upload_url, args.file_path)
    result: Dict[str, Any] = {
        "uploaded": True,
        "http_status": status,
        "file_path": args.file_path,
    }
    try:
        result["response"] = json.loads(resp_text) if resp_text else None
    except json.JSONDecodeError:
        result["response_raw"] = resp_text
    result["_note"] = (
        "上传成功后，用 gen-local-upload-url 返回的对应 path 作为 fast-transfer-file 本地文件源"
        "（file_type=2）的 file_list 传入。"
    )
    print_json(result)


def _reject_unsupported_file_sources(sources: List[Any]) -> None:
    """校验文件源仅为服务器文件(file_type=1)或本地文件(file_type=2)。

    第三方文件源(file_type=3)相关接口暂未提供，技能暂不支持；检测到即报错退出。
    """
    for fs in sources:
        if not isinstance(fs, dict):
            continue
        file_type = fs.get("file_type")
        if file_type is not None and int(file_type) not in (1, 2):
            print(
                "错误：当前技能仅支持服务器文件(file_type=1)与本地文件(file_type=2)分发；"
                "第三方文件源(file_type=3)相关接口暂未提供，暂不支持。",
                file=sys.stderr,
            )
            sys.exit(1)
        if fs.get("file_source_id") is not None or fs.get("file_source_code"):
            print(
                "错误：检测到第三方文件源字段(file_source_id/file_source_code)，"
                "当前技能暂不支持第三方文件源分发，请改用服务器文件或本地文件。",
                file=sys.stderr,
            )
            sys.exit(1)


def _build_transfer_file_sources(args: argparse.Namespace) -> List[Dict[str, Any]]:
    """组装文件分发的 file_source_list。

    仅支持服务器文件源(file_type=1)与本地文件源(file_type=2)。
    优先使用 --file-source-file（完整 JSON 数组，用于服务器/本地文件的滚动等复杂结构）；
    否则用便捷参数组装本地文件源与服务器文件源。第三方文件源(file_type=3)暂不支持。
    """
    if args.file_source_file:
        sources = _read_json_file(args.file_source_file, "--file-source-file")
        if not isinstance(sources, list):
            print("--file-source-file 内容须为 JSON 数组（file_source 列表）", file=sys.stderr)
            sys.exit(1)
        _reject_unsupported_file_sources(sources)
        return sources

    sources: List[Dict[str, Any]] = []

    local_files = _split_comma_keywords(args.local_file_list)
    if local_files:
        sources.append({"file_type": 2, "file_list": local_files})

    server_files = _split_comma_keywords(args.server_file_list)
    if server_files:
        source_server = _build_transfer_server(
            args.source_host_id_list, args.source_ip_list, None, "--source 服务器"
        )
        if not source_server:
            print(
                "错误：使用 --server-file-list 时须指定源服务器 --source-host-id-list 或 --source-ip-list。",
                file=sys.stderr,
            )
            sys.exit(1)
        account: Dict[str, Any] = {}
        if args.source_account_id is not None:
            account["id"] = int(args.source_account_id)
        if args.source_account_alias:
            account["alias"] = args.source_account_alias
        if not account:
            print(
                "错误：使用 --server-file-list 时须指定源账号 --source-account-alias 或 --source-account-id。",
                file=sys.stderr,
            )
            sys.exit(1)
        sources.append(
            {
                "file_type": 1,
                "file_list": server_files,
                "account": account,
                "server": source_server,
            }
        )

    if not sources:
        print(
            "错误：未指定任何源文件。请提供 --local-file-list（本地文件，路径取自 gen-local-upload-url）、"
            "--server-file-list（服务器文件）或 --file-source-file（完整 JSON，用于服务器/本地文件的滚动等复杂结构）。",
            file=sys.stderr,
        )
        sys.exit(1)
    return sources


def cmd_fast_transfer_file(args: argparse.Namespace) -> None:
    """快速分发文件（POST /api/v3/fast_transfer_file）。

    变更类/生产执行操作：真实调用前须遵守确认门禁（G1–G4），支持 --dry-run 预览请求体。
    源文件仅支持：服务器文件(file_type=1)、本地文件(file_type=2，需先 gen-local-upload-url + upload-local-file)。
    第三方文件源(file_type=3)相关接口暂未提供，暂不支持。
    """
    token = get_access_token(args.access_token)
    base = get_base_url()

    if not args.file_target_path:
        print("错误：请用 --file-target-path 指定目标路径。", file=sys.stderr)
        sys.exit(1)
    if args.account_id is None and not args.account_alias:
        print("错误：请提供目标执行账号 --account-alias 或 --account-id。", file=sys.stderr)
        sys.exit(1)

    body: Dict[str, Any] = {
        **scope_params(args.bk_scope_type, args.bk_scope_id),
        "file_target_path": args.file_target_path,
    }
    if args.name:
        body["task_name"] = args.name
    if args.file_target_name:
        body["file_target_name"] = args.file_target_name
    if args.account_id is not None:
        body["account_id"] = int(args.account_id)
    if args.account_alias:
        body["account_alias"] = args.account_alias

    target_server = _build_transfer_server(
        args.target_host_id_list, args.target_ip_list, args.target_server_file, "--target 服务器"
    )
    if not target_server:
        print(
            "错误：未指定目标服务器。请提供 --target-host-id-list、--target-ip-list 或 --target-server-file 之一。",
            file=sys.stderr,
        )
        sys.exit(1)
    body["target_server"] = target_server

    body["file_source_list"] = _build_transfer_file_sources(args)

    if args.transfer_mode is not None:
        mode = int(args.transfer_mode)
        if mode not in TRANSFER_MODE_TEXT:
            print("错误：--transfer-mode 仅支持 1（严谨模式）或 2（强制模式）。", file=sys.stderr)
            sys.exit(1)
        body["transfer_mode"] = mode
    if args.timeout is not None:
        timeout = int(args.timeout)
        if timeout < FAST_EXEC_TIMEOUT_MIN or timeout > FAST_EXEC_TIMEOUT_MAX:
            print(
                f"错误：--timeout 取值须在 {FAST_EXEC_TIMEOUT_MIN}-{FAST_EXEC_TIMEOUT_MAX} 秒之间。",
                file=sys.stderr,
            )
            sys.exit(1)
        body["timeout"] = timeout
    if args.download_speed_limit is not None:
        body["download_speed_limit"] = int(args.download_speed_limit)
    if args.upload_speed_limit is not None:
        body["upload_speed_limit"] = int(args.upload_speed_limit)
    if args.callback_url:
        body["callback_url"] = args.callback_url
    if args.no_start_task:
        body["start_task"] = False

    if args.dry_run:
        _scope_print(args, {"dry_run": True, "request_body": body})
        return

    data = v3_post_json(base, "/api/v3/fast_transfer_file", body, token)
    _scope_print(args, enrich_plan_execute_result(data, get_job_base_url()))


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

    p_il = sub.add_parser(
        "instance-list",
        help="查询任务执行历史（GET /api/v4/get_job_instance_list），按时间窗口列出作业实例，可多条件过滤",
    )
    p_il.add_argument("--bk-scope-type", default="biz", help="biz 或 biz_set，默认 biz")
    p_il.add_argument("--bk-scope-id", required=True, help="资源范围 ID，如业务 ID")
    p_il.add_argument(
        "--lookback-days",
        type=int,
        default=7,
        help=f"回溯天数，默认 7；硬上限 {MAX_JOB_HISTORY_LOOKBACK_DAYS} 天（超出自动截断）",
    )
    p_il.add_argument("--keyword", help="任务名称模糊匹配")
    p_il.add_argument("--operator", help="执行人，精准匹配")
    p_il.add_argument(
        "--launch-mode",
        type=int,
        choices=[1, 2, 3],
        help="执行方式：1 页面执行、2 API调用、3 定时执行",
    )
    p_il.add_argument(
        "--type",
        type=int,
        choices=[0, 1, 2],
        help="任务类型：0 作业执行、1 脚本执行、2 文件分发",
    )
    p_il.add_argument(
        "--status",
        type=int,
        help="任务状态，如 3 执行成功、4 执行失败（取值见 troubleshooting 手册状态码表）",
    )
    p_il.add_argument("--ip", help="执行目标服务器 IP，精准匹配")
    p_il.add_argument("--cron-id", type=int, help="按定时任务 ID 过滤")
    p_il.add_argument(
        "--job-instance-id",
        type=int,
        help="按实例 ID 精确查询；传入后接口将忽略其余过滤条件",
    )
    p_il.add_argument("--offset", type=int, default=0, help="从第几条开始，最大 10000，默认 0")
    p_il.add_argument(
        "--length",
        type=int,
        default=LIST_PAGE_DEFAULT,
        help=f"本页返回条数，默认 {LIST_PAGE_DEFAULT}，接口最大 200",
    )
    p_il.set_defaults(func=cmd_instance_list)

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

    p_fe = sub.add_parser(
        "fast-execute-script",
        help="快速执行脚本：到指定机器执行脚本（POST /api/v4/fast_execute_script，写操作，须过确认门禁）",
    )
    p_fe.add_argument("--bk-scope-type", default="biz", help="biz 或 biz_set，默认 biz")
    p_fe.add_argument("--bk-scope-id", required=True, help="资源范围 ID，如业务 ID")
    p_fe.add_argument("--name", help="自定义作业名称，长度不超过 512 字符")
    p_fe.add_argument(
        "--script-content",
        help="脚本内容明文（脚本会自动 Base64 编码）；PowerShell 等环境推荐用 --script-content-file",
    )
    p_fe.add_argument(
        "--script-content-file",
        help="从文件读取脚本内容明文（避免命令行转义问题，脚本自动 Base64 编码）",
    )
    p_fe.add_argument("--script-id", help="引用已有脚本 ID（与 --script-version-id/--script-content 三选一）")
    p_fe.add_argument("--script-version-id", type=int, help="引用已有脚本版本 ID（优先级最高）")
    p_fe.add_argument(
        "--script-language",
        help="脚本语言：名称（shell/bat/perl/python/powershell）或编码 1-5，默认 shell；引用脚本时忽略",
    )
    p_fe.add_argument("--script-param", help="脚本参数明文（脚本会自动 Base64 编码）")
    p_fe.add_argument(
        "--param-sensitive",
        action="store_true",
        help="标记脚本参数为敏感参数（执行详情页隐藏），默认否",
    )
    p_fe.add_argument("--account-alias", help="执行账号别名，如 root；与 --account-id 至少提供一个")
    p_fe.add_argument("--account-id", type=int, help="执行账号 ID；与 --account-alias 同时存在时优先")
    p_fe.add_argument(
        "--host-id-list",
        help="目标主机 bk_host_id 列表，逗号分隔",
    )
    p_fe.add_argument(
        "--ip-list",
        help="目标主机 IP 列表，逗号分隔，格式为 bk_cloud_id:ip",
    )
    p_fe.add_argument(
        "--execute-target-file",
        help="从文件读取完整 execute_target JSON（支持动态分组/拓扑节点/容器等复杂目标）",
    )
    p_fe.add_argument("--windows-interpreter", help="自定义 Windows 解释器路径，须以 .exe 结尾")
    p_fe.add_argument(
        "--timeout",
        type=int,
        help=f"脚本执行超时时间（秒），取值 {FAST_EXEC_TIMEOUT_MIN}-{FAST_EXEC_TIMEOUT_MAX}，不传则用接口默认 7200",
    )
    p_fe.add_argument("--callback-url", help="任务执行完成后的回调 URL")
    p_fe.add_argument(
        "--no-start-task",
        action="store_true",
        help="仅创建任务不自动启动（start_task=false），默认自动启动",
    )
    p_fe.add_argument(
        "--dry-run",
        action="store_true",
        help="只打印将提交的请求体（含 Base64 后的脚本），不调用执行接口",
    )
    p_fe.set_defaults(func=cmd_fast_execute_script)

    p_las = sub.add_parser(
        "list-authorized-scopes",
        help="查询当前用户有权限的业务/业务集（GET /api/v4/get_user_authorized_scopes），用于首次引导选择范围",
    )
    p_las.add_argument("--offset", type=int, default=0, help="分页起始偏移，从 0 开始，默认 0")
    p_las.add_argument(
        "--length",
        type=int,
        default=LIST_PAGE_DEFAULT,
        help=f"单页返回条数，默认 {LIST_PAGE_DEFAULT}，取值范围 1-200",
    )
    p_las.set_defaults(func=cmd_list_authorized_scopes)

    p_htt = sub.add_parser(
        "host-topo-tree",
        help="查询业务主机拓扑树（POST /api/v4/get_biz_host_topo_tree，仅业务 biz，默认全展开）",
    )
    p_htt.add_argument("--bk-scope-type", default="biz", help="仅支持 biz；业务集/租户集请用 host-search")
    p_htt.add_argument("--bk-scope-id", required=True, help="业务 ID")
    p_htt.set_defaults(func=cmd_host_topo_tree)

    p_hs = sub.add_parser(
        "host-search",
        help="按条件搜索资源范围下主机（POST /api/v4/search_scope_host），用于执行类操作填写主机",
    )
    p_hs.add_argument("--bk-scope-type", default="biz", help="biz 业务 / biz_set 业务集 / tenant_set 租户集，默认 biz")
    p_hs.add_argument("--bk-scope-id", required=True, help="资源范围 ID")
    p_hs.add_argument("--ipv4", help="IPv4 关键字列表，逗号分隔，模糊匹配，如 127.0.0.1,10.0")
    p_hs.add_argument("--ipv6", help="IPv6 关键字列表，逗号分隔，模糊匹配")
    p_hs.add_argument("--host-name", help="主机名称关键字列表，逗号分隔，模糊匹配")
    p_hs.add_argument("--os-name", help="操作系统名称关键字列表，逗号分隔，模糊匹配，如 linux,centos")
    p_hs.add_argument(
        "--alive",
        type=int,
        help="Agent 状态过滤：0 异常、1 正常；不传则不按 Agent 状态过滤",
    )
    p_hs.add_argument(
        "--topo-nodes",
        help='拓扑节点 JSON 数组（仅业务生效），如 \'[{"object_id":"module","instance_id":2001}]\'；PowerShell 推荐用 --topo-nodes-file',
    )
    p_hs.add_argument(
        "--topo-nodes-file",
        help="从文件读取拓扑节点 JSON 数组（避免命令行转义问题）；节点 object_id 取 biz/set/module，instance_id 取拓扑树 instance_id",
    )
    p_hs.add_argument("--offset", type=int, default=0, help="分页起始偏移，从 0 开始，默认 0")
    p_hs.add_argument(
        "--length",
        type=int,
        default=LIST_PAGE_DEFAULT,
        help=f"单页返回条数，默认 {LIST_PAGE_DEFAULT}，取值范围 1-200",
    )
    p_hs.set_defaults(func=cmd_host_search)

    p_al = sub.add_parser(
        "account-list",
        help="查询业务下执行账号列表（GET /api/v3/get_account_list），用于执行类操作填写账号时引导选择",
    )
    p_al.add_argument("--bk-scope-type", default="biz", help="biz 业务 / biz_set 业务集，默认 biz")
    p_al.add_argument("--bk-scope-id", required=True, help="资源范围 ID，如业务 ID")
    p_al.add_argument(
        "--category",
        help="账号用途过滤：名称 system/db 或编码 1（系统账号）、2（DB账号）；不传则不区分",
    )
    p_al.add_argument("--account", help="账号名称，精确/模糊匹配（按接口实现）")
    p_al.add_argument("--alias", help="账号别名过滤")
    p_al.add_argument("--start", type=int, default=0, help="分页起始位置，从 0 开始，默认 0")
    p_al.add_argument(
        "--length",
        type=int,
        default=LIST_PAGE_DEFAULT,
        help=f"单页返回条数，默认 {LIST_PAGE_DEFAULT}（接口最大 1000）",
    )
    p_al.set_defaults(func=cmd_account_list)

    p_gu = sub.add_parser(
        "gen-local-upload-url",
        help="生成本地文件上传 URL（POST /api/v3/generate_local_file_upload_url），本地文件分发第一步",
    )
    p_gu.add_argument("--bk-scope-type", default="biz", help="biz 业务 / biz_set 业务集，默认 biz")
    p_gu.add_argument("--bk-scope-id", required=True, help="资源范围 ID，如业务 ID")
    p_gu.add_argument(
        "--file-names",
        required=True,
        help="要上传的文件名列表，逗号分隔，如 a.sh,b.tar.gz（仅文件名，用于生成上传地址与分发路径）",
    )
    p_gu.set_defaults(func=cmd_gen_local_upload_url)

    p_ulf = sub.add_parser(
        "upload-local-file",
        help="上传本地文件到制品库临时地址（HTTP PUT，非 APIGW），本地文件分发第二步",
    )
    p_ulf.add_argument(
        "--upload-url",
        required=True,
        help="gen-local-upload-url 返回的 upload_url（自带鉴权 token）",
    )
    p_ulf.add_argument("--file-path", required=True, help="待上传的本地文件绝对/相对路径")
    p_ulf.set_defaults(func=cmd_upload_local_file)

    p_ft = sub.add_parser(
        "fast-transfer-file",
        help="快速分发文件：分发文件到目标机器（POST /api/v3/fast_transfer_file，写操作，须过确认门禁）",
    )
    p_ft.add_argument("--bk-scope-type", default="biz", help="biz 业务 / biz_set 业务集，默认 biz")
    p_ft.add_argument("--bk-scope-id", required=True, help="资源范围 ID，如业务 ID")
    p_ft.add_argument("--name", help="自定义作业名称，长度不超过 512 字符")
    p_ft.add_argument("--file-target-path", help="文件分发目标路径（必填），如 /tmp/")
    p_ft.add_argument("--file-target-name", help="目标文件名（可选），不传保持源文件名")
    p_ft.add_argument("--account-alias", help="目标机器执行账号别名；与 --account-id 至少提供一个")
    p_ft.add_argument("--account-id", type=int, help="目标机器执行账号 ID；与 --account-alias 同时存在时优先")
    p_ft.add_argument("--target-host-id-list", help="目标主机 bk_host_id 列表，逗号分隔")
    p_ft.add_argument("--target-ip-list", help="目标主机 IP 列表，逗号分隔，格式为 bk_cloud_id:ip")
    p_ft.add_argument(
        "--target-server-file",
        help="从文件读取完整 target_server JSON（支持动态分组/拓扑节点等复杂目标）",
    )
    p_ft.add_argument(
        "--local-file-list",
        help="本地文件源路径列表（file_type=2），逗号分隔；路径取自 gen-local-upload-url 返回的 path，须先 upload-local-file 上传",
    )
    p_ft.add_argument(
        "--server-file-list",
        help="服务器文件源路径列表（file_type=1），逗号分隔，源文件绝对路径；须配合 --source-account-* 与 --source-host-id-list/--source-ip-list",
    )
    p_ft.add_argument("--source-account-alias", help="服务器文件源账号别名（--server-file-list 时用）")
    p_ft.add_argument("--source-account-id", type=int, help="服务器文件源账号 ID（--server-file-list 时用）")
    p_ft.add_argument("--source-host-id-list", help="服务器文件源主机 bk_host_id 列表，逗号分隔")
    p_ft.add_argument("--source-ip-list", help="服务器文件源主机 IP 列表，逗号分隔，格式为 bk_cloud_id:ip")
    p_ft.add_argument(
        "--file-source-file",
        help="从文件读取完整 file_source_list JSON 数组（仅服务器文件 file_type=1 / 本地文件 file_type=2，用于滚动等复杂结构；第三方文件源暂不支持）",
    )
    p_ft.add_argument(
        "--transfer-mode",
        type=int,
        help="传输模式：1 严谨模式、2 强制模式；不传用接口默认（强制模式）",
    )
    p_ft.add_argument(
        "--timeout",
        type=int,
        help=f"任务超时时间（秒），取值 {FAST_EXEC_TIMEOUT_MIN}-{FAST_EXEC_TIMEOUT_MAX}，不传用接口默认 7200",
    )
    p_ft.add_argument("--download-speed-limit", type=int, help="下载限速，单位 MB；不传不限速")
    p_ft.add_argument("--upload-speed-limit", type=int, help="上传限速，单位 MB；不传不限速")
    p_ft.add_argument("--callback-url", help="任务执行完成后的回调 URL")
    p_ft.add_argument(
        "--no-start-task",
        action="store_true",
        help="仅创建任务不自动启动（start_task=false），默认自动启动",
    )
    p_ft.add_argument(
        "--dry-run",
        action="store_true",
        help="只打印将提交的请求体，不调用分发接口",
    )
    p_ft.set_defaults(func=cmd_fast_transfer_file)

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
