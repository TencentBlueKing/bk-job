/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

import Entry from '@views/executive-history/index.vue';

import I18n from '@/i18n';

export default {
  path: 'execute',
  name: 'executiveHistory',
  component: Entry,
  redirect: {
    name: 'historyList',
  },
  meta: {
    title: I18n.t('执行历史'),
    group: 'business',
  },
  children: [
    {
      path: 'list',
      name: 'historyList',
      component: () => import('@views/executive-history/list'),
      meta: {
        pageTitle: I18n.t('执行历史'),
        skeleton: 'list',
      },
    },
    {
      path: 'task/:id',
      name: 'historyTask',
      component: () => import('@views/executive-history/task-detail'),
      meta: {
        title: I18n.t('作业执行详情'),
        skeleton: 'taskExecutiveDetail',
      },
    },
    {
      path: 'quick-launch/:taskInstanceId',
      name: 'quickLaunchStep',
      component: () => import('@views/executive-history/step-detail'),
      meta: {
        title: I18n.t('快速执行详情'),
        full: true,
        skeleton: 'taskStepExecutiveDetail',
      },
    },
    {
      path: 'step/:taskInstanceId',
      name: 'historyStep',
      component: () => import('@views/executive-history/step-detail'),
      meta: {
        title: I18n.t('步骤执行详情'),
        full: true,
        skeleton: 'taskStepExecutiveDetail',
      },
    },
    {
      path: 'redo_task/:taskInstanceId',
      name: 'redoTask',
      component: () => import('@views/executive-history/redo-task'),
      meta: {
        title: I18n.t('重做执行方案'),
        skeleton: 'setVariable',
      },
    },
  ],
};
