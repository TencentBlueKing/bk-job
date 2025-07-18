# coding:utf-8
"""
Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.

Copyright (C) 2021 Tencent.  All rights reserved.

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
"""
功能说明
该脚本会在当前脚本所在目录下生成 README.md 文件，并输出缺少功能描述的文档路径。
"""
import os
import sys
import re

# 获取当前脚本的目录
script_dir = os.path.dirname(os.path.abspath(__file__))

language_info = {
    "en": {
        "docs_dir_path": "esb/jobv3-confapis/apidocs/en",
        "template_file_name": "README_en.tpl",
        "output_file_name": "README_en.md",
        "function_desc_title": "### Function Description",
        "resource_name_title": "Resource Name",
        "resource_description_title": "Resource Description"
    },
    "zh_hans": {
        "docs_dir_path": "esb/jobv3-confapis/apidocs/zh_hans",
        "template_file_name": "README.tpl",
        "output_file_name": "README.md",
        "function_desc_title": "### 功能描述",
        "resource_name_title": "资源名称",
        "resource_description_title": "资源描述"
    }
}

# 获取使用的分支
branch = "master"
if len(sys.argv) >= 2:
    branch = sys.argv[1].strip()

# 获取使用的语言
language = "zh_hans"
if len(sys.argv) >= 3:
    language = sys.argv[2].strip()

print("文档引用的代码分支为: %s" % branch)
# 常量定义
apidoc_dir = os.path.join(script_dir, "../../../docs/apidoc")
docs_dir_path = language_info[language]["docs_dir_path"]
docs_dir = os.path.join(apidoc_dir, docs_dir_path)
docs_link_prefix = "https://github.com/TencentBlueKing/bk-job/blob/" + branch + "/docs/apidoc/" + docs_dir_path + "/"
template_file = os.path.join(script_dir, language_info[language]["template_file_name"])
output_file_name = language_info[language]["output_file_name"]
output_file = os.path.join(apidoc_dir, output_file_name)

# 初始化结果列表
results = []

# 遍历目录下的所有文件
for root, dirs, files in os.walk(docs_dir):
    for file in files:
        if file.endswith(".md"):
            file_path = os.path.join(root, file)
            with open(file_path, 'r', encoding='utf-8') as f:
                lines = f.readlines()

            # 提取功能描述
            func_desc = None
            for i, line in enumerate(lines):
                if line.strip() == language_info[language]["function_desc_title"]:
                    # 查找功能描述内容，忽略空白行
                    for j in range(i + 1, len(lines)):
                        if lines[j].strip():
                            func_desc = lines[j].strip()
                            break
                    break

            if func_desc:
                # 去掉末尾的标点符号
                func_desc = re.sub(r'[。！？.,!?]$', '', func_desc)
                # 提取接口名称
                interface_name = os.path.splitext(file)[0]
                # 生成资源名称和资源描述
                resource_name = f"[{interface_name}]({docs_link_prefix}{file})"
                resource_desc = func_desc
                results.append(f"| {resource_name} | {resource_desc} |")
            else:
                print(f"功能描述缺失: {file_path}")

resource_name_title = language_info[language]["resource_name_title"]
resource_description_title = language_info[language]["resource_description_title"]
# 生成接口文档简介内容
api_summary = "| " + resource_name_title + " | " + resource_description_title + " |\n"
api_summary += "| -------- | -------- |\n"
for result in results:
    api_summary += result + "\n"

# 读取模板文件并替换占位符
with open(template_file, 'r', encoding='utf-8') as f:
    template_content = f.read()

output_content = template_content.replace("{{api_summary}}", api_summary)

# 写入最终的输出文件
with open(output_file, 'w', encoding='utf-8') as f:
    f.write(output_content)

print(f"接口文档简介已生成: {output_file}")
