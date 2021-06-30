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
    namespace: 'dashboard',
    message: {
        数据初始时间: 'Data From',
        最近更新时间: 'Last Update',
        业务类: 'BUSINESS',
        资源类: 'RESOURCE',
        执行类: 'TASK',
        'Linux 账号数': 'Linux Account',
        'Windows 账号数': 'Windows Account',
        'DB 账号数': 'DB Account',
        活跃业务量: 'Active',
        '3 天内至少执行过一次任务的业务（定时任务除外）': 'At least one execution record in last 3 days(except Cron)',
        查看趋势图: 'Trend',
        查看列表: 'List',
        活跃业务量趋势图: 'Trend of Active Business',
        活跃业务量列表: 'List of Active Business',
        业务名: 'BUSINESS NAME',
        '在 JOB有任意执行过一次任务记录的业务': 'At least one execution record on JOB since it created',
        接入业务量趋势图: 'Trend of Total Business',
        接入业务量: 'Total',
        接入业务量列表: 'List of Total Business',
        同比: 'Lswk',
        环比: 'Yst',
        选择日期: 'Datetime',
        定时任务开关分布: 'ON / OFF Ratio of CRON',
        开启: 'On',
        关闭: 'Off',
        定时任务类型分布: 'Repeat Frequency Type of CRON',
        单次执行: 'Run once',
        周期执行: 'Round-robin',
        累计执行失败次数: 'Total of execution failure',
        累计执行失败次数趋势图: 'Trend of execution failure',
        累计执行失败次数列表: 'List of execution failure',
        失败次数: 'COUNT',
        占比: 'RATIO',
        按渠道统计: 'By Channel',
        按类型统计: 'By Type',
        按执行耗时统计: 'By duration',
        返回: 'Back',
        类型统计: ' ',
        '7 天内': 'in 7 days',
        '14 天内': 'in 14 days',
        '30 天内': 'in 30 days',
        页面执行: 'Web',
        'API 调用': 'API',
        定时执行: 'Cron',
        执行数: 'Number of Execution',
        执行失败次数: 'Number of Execution Failure',
        快速执行脚本: 'Script Execution(Quick Launch)',
        快速分发文件: 'File Transfer(Quick Launch)',
        作业执行: 'Job Plan Execution',
        '≥ 10分钟': '≥ 10mins',
        '1~10分钟以内（包含10分钟）': '1~10mins',
        '1分钟以内（包含1分钟）': '≤ 1min',
        强制模式: 'Force Mode',
        严谨模式: 'Stict Mode',
        保险模式: 'Safe Mode',
        累计任务执行次数: 'Total of Execution',
        累计任务执行次数趋势图: 'Trend of Execution',
        累计任务执行次数列表: 'List of Execution',
        脚本量: 'Scripts',
        脚本量趋势图: 'Trend of Scripts',
        脚本量列表: 'List of Scripts',
        使用率: 'Usage',
        '被作业模板引用的脚本总数（去重）/ 脚本总数，比率越高代表脚本在作业的使用率越高': 'Total number of scripts referenced by the job template (de-duplication) / Total number of scripts, the higher the ratio, the higher the usage of the script',
        脚本类型分布: 'Script Type',
        脚本版本状态分布: 'Script Version',
        已上线: 'Online',
        禁用: 'Banned',
        已下线: 'Offline',
        未上线: 'Stand-by',
        复用率: 'Re-use',
        '引用脚本的步骤总数 / 被引用的脚本总数（去重），比率越高代表脚本在作业中被重复利用的价值越大': 'The total number of steps of the quoted script / the total number of quoted scripts(de-duplication), the higher the ratio, the greater value of the script being reused',
        标签: 'Tags',
        执行方案量: 'Job Plans',
        执行方案量趋势图: 'Trend of Job Plans',
        执行方案量列表: 'List of Job Plans',
        作业模版量: 'Job Templates',
        作业模版量趋势图: 'Trend of Job Templates',
        作业模版量列表: 'List of Job Templates',
        作业步骤类型使用占比: 'Usage of Job step type',
        文件分发: 'File Transfer',
        本地文件源: 'Local File',
        服务器文件源: 'Remote File',
        脚本执行: 'Script Execution',
        手工录入: 'Custom',
        脚本引用: 'Quoted',
        人工确认: 'Confirmation',
        作业步骤: 'Job Step',
        执行次数: 'Number of Execution',
        今天: 'Today',
        昨天: 'Yesterday',
        最近3天: 'Last 3 days',
        最近7天: 'Last 7 days',
        最近30天: 'Last 30 days',
    },
};
