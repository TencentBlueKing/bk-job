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

VERSION_LOG_PATH = os.environ.get("VERSION_LOG_PATH", os.getcwd())

# data元素格式
'''
{
    "version": "V1.0.0.0",
    "time": "2020-03-29",
    "content": "### 【V1.0.0.0】版本更新明细\n#### 【新增】一个功能.\n"
}
'''
languageDataMap = {}
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
        fileName = path[i + 1:]
        separaterIndex = fileName.find("_")
        versionName = ""
        date = ""
        if separaterIndex == -1:
            versionName = fileName[0:len(fileName) - 3]
            date = "latest"
        else:
            versionName = fileName[0:separaterIndex]
            date = fileName[separaterIndex + 1: len(fileName) - 3]
        f = codecs.open(path, "r", encoding="UTF-8")
        content = f.read()
        f.close()
        data.append({"version": versionName, "time": date, "content": content})
        print("add to versionLog:", path)
    else:
        print("not md file, ignore:", path)


def searchTree(data, path):
    l = os.listdir(path)
    for name in l:
        wholePath = os.path.join(path, name)
        if os.path.isdir(wholePath):
            searchTree(data, wholePath)
        else:
            process(data, wholePath)
    return data


def writeOneLanguageVersionLog(language, versionLogData, fileName):
    # 时间降序排列
    versionLogData.sort(key=(lambda x: x["time"]), reverse=True)
    resp["data"] = versionLogData
    bundledFile = codecs.open(os.path.join(VERSION_LOG_PATH, fileName), "w", encoding="UTF-8")
    bundledFile.write(json.dumps(resp))
    bundledFile.close()


files = os.listdir('.')
dirs = []
for file in files:
    if os.path.isdir(file):
        dirs.append(file)

for dir in dirs:
    language = dir
    versionLogData = searchTree([], os.path.join(VERSION_LOG_PATH, language))
    writeOneLanguageVersionLog(dir, versionLogData, "bundledVersionLog_" + language + ".json")
    # 生成默认语言文件
    if language.lower().replace("_", "").replace("-", "") == DEFAULT_LANGUAGE.lower().replace("_", "").replace("-", ""):
        writeOneLanguageVersionLog(dir, versionLogData, "bundledVersionLog.json")
