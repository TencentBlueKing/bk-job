/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 *
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
*/

/* eslint-disable max-len */

export default {
    namespace: 'detectRecords',
    message: {
        '搜索拦截ID，表达式，业务，执行人，执行方式，调用方，动作…': '',
        拦截ID: 'ID',
        表达式: {
            label: 'GRAMMAR REGEX',
            colHead: 'GRAMMAR REGEX',
        },
        业务: {
            label: 'APP',
            colHead: 'APP',
        },
        执行人: {
            label: 'Launched By',
            colHead: 'LAUNCHED BY',
        },
        执行时间: 'EXECUTE TIME',
        执行方式: {
            label: 'Source',
            colHead: 'SOURCE',
        },
        调用方: {
            label: 'Client',
            colHead: 'CLIENT',
        },
        动作: {
            label: 'Action',
            colHead: 'ACTION',
        },
        脚本语言: {
            label: 'Script Language',
            colHead: 'SCRIPT LANGUAGE',
        },
        操作: 'ACTIONS',
        选择日期: 'Select datetime',
        近1小时: 'Last 1 hour',
        近12小时: 'Last 12 hours',
        近1天: 'Last 24 hours',
        近7天: 'Last 7 days',
        至今: 'until Now',
        定时执行: 'Crons',
        API调用: 'API',
        页面执行: 'Web UI',
        扫描: 'Scan',
        拦截: 'Block',
        查看脚本: 'Script Details',
        脚本内容: 'Contents',
        '【扫描】': 'SCAN',
        '【拦截】': 'BLOCK',
        '命中规则的脚本执行任务仅会做记录，不会拦截': 'The script execution task that hits the rule will only be recorded and not be blocked',
        '命中规则的脚本执行任务会被记录，并中止运行': 'The script execution task that hits the rule will be recorded and blocked',
    },
};
