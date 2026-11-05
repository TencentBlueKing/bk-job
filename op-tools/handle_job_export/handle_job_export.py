#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把 BK-JOB 3.11.x 导出的作业包改成 3.12.x 能导入的时间格式。

3.11 导出时，作业模板、执行方案、标签上的 createTime / lastModifyTime
被写成 "yyyy-MM-dd HH:mm:ss"。3.12 这些字段是 Long，导入时期望毫秒时间戳。
本工具解开 zip（下载下来的文件名通常是 .jobexport，内容仍是 zip），
只改这两个字段，再压回 zip。

不依赖第三方库。加密导出包（文件头为作业平台魔数）无法在无密码时处理。
"""

from __future__ import print_function

import argparse
import io
import json
import os
import re
import sys
import zipfile
from datetime import datetime, timedelta, timezone

# 与 LongTimestampSerializer / JobContextUtil 默认时区一致：东八区
DEFAULT_TZ_OFFSET_HOURS = 8
DATETIME_PATTERN = re.compile(r"^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}$")
INTEGER_PATTERN = re.compile(r"^-?\d+$")
# 3.11 用 LongTimestampSerializer 写成字符串、3.12 改为 Long 的字段
TIME_FIELD_NAMES = frozenset(["createTime", "lastModifyTime"])
# Constant.JOB_FILE_MAGIC，加密导出包的文件头
JOB_FILE_MAGIC = b"\x18\x01\x19\x24\x09\x18\x13\x10"
IMPORTABLE_SUFFIXES = (".jobexport", ".zip")


class ExportHandleError(Exception):
    """导出包无法处理时抛出，信息可直接给用户看。"""


def tz_from_offset_hours(hours):
    return timezone(timedelta(hours=hours))


def datetime_text_to_millis(text, tz):
    """把 yyyy-MM-dd HH:mm:ss 按给定时区解释成毫秒时间戳。"""
    try:
        local_dt = datetime.strptime(text, "%Y-%m-%d %H:%M:%S")
    except ValueError as exc:
        raise ExportHandleError("无法解析时间 %r：%s" % (text, exc))
    aware = local_dt.replace(tzinfo=tz)
    return int(aware.timestamp() * 1000)


def convert_time_value(value, path, tz, counter):
    """转换一个时间字段的值。已经是数字的保持原样。"""
    if value is None or isinstance(value, bool):
        return value
    if isinstance(value, int):
        return value
    if isinstance(value, float):
        if value.is_integer():
            return int(value)
        raise ExportHandleError("%s 是小数 %r，无法当作时间戳" % (path, value))
    if isinstance(value, list):
        return [
            convert_time_value(item, "%s[%d]" % (path, index), tz, counter)
            for index, item in enumerate(value)
        ]
    if isinstance(value, str):
        text = value.strip()
        if text == "":
            counter[0] += 1
            return 0
        if INTEGER_PATTERN.match(text):
            counter[0] += 1
            return int(text)
        if DATETIME_PATTERN.match(text):
            counter[0] += 1
            return datetime_text_to_millis(text, tz)
        raise ExportHandleError(
            "%s 的值 %r 不是 yyyy-MM-dd HH:mm:ss，也不是时间戳" % (path, value)
        )
    raise ExportHandleError("%s 的类型 %s 无法转换成时间戳" % (path, type(value).__name__))


def convert_node(node, path, tz, counter):
    if isinstance(node, dict):
        for key, value in node.items():
            child_path = "%s.%s" % (path, key) if path else key
            if key in TIME_FIELD_NAMES:
                node[key] = convert_time_value(value, child_path, tz, counter)
            else:
                convert_node(value, child_path, tz, counter)
    elif isinstance(node, list):
        for index, item in enumerate(node):
            convert_node(item, "%s[%d]" % (path, index), tz, counter)


def convert_json_bytes(data, tz):
    """
    转换一段 JSON。
    返回 (新内容或 None, 改写的字段数)。None 表示内容无需改写，调用方应保留原字节。
    """
    try:
        text = data.decode("utf-8")
    except UnicodeDecodeError:
        return None, 0
    try:
        document = json.loads(text)
    except ValueError:
        return None, 0
    counter = [0]
    convert_node(document, "", tz, counter)
    if counter[0] == 0:
        return None, 0
    new_text = json.dumps(document, ensure_ascii=False, separators=(",", ":"))
    return new_text.encode("utf-8"), counter[0]


def _is_encrypted_export(header):
    return header.startswith(JOB_FILE_MAGIC)


def _clone_zip_info(src_info):
    date_time = src_info.date_time
    # ZipInfo 不接受 1980 年之前的时间
    if not date_time or date_time[0] < 1980:
        date_time = (1980, 1, 1, 0, 0, 0)
    info = zipfile.ZipInfo(filename=src_info.filename, date_time=date_time)
    if src_info.compress_type == zipfile.ZIP_STORED:
        info.compress_type = zipfile.ZIP_STORED
    else:
        info.compress_type = src_info.compress_type or zipfile.ZIP_DEFLATED
    info.external_attr = src_info.external_attr
    # 只保留 UTF-8 文件名标记，避免沿用旧的数据描述符标记导致长度错乱
    info.flag_bits = src_info.flag_bits & 0x800
    return info


def convert_zip_bytes(data, tz):
    """
    处理一个导出 zip 的字节。
    返回 (新 zip 字节, 统计 dict)。
    """
    if _is_encrypted_export(data[:len(JOB_FILE_MAGIC)]):
        raise ExportHandleError(
            "这是加密后的导出包，时间字段在密文里，本工具不能直接改。"
            "请用不带密码重新导出，或先在 3.11 解密后再处理。"
        )
    if not zipfile.is_zipfile(io.BytesIO(data)):
        raise ExportHandleError("文件不是 zip。3.11 导出包内容应是 zip（下载名通常为 .jobexport）。")

    stats = {
        "json_entries": 0,
        "converted_entries": [],
        "time_fields": 0,
        "skipped_non_json": 0,
    }
    output = io.BytesIO()
    with zipfile.ZipFile(io.BytesIO(data), "r") as source, zipfile.ZipFile(
        output, "w", compression=zipfile.ZIP_DEFLATED, allowZip64=True
    ) as target:
        for info in source.infolist():
            raw = source.read(info.filename)
            new_info = _clone_zip_info(info)
            if info.is_dir() or info.filename.endswith("/"):
                target.writestr(new_info, raw)
                continue
            name = info.filename.replace("\\", "/")
            base_name = name.rsplit("/", 1)[-1]
            if base_name.lower().endswith(".json"):
                stats["json_entries"] += 1
                converted, count = convert_json_bytes(raw, tz)
                if converted is None:
                    target.writestr(new_info, raw)
                else:
                    target.writestr(new_info, converted)
                    stats["converted_entries"].append(name)
                    stats["time_fields"] += count
            else:
                stats["skipped_non_json"] += 1
                target.writestr(new_info, raw)
    if stats["json_entries"] == 0:
        raise ExportHandleError("压缩包里没有 .json 文件，不像是作业导出包。")
    return output.getvalue(), stats


def default_output_path(src_path):
    """原名-handled，保留原扩展名，这样 .jobexport 处理后仍可直接导入。"""
    directory, filename = os.path.split(src_path)
    stem, suffix = os.path.splitext(filename)
    if suffix.lower() in IMPORTABLE_SUFFIXES:
        new_name = stem + "-handled" + suffix
    else:
        new_name = filename + "-handled.zip"
    return os.path.join(directory, new_name)


def convert_file(src_path, dst_path, tz):
    with open(src_path, "rb") as src_file:
        data = src_file.read()
    new_bytes, stats = convert_zip_bytes(data, tz)
    parent = os.path.dirname(os.path.abspath(dst_path))
    if parent and not os.path.isdir(parent):
        os.makedirs(parent)
    with open(dst_path, "wb") as dst_file:
        dst_file.write(new_bytes)
    return stats


def build_parser():
    parser = argparse.ArgumentParser(
        description="把 3.11.x 作业导出包里的 yyyy-MM-dd HH:mm:ss 时间字段改成 3.12.x 的毫秒时间戳"
    )
    parser.add_argument("package", help="3.11 导出的文件，内容为 zip，扩展名一般为 .jobexport 或 .zip")
    parser.add_argument(
        "-o",
        "--output",
        help="输出路径，默认是同目录下的 <原名>-handled<原扩展名>",
    )
    parser.add_argument(
        "--tz-offset",
        type=int,
        default=DEFAULT_TZ_OFFSET_HOURS,
        help="时间字符串所在时区的小时偏移，默认 8（东八区，与作业平台默认时区一致）",
    )
    parser.add_argument(
        "--force",
        action="store_true",
        help="输出文件已存在时覆盖",
    )
    return parser


def main(argv=None):
    parser = build_parser()
    args = parser.parse_args(argv)
    src_path = args.package
    if not os.path.isfile(src_path):
        print("找不到文件：%s" % src_path, file=sys.stderr)
        return 1
    dst_path = args.output or default_output_path(src_path)
    if os.path.abspath(src_path) == os.path.abspath(dst_path):
        print("输出路径不能和原文件相同", file=sys.stderr)
        return 1
    if os.path.exists(dst_path) and not args.force:
        print("输出文件已存在：%s （加 --force 覆盖）" % dst_path, file=sys.stderr)
        return 1
    try:
        stats = convert_file(src_path, dst_path, tz_from_offset_hours(args.tz_offset))
    except ExportHandleError as exc:
        print(str(exc), file=sys.stderr)
        return 1
    except (OSError, zipfile.BadZipFile, RuntimeError) as exc:
        print("处理失败：%s" % exc, file=sys.stderr)
        return 1

    print("已写出：%s" % dst_path)
    print("json 条目：%d，改写时间字段：%d" % (stats["json_entries"], stats["time_fields"]))
    if stats["converted_entries"]:
        print("已修改：%s" % ", ".join(stats["converted_entries"]))
    else:
        print("没有发现 yyyy-MM-dd HH:mm:ss 格式的 createTime / lastModifyTime，已原样打包")
    return 0


if __name__ == "__main__":
    sys.exit(main())
