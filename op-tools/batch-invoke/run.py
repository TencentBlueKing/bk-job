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
import glob
import json
import os
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime as dt

encoding = "UTF-8"


def read_file_lines_as_arr(file_path):
    f = codecs.open(file_path, "r", encoding)
    lines = f.readlines()
    f.close()
    lines = (list)(map(lambda x: x.strip(), lines))
    lines = (list)(filter(lambda x: x != "", lines))
    return lines


def write_to_json_file(obj, file_path):
    f = codecs.open(file_path, "w", encoding)
    json.dump(obj, f, ensure_ascii=False)
    f.close()


def execute_cmd(cmd):
    proc = subprocess.Popen(
        cmd,
        shell=True,
        stdout=subprocess.PIPE,
        bufsize=-1
    )
    return proc.stdout.read()


def execute_cmd_and_get_json_result(number, cmd):
    output = {"cmd": cmd}
    result = execute_cmd(cmd)
    output["result"] = result.decode(encoding)
    print(str(number) + " " + "=" * 50)
    print("execute cmd: %s" % cmd)
    print("result:")
    try:
        print(result.decode(encoding))
    except:
        try:
            print(result.decode("ASCII"))
        except:
            print(result)
    return [number, output]


def main():
    while True:
        plan = input("请输入要执行的脚本名称(例如execute.sh，输入exit退出):")
        if plan == "exit":
            break
        elif os.path.exists(plan):
            initial_cmd = "./" + plan
        else:
            print("输入的脚本不存在，请重新输入")
            continue
        # 并发数
        concurrency = 1
        if len(sys.argv) > 1:
            try:
                concurrency = int(sys.argv[1])
            except Exception:
                print("Invalid concurrency:%s, which should be a number" % sys.argv[1])
                exit(1)
        dirname, filename = os.path.split(os.path.abspath(sys.argv[0]))
        print("py file: %s/%s" % (dirname, filename))
        print("work dir:%s" % dirname)
        os.chdir(dirname)
        print("begin to batch run with concurrency: %d" % concurrency)
        # shell脚本dos2unix
        cmd = "dos2unix " + initial_cmd
        execute_cmd(cmd)
        # 为脚本添加执行权限
        cmd = "chmod +x " + initial_cmd
        execute_cmd(cmd)

        # 数据文件检测与排序
        data_path = plan.rstrip(".sh") + "_data_*.txt"
        data_file_list = glob.glob(data_path)
        data_file_list.sort(key=lambda x: int(x.lstrip(plan.rstrip(".sh")+"_data_").rstrip(".txt")))
        data_file_num = len(data_file_list)
        print("%d data file detected:" % data_file_num)
        for data_file in data_file_list:
            print(data_file)
        print("")

        # 数据文件读取与一致性检测
        all_data_lines = []
        line_num = None
        for data_file in data_file_list:
            lines = read_file_lines_as_arr(data_file)
            all_data_lines.append(lines)
            current_line_num = len(lines)
            if line_num == current_line_num:
                continue
            elif line_num is not None and line_num != current_line_num:
                print("lines num not fit, %d lines in former data files, %d lines in %s" % (
                    line_num, current_line_num, data_file))
                exit(1)
            else:
                line_num = current_line_num
        if line_num is None:
            print("No lines in data file, please check ")
            exit(1)
        # 并发执行

        output_arr = []
        future_list = []
        pool = ThreadPoolExecutor(max_workers=concurrency)
        start_time = dt.now()
        for i in range(line_num):
            cmd = initial_cmd
            for data_lines in all_data_lines:
                cmd = cmd + " " + data_lines[i]
            future = pool.submit(execute_cmd_and_get_json_result, i + 1, cmd)
            future_list.append(future)
        # 收集执行结果
        for future in future_list:
            output = future.result()
            output_arr.append(output)
        end_time = dt.now()
        print("time consuming: %s" % (end_time - start_time))
        # 按原始数据顺序排序
        output_arr.sort(key=lambda x: x[0])
        output_arr = (list)(map(lambda x: x[1], output_arr))
        # 保存结果数据
        write_to_json_file(output_arr, plan.rstrip(".sh")+"_output.json")


if __name__ == "__main__":
    main()
