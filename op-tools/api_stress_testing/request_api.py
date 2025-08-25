#  Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
#
#  Copyright (C) 2021 Tencent.  All rights reserved.
#
#  BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
#
#  License for BK-JOB蓝鲸智云作业平台:
#  --------------------------------------------------------------------
#  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
#  documentation files (the "Software"), to deal in the Software without restriction, including without limitation
#  the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
#  to permit persons to whom the Software is furnished to do so, subject to the following conditions:
#
#  The above copyright notice and this permission notice shall be included in all copies or substantial portions of
#  the Software.
#
#  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
#  THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
#  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
#  CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
#  IN THE SOFTWARE.

"""
Script Name: request_api.py

Description:
用于对单接口进行压力测试，采用多进程+协程并发请求API

Python Version:
- Python 3.6

Dependencies:
以下列出的依赖库以及其推荐版本号。
由于不同环境可能版本不一，建议使用下述版本：
- argparse-manpage==4.5
- aiohttp==3.8.6

"""


import sys
import time
import asyncio
import aiohttp
import argparse
from multiprocessing import Process, Manager

url = 'example.com'
auth_header = ''
headers = {"X-Bkapi-Authorization": auth_header}
data = {}


async def request_api(session, url, seq, times, errors, request_time_list):
    start_time = time.time()
    # 请求开始时间
    request_time_list.append(start_time * 1000)
    try:
        async with session.get(url, headers=headers, data=data) as response:
            used_time = (time.time() - start_time) * 1000
            times.append(used_time)
            # 请求失败记录错误数
            if not (200 <= response.status < 300):
                errors.append(seq)
    except Exception as e:
        errors.append(seq)


async def run_concurrent_requests(concurrent_cnt, times, errors, request_time_list):
    async with aiohttp.ClientSession() as session:
        tasks = [request_api(session, url, seq, times, errors, request_time_list) for seq in range(concurrent_cnt)]
        await asyncio.gather(*tasks)


def worker(concurrent_cnt, times, errors, request_time_list):
    loop = asyncio.get_event_loop()
    try:
        loop.run_until_complete(run_concurrent_requests(concurrent_cnt, times, errors, request_time_list))
    finally:
        loop.close()


def generate_report(nums, times, errors, request_time_list):
    """
    生成压测报告。

    参数：
    nums (int): 总请求数。
    times (list of float): 每个请求的耗时列表。
    errors (list of int): 记录错误请求序号的列表。
    request_time_list (list of float): 每个请求发起的时间戳列表。
    """
    num_errors = len(errors)
    min_time = min(times) if times else 0
    max_time = max(times) if times else 0
    avg_time = sum(times) / nums if times else 0
    error_rate = (num_errors / nums) * 100
    time_spent_sending_requests = (max(request_time_list) - min(request_time_list))

    print(f"Total requests: {nums}")
    print(f"Minimum response time: {min_time:.2f} ms")
    print(f"Maximum response time: {max_time:.2f} ms")
    print(f"Average response time: {avg_time:.2f} ms")
    print(f"Error rate: {error_rate:.2f}%")
    print(f"Time spent sending requests: {time_spent_sending_requests:.2f} ms")
    print("BK_CI_SETENV:avg_time=" + f"{avg_time:.2f}" + "BK_CI_SETENV_END")
    print("BK_CI_SETENV:max_time=" + f"{max_time:.2f}" + "BK_CI_SETENV_END")
    print("BK_CI_SETENV:min_time=" + f"{min_time:.2f}" + "BK_CI_SETENV_END")
    print("BK_CI_SETENV:error_rate=" + str(error_rate) + "BK_CI_SETENV_END")
    print("BK_CI_SETENV:time_spent_sending_requests=" + f"{time_spent_sending_requests:.2f}" + "BK_CI_SETENV_END")


if __name__ == '__main__':

    parser = argparse.ArgumentParser()
    parser.add_argument("--app_code", type=str)
    parser.add_argument("--app_secret", type=str)
    parser.add_argument("--username", type=str)
    parser.add_argument("--url", type=str)
    parser.add_argument("--concurrent_cnt", type=str)
    parser.add_argument("--process_cnt", type=str)
    args = parser.parse_args()

    # 初始化参数
    if args.app_code is not None:
        app_code = args.app_code
    else:
        print("app_code cannot be None")
        sys.exit(1)

    if args.app_secret is not None:
        app_secret = args.app_secret
    else:
        print("app_secret cannot be None")
        sys.exit(1)

    if args.username is not None:
        username = args.username
    else:
        print("username cannot be None")
        sys.exit(1)

    if args.url is not None:
        api_url = args.url
        url = api_url
    else:
        print("url cannot be None")
        sys.exit(1)

    if args.concurrent_cnt is not None:
        concurrent_cnt = args.concurrent_cnt
    else:
        concurrent_cnt = 10

    if args.process_cnt is not None:
        process_cnt = args.process_cnt
    else:
        process_cnt = 1

    auth_header = '{{"bk_app_code": "{}", "bk_app_secret": "{}", "bk_username": "{}"}}'.format(app_code, app_secret, username)
    headers = {"X-Bkapi-Authorization": auth_header}

    manager = Manager()
    # 响应耗时
    times = manager.list()
    # 错误数
    errors = manager.list()
    # 请求开始时间
    request_time_list = manager.list()

    processes = []
    number_of_processes = int(process_cnt)
    concurrent_cnt_per_process = int(concurrent_cnt)

    for _ in range(number_of_processes):
        p = Process(target=worker, args=(concurrent_cnt_per_process, times, errors, request_time_list))
        processes.append(p)
        p.start()

    for p in processes:
        p.join()

    # 生成压测报告
    generate_report(
        nums = number_of_processes * concurrent_cnt_per_process,
        times = times,
        errors = errors,
        request_time_list = request_time_list
    )

