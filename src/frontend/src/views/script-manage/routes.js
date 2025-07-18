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

import Entry from '@views/script-manage/index.vue';

import I18n from '@/i18n';

export default {
  path: 'script_manage',
  name: 'scriptManage',
  component: Entry,
  redirect: {
    name: 'scriptList',
  },
  meta: {
    title: I18n.t('脚本管理'),
    group: 'business',
  },
  children: [
    {
      path: 'list',
      name: 'scriptList',
      component: () => import('@views/script-manage/list/'),
      meta: {
        pageTitle: I18n.t('脚本管理'),
        skeleton: 'taskList',
        full: true,
      },
    },
    {
      path: 'create',
      name: 'createScript',
      component: () => import('@views/script-manage/create/'),
      meta: {
        title: I18n.t('新建脚本'),
      },
    },
    {
      path: 'version/:id',
      name: 'scriptVersion',
      component: () => import('@views/script-manage/version'),
      meta: {
        title: I18n.t('版本管理'),
        skeleton: 'scriptVersion',
      },
    },
    {
      path: 'sync/:scriptId/:scriptVersionId',
      name: 'scriptSync',
      component: () => import('@views/script-manage/sync-confirm'),
      meta: {
        title: I18n.t('同步确认'),
      },
    },
    {
      path: 'sync_task/:scriptId/:scriptVersionId',
      name: 'scriptSyncTask',
      component: () => import('@views/script-manage/sync-task'),
      meta: {
        title: I18n.t('同步任务'),
      },
    },
  ],
};
