#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""把 AIDEV 资源文件中的 ${占位符} 渲染为部署环境的真实值。

占位符取值来自同名环境变量，环境变量由 Helm 的同步 Job 注入。

刻意逐个按名单替换，而不是全量替换所有 ${xxx}：知识库文档中大量出现作业平台
魔法变量的字面写法（如 ${JOB_NAMESPACE_ALL}、${svr_addr}），全量替换会把这些
说明文档中的示例清成空串。
"""
import argparse
import os
import sys

# 支持渲染的占位符名单。新增占位符时在此追加变量名，并在
# support-files/kubernetes/charts/bk-job/templates/job-migration/sync-bkaidev-job.yaml
# 中补上对应的环境变量。
PLACEHOLDER_NAMES = [
    "JOB_URL_BASE",
    # Agent 的 spec.generate_type 取值：多租户为 global（全租户），非多租户为 public（所有空间可见）
    "JOB_AI_GENERATE_TYPE",
]

# 需要渲染的路径，相对于资源根目录。
# 刻意不含 bin 目录：其中只有脚本，没有需要渲染的资源。
RENDER_PATHS = [
    "bkai.yaml",
    "agents",
    "skills",
    "knowledgebases",
]

# 只处理文本资源文件，避免误改二进制文件
TEXT_SUFFIXES = {".yaml", ".yml", ".md", ".json", ".txt", ".j2"}


def log(level, message):
    print("[{}] {}".format(level, message), flush=True)


def iter_target_files(base_dir):
    """按 RENDER_PATHS 收集待渲染的文本文件，路径不存在时跳过。"""
    for relative_path in RENDER_PATHS:
        path = os.path.join(base_dir, relative_path)
        if not os.path.exists(path):
            continue
        if os.path.isfile(path):
            yield path
            continue
        for current_dir, _, file_names in os.walk(path):
            for file_name in sorted(file_names):
                if os.path.splitext(file_name)[1].lower() in TEXT_SUFFIXES:
                    yield os.path.join(current_dir, file_name)


def read_files(paths):
    """读取待渲染文件的内容。

    newline="" 保证读写不改动原文件的换行风格；编码固定 UTF-8，
    资源文件出现其他编码时直接报错，避免静默写坏文件。
    """
    contents = {}
    for path in paths:
        try:
            with open(path, encoding="utf-8", newline="") as file:
                contents[path] = file.read()
        except UnicodeDecodeError as error:
            log("ERROR", "Resource file {} is not utf-8 encoded: {}".format(path, error))
            return None
    return contents


def render(base_dir):
    paths = list(iter_target_files(base_dir))
    if not paths:
        log("WARN", "No resource file to render under {}".format(base_dir))
        return 0
    log("INFO", "Found {} resource file(s) under {}".format(len(paths), base_dir))

    contents = read_files(paths)
    if contents is None:
        return 1
    originals = dict(contents)

    for name in PLACEHOLDER_NAMES:
        placeholder = "${{{}}}".format(name)
        used_paths = [path for path in paths if placeholder in contents[path]]
        if not used_paths:
            log("INFO", "Placeholder {} is not used, skipped".format(placeholder))
            continue
        value = os.environ.get(name, "")
        if not value:
            log("ERROR", "Placeholder {} is used in {} file(s) but env {} is empty".format(
                placeholder, len(used_paths), name))
            return 1
        log("INFO", "Rendering {} -> {}".format(placeholder, value))
        for path in used_paths:
            text = contents[path]
            log("INFO", "  {}: {} occurrence(s)".format(path, text.count(placeholder)))
            contents[path] = text.replace(placeholder, value)

    changed = 0
    for path, text in contents.items():
        if text == originals[path]:
            continue
        with open(path, "w", encoding="utf-8", newline="") as file:
            file.write(text)
        changed += 1
    log("INFO", "Rendered {} file(s)".format(changed))
    return 0


def main():
    parser = argparse.ArgumentParser(
        description="Render placeholders in aidev resource files before syncing to the aidev platform")
    parser.add_argument("--base-dir", required=True, help="aidev resource base dir, e.g. /bk-job")
    args = parser.parse_args()
    if not os.path.isdir(args.base_dir):
        log("ERROR", "Base dir {} does not exist".format(args.base_dir))
        return 1
    return render(args.base_dir)


if __name__ == "__main__":
    sys.exit(main())
