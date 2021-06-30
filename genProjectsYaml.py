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

import sys

moduleDefTpl = """- name: {module}
  module: job
  project_dir: job/{module}
  alias: {module}
  language: java/1.8
  version: {version}
  version_type: {edition}

"""


def main():
    print("Usage:python genProjectsYaml.py {edition} {version} {module1} {module2} ...")
    edition = sys.argv[1]
    version = sys.argv[2]
    modules = sys.argv[3:]
    print("edition=" + str(edition))
    print("version=" + str(version))
    print("modules=" + ",".join(modules))
    if len(modules) <= 0:
        print("No modules specified")
        return
    global moduleDefTpl
    moduleDefTpl = moduleDefTpl.replace("{edition}", edition)
    moduleDefTpl = moduleDefTpl.replace("{version}", version)
    f = open("projects.yaml", "w")
    for module in modules:
        moduleDef = moduleDefTpl.replace("{module}", module)
        f.write(moduleDef)
    f.close()
    print("done")


if __name__ == "__main__":
    main()
