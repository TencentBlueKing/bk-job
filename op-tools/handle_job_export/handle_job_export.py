#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
把 BK-JOB 3.11.x 导出的作业包改成 3.12.x 能导入的时间格式。

3.11 导出时，作业模板、执行方案、标签上的 createTime / lastModifyTime
被写成 "yyyy-MM-dd HH:mm:ss"。3.12 这些字段是 Long，导入时期望毫秒时间戳。
本工具解开 zip（下载下来的文件名通常是 .jobexport，内容仍是 zip），
只改这两个字段，再压回 zip。

不依赖第三方库。加密导出包（文件头为作业平台魔数）无法在无密码时处理。
读取前会限制导出包大小、条目数量、单个条目以及全部条目的解压后大小，避免畸形 zip 撑满内存。
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
# 正常导出包远小于这些值。上限用来挡住高压缩比 zip，处理超大的真实导出包时可用命令行调高。
DEFAULT_MAX_PACKAGE_BYTES = 512 * 1024 * 1024
DEFAULT_MAX_ENTRY_BYTES = 100 * 1024 * 1024
DEFAULT_MAX_TOTAL_BYTES = 512 * 1024 * 1024
DEFAULT_MAX_ENTRY_COUNT = 10000


class ExportHandleError(Exception):
    """导出包无法处理时抛出，信息可直接给用户看。"""


class ZipLimits(object):
    """读取导出包时的大小上限。比较的是 zip 中央目录里声明的解压后大小，读之前就拒绝。"""

    def __init__(self, max_package_bytes=DEFAULT_MAX_PACKAGE_BYTES,
                 max_entry_bytes=DEFAULT_MAX_ENTRY_BYTES,
                 max_total_bytes=DEFAULT_MAX_TOTAL_BYTES,
                 max_entry_count=DEFAULT_MAX_ENTRY_COUNT):
        self.max_package_bytes = max_package_bytes
        self.max_entry_bytes = max_entry_bytes
        self.max_total_bytes = max_total_bytes
        self.max_entry_count = max_entry_count


def _format_size(num_bytes):
    if num_bytes >= 1024 * 1024 and num_bytes % (1024 * 1024) == 0:
        return "%dMB" % (num_bytes // (1024 * 1024))
    if num_bytes >= 1024 and num_bytes % 1024 == 0:
        return "%dKB" % (num_bytes // 1024)
    return "%d 字节" % num_bytes


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


def _is_dir_entry(info):
    name = info.filename
    if name.endswith("/") or name.endswith("\\"):
        return True
    is_dir = getattr(info, "is_dir", None)
    return bool(is_dir and is_dir())


def _reject_entry_count(count, limits):
    if count > limits.max_entry_count:
        raise ExportHandleError(
            "压缩包内有 %d 个条目，超过上限 %d" % (count, limits.max_entry_count)
        )


def _find_eocd(tail):
    """在文件尾里定位中央目录结束记录，并用注释长度确认没有找错签名。"""
    signature = b"PK\x05\x06"
    start = len(tail)
    while start >= 22:
        pos = tail.rfind(signature, 0, start)
        if pos < 0:
            return None
        if pos + 22 > len(tail):
            start = pos
            continue
        comment_len = int.from_bytes(tail[pos + 20:pos + 22], "little")
        if pos + 22 + comment_len == len(tail):
            return pos
        start = pos
    return None


def _declared_entry_count(file_size, read_range):
    """
    从中央目录结束记录读出条目数。
    在 ZipFile 解析全部条目之前调用，避免海量条目先变成对象。
    解析不了时返回 None，交给后面的 ZipFile 再判断。
    """
    if file_size < 22:
        return None
    tail_len = min(file_size, 22 + 65535)
    tail = read_range(file_size - tail_len, tail_len)
    pos = _find_eocd(tail)
    if pos is None:
        return None
    total_entries = int.from_bytes(tail[pos + 10:pos + 12], "little")
    if total_entries != 0xFFFF:
        return total_entries
    locator_sig = b"PK\x06\x07"
    loc = tail.rfind(locator_sig, 0, pos)
    if loc < 0 or loc + 20 > pos:
        return None
    zip64_offset = int.from_bytes(tail[loc + 8:loc + 16], "little")
    if zip64_offset < 0 or zip64_offset >= file_size:
        return None
    header = read_range(zip64_offset, 44)
    if len(header) < 44 or not header.startswith(b"PK\x06\x06"):
        return None
    return int.from_bytes(header[32:40], "little")


def _reject_oversized_archive(infos, limits):
    """按中央目录里的解压后大小拒绝，不先把条目读进内存。"""
    _reject_entry_count(len(infos), limits)
    total = 0
    for info in infos:
        if _is_dir_entry(info):
            continue
        size = info.file_size
        if size < 0:
            raise ExportHandleError("条目 %s 的大小非法" % info.filename)
        if size > limits.max_entry_bytes:
            raise ExportHandleError(
                "条目 %s 解压后大小为 %s，超过单文件上限 %s"
                % (info.filename, _format_size(size), _format_size(limits.max_entry_bytes))
            )
        total += size
        if total > limits.max_total_bytes:
            raise ExportHandleError(
                "解压后总大小超过上限 %s" % _format_size(limits.max_total_bytes)
            )


def _read_entry_limited(source, info, limits):
    """最多读到单文件上限，避免声明大小不可信时仍把超大内容解压出来。"""
    with source.open(info, "r") as entry:
        raw = entry.read(limits.max_entry_bytes + 1)
    if len(raw) > limits.max_entry_bytes:
        raise ExportHandleError(
            "条目 %s 实际解压超过单文件上限 %s"
            % (info.filename, _format_size(limits.max_entry_bytes))
        )
    return raw


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


def _rewrite_zip(source, target, tz, limits):
    """把 source 里的条目写入 target。超限时直接失败，不继续解压。"""
    infos = source.infolist()
    _reject_oversized_archive(infos, limits)
    stats = {
        "json_entries": 0,
        "converted_entries": [],
        "time_fields": 0,
        "skipped_non_json": 0,
    }
    read_total = 0
    for info in infos:
        new_info = _clone_zip_info(info)
        if _is_dir_entry(info):
            target.writestr(new_info, b"")
            continue
        raw = _read_entry_limited(source, info, limits)
        read_total += len(raw)
        if read_total > limits.max_total_bytes:
            raise ExportHandleError(
                "解压后总大小超过上限 %s" % _format_size(limits.max_total_bytes)
            )
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
    return stats


def _encrypted_export_message():
    return (
        "这是加密后的导出包，时间字段在密文里，本工具不能直接改。"
        "请用不带密码重新导出，或先在 3.11 解密后再处理。"
    )


def convert_zip_bytes(data, tz, limits=None):
    """
    处理一个导出 zip 的字节。
    返回 (新 zip 字节, 统计 dict)。
    """
    if limits is None:
        limits = ZipLimits()
    if len(data) > limits.max_package_bytes:
        raise ExportHandleError(
            "导出包大小为 %s，超过上限 %s" % (_format_size(len(data)), _format_size(limits.max_package_bytes))
        )
    if _is_encrypted_export(data[:len(JOB_FILE_MAGIC)]):
        raise ExportHandleError(_encrypted_export_message())
    if not zipfile.is_zipfile(io.BytesIO(data)):
        raise ExportHandleError("文件不是 zip。3.11 导出包内容应是 zip（下载名通常为 .jobexport）。")
    declared_count = _declared_entry_count(len(data), lambda offset, length: data[offset:offset + length])
    if declared_count is not None:
        _reject_entry_count(declared_count, limits)

    output = io.BytesIO()
    with zipfile.ZipFile(io.BytesIO(data), "r") as source, zipfile.ZipFile(
        output, "w", compression=zipfile.ZIP_DEFLATED, allowZip64=True
    ) as target:
        stats = _rewrite_zip(source, target, tz, limits)
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


def _check_package_file(src_path, limits):
    size = os.path.getsize(src_path)
    if size > limits.max_package_bytes:
        raise ExportHandleError(
            "导出包大小为 %s，超过上限 %s" % (_format_size(size), _format_size(limits.max_package_bytes))
        )
    with open(src_path, "rb") as src_file:
        header = src_file.read(len(JOB_FILE_MAGIC))
    if _is_encrypted_export(header):
        raise ExportHandleError(_encrypted_export_message())
    if not zipfile.is_zipfile(src_path):
        raise ExportHandleError("文件不是 zip。3.11 导出包内容应是 zip（下载名通常为 .jobexport）。")
    declared_count = _declared_entry_count(
        size, lambda offset, length: _read_file_range(src_path, offset, length)
    )
    if declared_count is not None:
        _reject_entry_count(declared_count, limits)


def _read_file_range(path, offset, length):
    with open(path, "rb") as src_file:
        src_file.seek(offset)
        return src_file.read(length)


def convert_file(src_path, dst_path, tz, limits=None):
    """从磁盘读 zip、把结果直接写到目标文件，不把整个包放进内存。"""
    if limits is None:
        limits = ZipLimits()
    _check_package_file(src_path, limits)
    parent = os.path.dirname(os.path.abspath(dst_path))
    if parent and not os.path.isdir(parent):
        os.makedirs(parent)
    tmp_path = dst_path + ".tmp"
    try:
        with zipfile.ZipFile(src_path, "r") as source, zipfile.ZipFile(
            tmp_path, "w", compression=zipfile.ZIP_DEFLATED, allowZip64=True
        ) as target:
            stats = _rewrite_zip(source, target, tz, limits)
        os.replace(tmp_path, dst_path)
    except BaseException:
        if os.path.exists(tmp_path):
            try:
                os.remove(tmp_path)
            except OSError:
                pass
        raise
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
    parser.add_argument(
        "--max-package-mb",
        type=int,
        default=DEFAULT_MAX_PACKAGE_BYTES // (1024 * 1024),
        help="导出包文件本身的上限（MB），默认 512。超过则不读取",
    )
    parser.add_argument(
        "--max-entry-mb",
        type=int,
        default=DEFAULT_MAX_ENTRY_BYTES // (1024 * 1024),
        help="单个条目解压后的上限（MB），默认 100。超过则不读取该条目",
    )
    parser.add_argument(
        "--max-total-mb",
        type=int,
        default=DEFAULT_MAX_TOTAL_BYTES // (1024 * 1024),
        help="全部条目解压后的总上限（MB），默认 512",
    )
    return parser


def _limits_from_args(args):
    for name, value in (
        ("--max-package-mb", args.max_package_mb),
        ("--max-entry-mb", args.max_entry_mb),
        ("--max-total-mb", args.max_total_mb),
    ):
        if value <= 0:
            raise ExportHandleError("%s 必须是正整数" % name)
    return ZipLimits(
        max_package_bytes=args.max_package_mb * 1024 * 1024,
        max_entry_bytes=args.max_entry_mb * 1024 * 1024,
        max_total_bytes=args.max_total_mb * 1024 * 1024,
    )


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
        limits = _limits_from_args(args)
        stats = convert_file(src_path, dst_path, tz_from_offset_hours(args.tz_offset), limits)
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
