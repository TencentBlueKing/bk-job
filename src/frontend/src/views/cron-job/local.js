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
    namespace: 'cron',
    message: {
        定时任务: 'Cron',
        新建: 'New',
        '搜索任务ID，任务名称，更新人...': 'Search by Cron Job ID / Cron Name / Last Modified By ...',
        任务ID: 'Cron ID',
        执行方案名称: 'JOB PLAN',
        更新时间: 'LAST MODIFIED ON',
        最新执行结果: 'LATEST RESULT',
        周期成功率: 'SUCCESS RATE',
        更多失败记录: 'More',
        操作: 'ACTIONS',
        编辑: 'Edit',
        '确定删除该定时任务？': 'Are you sure to delete it?',
        '删除后不可恢复，请谨慎操作！': 'It cannot be restored after deletion, please proceed with caution!',
        删除: 'Delete',
        编辑定时任务: 'Edit Cron',
        新建定时任务: 'New Cron',
        保存: 'Save',
        提交: 'Commit',
        开启成功: 'Cron has been opened.',
        关闭成功: 'Cron has been closed.',
        删除定时任务成功: 'Cron has been deleted.',
        '任务名称：': 'Name: ',
        '执行策略：': 'Repeat Frequency: ',
        单次执行: 'Run once',
        周期执行: 'Round-robin',
        '执行时间：': 'Time Set: ',
        '执行方案：': 'Job Plan: ',
        该执行方案无全局变量: 'The Plan has no variable related',
        '推荐按照该定时执行的实际场景来取名...': 'Name it as actual use scenario [Recommended]...',
        选择日期时间: 'Pick your time',
        作业模板: 'Job Template',
        选择作业模板: 'Select the Job Template you need...',
        选择执行方案: 'Select the Job Plan you need...',
        查看执行方案: 'View Plan Details',
        单次执行时间必填: 'Datetime is required',
        请输入正确时间表达式: 'The time set expression is invalid',
        任务名称必填: 'Cron name is required',
        '任务名称已存在，请重新输入': 'Name already exist, please try another one...',
        作业模板必填: 'Job Template is required',
        执行方案必填: 'Job Plan is required',
        定时任务编辑成功: '',
        '定时任务创建成功(默认关闭，请手动开启)': 'New Cron has been created (Default status: OFF)',
        '执行时间无效（早于当前时间）': 'Invalid execute time (earlier than now)',
        定时任务详情: 'Cron Details',
        执行方案: 'Job Plan',
        设置结束时间: 'End-time Settings',
        执行前通知: 'Notify before launch',
        结束前通知: 'Notify before End-Time',
        执行前: 'Before',
        通知方式: 'Notify by',
        通知对象: 'Notify to',
        输入通知对象: 'Type the username you want to notify...',
        分钟: 'Mins',
        小时: 'Hour',
        '结束时间：': 'End-time: ',
        '结束前通知：': 'Notify before End-Time: ',
        '通知对象：': 'Notify to: ',
        '通知方式：': 'Notify by: ',
        '执行前通知：': 'Notify before launch: ',
        结束前: 'Before',
        全部: 'All',
        '任务 ID': 'CRON ID',
        任务名称: {
            label: 'Cron Name',
            colHead: 'CRON NAME',
        },
        执行策略: {
            label: 'Repeat Frequency',
            colHead: 'REPEAT FREQUENCY',
        },
        创建人: 'CREATED BY',
        创建时间: 'CREATED ON',
        更新人: {
            label: 'Last Modify By',
            colHead: 'LAST MODIFIED BY',
        },
        任务状态: {
            label: 'Status',
            colHead: 'STATUS',
        },
        执行人: {
            label: 'Launched By',
            colHead: 'LAUNCHED BY',
        },
        开始时间: {
            label: 'Started',
            colHead: 'STARTED',
        },
        耗时时长: 'DURATION',
        至今: 'until Now',
        等待执行: 'Pending',
        正在执行: 'Running',
        执行成功: 'Successful',
        执行失败: 'Failed',
        等待确认: 'Waiting',
        强制终止中: 'Terminating',
        强制终止成功: 'Terminated',
        强制终止失败: 'Terminate failed',
        确认终止: 'Termination Confirmed',
        '搜索指定任务ID 或 根据字段筛选结果': 'Search by ID or specified field...',
        '搜索条件带任务ID时，将自动忽略其他条件': 'When condition is task ID, other conditions will be ignored',
        执行记录: 'Records',
        近1小时: 'Last 1 hour',
        近12小时: 'Last 12 hours',
        近1天: 'Last 24 hours',
        近7天: 'Last 7 days',
        执行方案ID: 'PLAN ID',
        定时执行记录: 'Records of execute history',
        将覆盖其它条件: 'override other conditions',
        设置的提醒时间已过期: 'Notify Time expired',
        ID只支持数字: 'ID is only allow numbers',
        '「周期成功率」采样规则和计算公式': '"Success Rate" sampling rule & calculation formula',
        '采样规则：': 'Sampling Rule :',
        '近 24小时执行次数 ＞10，则 “分母” 为近 24 小时执行总数': 'If total executions in last 24hrs ＞10, then "Denominator" is the total of last 24hrs',
        '近 24小时执行次数 ≤ 10，则 “分母” 为近 10 次执行任务': 'If total executions in last 24hrs ＜10, then "Denominator" is the last 10 records',
        '计算公式：': 'Calculation Formula :',
        '成功次数(分子) / 分母 * 100 = 周期成功率（%）': 'Number of successes (Numerator) / Denominator * 100 = Success Rate (%)',
        分: 'Minute',
        时: 'Hour',
        日: 'Day',
        月: 'Month',
        周: 'Week',
        '下次：': 'Next：',
        唤起时间: 'LAUNCH ON',
        原因: 'FAILED MESSAGE',
        任务正常启动: 'Launched',
        任务未能启动: 'Unlaunched',
    },
};
