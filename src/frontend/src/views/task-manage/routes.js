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

export default {
    path: 'task_manage',
    name: 'taskManage',
    component: () => import('@views/task-manage'),
    redirect: {
        name: 'taskList',
    },
    meta: {
        title: I18n.t('作业'),
        group: 'business',
    },
    children: [
        {
            path: 'list',
            name: 'taskList',
            component: () => import('@views/task-manage/list'),
            meta: {
                pageTitle: I18n.t('作业'),
                full: true,
                skeleton: 'taskList',
            },
        },
        {
            path: 'detail/:id',
            name: 'templateDetail',
            component: () => import('@views/task-manage/template-detail'),
            meta: {
                title: I18n.t('查看作业模板'),
                skeleton: 'taskDetail',
            },
        },
        {
            path: 'create',
            name: 'templateCreate',
            component: () => import('@views/task-manage/template-operation'),
            meta: {
                title: I18n.t('新建作业模板'),
                full: true,
            },
        },
        {
            path: 'import',
            name: 'taskImport',
            component: () => import('@views/task-manage/import'),
            meta: {
                title: I18n.t('导入作业.nav'),
                full: true,
            },
        },
        {
            path: 'export',
            name: 'taskExport',
            component: () => import('@views/task-manage/export'),
            meta: {
                title: I18n.t('导出作业'),
                full: true,
            },
        },
        {
            // stepId 默认展示指定步骤
            path: 'edit/:id/:stepId?',
            name: 'templateEdit',
            component: () => import('@views/task-manage/template-operation'),
            meta: {
                title: I18n.t('编辑作业模板'),
                full: true,
                skeleton: 'taskDetail',
            },
        },
        {
            path: 'clone/:id',
            name: 'templateClone',
            component: () => import('@views/task-manage/template-operation'),
            meta: {
                title: I18n.t('克隆作业模板'),
                skeleton: 'taskDetail',
            },
        },
        {
            path: 'view_plan/:templateId',
            name: 'viewPlan',
            component: () => import('@views/task-manage/template-plan-list'),
            meta: {
                title: I18n.t('查看执行方案'),
                skeleton: 'list',
            },
        },
        {
            path: 'sync_plan/:templateId/:id',
            name: 'syncPlan',
            component: () => import('@views/task-manage/sync-plan'),
            meta: {
                title: I18n.t('同步确认'),
                full: true,
            },
        },
        {
            path: 'debug_plan/:id',
            name: 'debugPlan',
            component: () => import('@views/task-manage/debug-plan/index'),
            meta: {
                title: I18n.t('调试'),
                skeleton: 'setVariable',
            },
        },
        {
            path: 'setting_variable/:templateId/:id/:debug?',
            name: 'settingVariable',
            component: () => import('@views/task-manage/setting-variable'),
            meta: {
                title: I18n.t('设置全局变量'),
                skeleton: 'setVariable',
            },
        },
    ],
};
