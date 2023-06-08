# coding:utf-8
"""
Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.

Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.

BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.

License for BK-JOB蓝鲸智云作业平台:
--------------------------------------------------------------------
Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the Software without restriction, including without limitation
the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
to permit persons to whom the Software is furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of
the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
IN THE SOFTWARE.
"""

import codecs
import json
import os
import functools

VERSION_LOG_PATH = os.environ.get("VERSION_LOG_PATH", os.getcwd())

# data元素格式
'''
{
    "version": "V1.0.0.0",
    "time": "2020-03-29",
    "content": "### 【V1.0.0.0】版本更新明细\n#### 【新增】一个功能.\n"
}
'''
language_data_map = {}
resp = {
    "success": True,
    "code": 0,
    "errorMsg": None,
    "data": [],
    "requestId": None
}
DEFAULT_LANGUAGE = "zh_CN"


def process(data, path):
    if path.endswith(".md"):
        i = path.rindex(os.sep)
        file_name = path[i + 1:]
        separater_index = file_name.find("_")
        if separater_index == -1:
            version_name = file_name[0:len(file_name) - 3]
            date = "latest"
        else:
            version_name = file_name[0:separater_index]
            date = file_name[separater_index + 1: len(file_name) - 3]
        f = codecs.open(path, "r", encoding="UTF-8")
        content = f.read()
        f.close()
        data.append({"version": version_name, "time": date, "content": content})
        print("add to versionLog:", path)
    else:
        print("not md file, ignore:", path)


def search_tree(data, path):
    dir_names = os.listdir(path)
    for name in dir_names:
        whole_path = os.path.join(path, name)
        if os.path.isdir(whole_path):
            search_tree(data, whole_path)
        else:
            process(data, whole_path)
    return data


def compare_version(version_log_1, version_log_2):
    version_1 = version_log_1["version"].lower().lstrip("v")
    version_2 = version_log_2["version"].lower().lstrip("v")
    version1_arr = version_1.split(".")
    version2_arr = version_2.split(".")
    version1_arr_len = len(version1_arr)
    version2_arr_len = len(version2_arr)
    min_length = min(version1_arr_len, version2_arr_len)
    for i in range(min_length):
        version1_num = int(version1_arr[i])
        version2_num = int(version2_arr[i])
        if version1_num != version2_num:
            return version1_num - version2_num
    if version1_arr_len > min_length:
        return 1
    if version2_arr_len > min_length:
        return -1
    return 0


def write_one_language_version_log(version_log_data, file_name):
    # 版本降序排列
    version_log_data.sort(key=functools.cmp_to_key(compare_version), reverse=True)
    resp["data"] = version_log_data
    bundled_file = codecs.open(os.path.join(VERSION_LOG_PATH, file_name), "w", encoding="UTF-8")
    bundled_file.write(json.dumps(resp))
    bundled_file.close()


def run():
    files = os.listdir('.')
    dirs = []
    for file in files:
        if os.path.isdir(file):
            dirs.append(file)

    for _dir in dirs:
        language = _dir
        version_log_data = search_tree([], os.path.join(VERSION_LOG_PATH, language))
        write_one_language_version_log(version_log_data, "bundledVersionLog_" + language + ".json")
        # 生成默认语言文件
        if language.lower().replace("_", "").replace("-", "") == DEFAULT_LANGUAGE.lower() \
                .replace("_", "").replace("-", ""):
            write_one_language_version_log(version_log_data, "bundledVersionLog.json")


if __name__ == "__main__":
    run()
