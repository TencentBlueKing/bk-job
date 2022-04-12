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
import os, sys
from jinja2 import Environment, FileSystemLoader
import codecs


def render_file_in_dir(var_dict, path, mode):
    load = FileSystemLoader(path)
    env = Environment(loader=load)
    file_list = os.listdir(path)
    for file_name in file_list:
        print("")
        template = env.get_template(file_name)
        rendered_content = template.render(var_dict)
        file_path = path + "/" + file_name
        f = codecs.open(file_path, "r", encoding="utf-8")
        content = f.read()
        print(file_path + " before rendered:")
        print(content)
        print(file_path + " after rendered:")
        print(rendered_content)
        if mode == "w":
            f = codecs.open(file_path, "w", encoding="utf-8")
            f.write(rendered_content)
            f.close()
            print("***" + file_path + " rendered!" + "***")


if __name__ == '__main__':
    if len(sys.argv) < 3:
        print("Usage:python renderTemplates.py {edition(ee/ce)} {mode(t:test/w:write)}")
        print("Example:python renderTemplates.py ee t")
        print("Example:python renderTemplates.py ce w")
        exit(1)
    edition = sys.argv[1]
    mode = sys.argv[2]
    print("edition=" + edition)
    print("mode=" + mode)
    var_dict = {
        "job_edition": edition
    }
    render_file_in_dir(var_dict, "../templates", mode)
