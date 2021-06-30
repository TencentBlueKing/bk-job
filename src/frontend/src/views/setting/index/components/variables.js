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

import I18n from '@/i18n';

export const InternalVariables = {
    general: [
        {
            name: '{{ task.id }}',
            meaning: I18n.t('setting.任务的ID'),
            examples: '127',
        },
        {
            name: '{{ task.name }}',
            meaning: I18n.t('setting.任务的名称'),
            examples: I18n.t('setting.获取所有主机的设备型号'),
        },
        {
            name: '{{ task.bk_biz_id }}',
            meaning: I18n.t('setting.任务所属业务的 ID'),
            examples: '100147',
        },
        {
            name: '{{ task.bk_biz_name }}',
            meaning: I18n.t('setting.任务所属业务的名称'),
            examples: I18n.t('setting.王者荣耀'),
        },
        {
            name: '{{ task.operator }}',
            meaning: I18n.t('setting.任务的操作人'),
            examples: 'admin',
        },
        {
            name: '{{ task.start_time }}',
            meaning: I18n.t('setting.任务开始时间'),
            examples: '2020-05-10 20:32:18 (+0800)',
        },
        {
            name: '{{ task.type }}',
            meaning: I18n.t('setting.任务的类型'),
            examples: I18n.t('setting.快速执行脚本 / 作业任务 / 定时任务 / ...'),
        },
        {
            name: '{{ task.url }}',
            meaning: I18n.t('setting.任务的详情链接'),
            examples: 'https://cloud-job.blueking.com/127/crons/...',
        },
    ],
    job: [
        {
            name: '{{ task.step.name }}',
            meaning: I18n.t('setting.任务当前步骤的名称'),
            examples: I18n.t('setting.这是检查配置文件语法的步骤'),
        },
        {
            name: '{{ task.step.type }}',
            meaning: I18n.t('setting.任务当前步骤的类型'),
            examples: I18n.t('setting.脚本执行 or 文件分发 or 人工确认'),
        },
        {
            name: '{{ task.step.total_seq_cnt }}',
            meaning: I18n.t('setting.任务的步骤总数'),
            examples: '12',
        },
        {
            name: '{{ task.step.current_seq_id }}',
            meaning: I18n.t('setting.任务当前步骤的序号'),
            examples: '7',
        },
        {
            name: '{{ task.step.duration }}',
            meaning: I18n.t('setting.任务当前步骤执行耗时'),
            examples: '12.375',
        },
        {
            name: '{{ task.total_duration }}',
            meaning: I18n.t('setting.任务执行总耗时'),
            examples: '65.831',
        },
        {
            name: '{{ task.step.failed_cnt }}',
            meaning: I18n.t('setting.任务当前步骤执行失败数'),
            examples: '83',
        },
        {
            name: '{{ task.step.success_cnt }}',
            meaning: I18n.t('setting.任务当前步骤执行成功数'),
            examples: '146',
        },
        {
            name: '{{ task.step.confirm_info }}',
            meaning: I18n.t('setting.任务的人工确认描述'),
            examples: I18n.t('setting.请确认以上执行步骤是否执行正常'),
        },
        {
            name: '{{ task.step.confirmer }}',
            meaning: I18n.t('setting.任务的确认步骤的干系人'),
            examples: 'admin',
        },
    ],
    cron: [
        {
            name: '{{ task.cron.plan_name }}',
            meaning: I18n.t('setting.定时关联的执行方案名'),
            examples: I18n.t('setting.这是一个测试的作业执行方案'),
        },
        {
            name: '{{ task.cron.plan_id }}',
            meaning: I18n.t('setting.定时关联的执行方案ID'),
            examples: '10000058',
        },
        {
            name: '{{ task.cron.repeat_freq }}',
            meaning: I18n.t('setting.定时任务的执行策略'),
            examples: I18n.t('setting.单次执行 or 周期执行'),
        },
        {
            name: '{{ task.cron.time_set }}',
            meaning: I18n.t('setting.定时任务的执行时间'),
            examples: '* * * * * or 2020-05-12 23:31:00',
        },
        {
            name: '{{ task.cron.notify_time }}',
            meaning: I18n.t('setting.定时任务提前通知时间'),
            examples: '30',
        },
    ],
};
