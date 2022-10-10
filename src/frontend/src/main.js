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

import Vue from 'vue';

import AppManageService from '@service/app-manage';
import QueryGlobalSettingService from '@service/query-global-setting';
import TaskExecuteService from '@service/task-execute';
import TaskPlanService from '@service/task-plan';

import '@/common/bkmagic';
import '@/css/reset.css';
import '@/css/app.css';
import '@bk-icon/style.css';
import '@bk-icon/iconcool.js';
import App from '@/App';
import i18n from '@/i18n';
import IframeApp from '@/iframe-app';
import createRouter from '@/router';
import store from '@/store';
import { scopeCache } from '@/utils/cache-helper';
import EntryTask from '@/utils/entry-task';

/**
 * @desc 启动打印当前系统信息
 */
console.log(
    process.env.JOB_WELCOME,
    'font-weight: 900; color: #3a84ff',
    'font-weight: 900; color: #2DCB8D;',
);

/**
 * @desc 页面数据的编辑状态
 */
window.changeConfirm = false;

/**
 * @desc 因为 IP 有白名单功能，生效范围需要更新场景区分
 * - '' 所有
 * - SCRIPT_EXECUTE 生效范围脚本执行
 * - FILE_DISTRIBUTION 生效范围文件分发
 */
window.IPInputScope = '';
/**
 * @desc 开启路由回溯
 */
window.routerFlashBack = false;

/**
 * fix: 兼容业务集功能上线前的任务执行详情 URL
 *
 * 老版 URL 格式： /${APP_ID}/execute/step/${TASK_ID}，
 * 解析 TASK_ID 拼接 api_execute/TASK_ID 跳转
 */
const oldExecute = window.location.pathname.match(/^\/\d+\/execute\/step\/(\d+)/);
if (oldExecute) {
    window.location.href = `/api_execute/${oldExecute[1]}`;
}

/**
 * @desc 浏览器框口关闭提醒
 */
window.addEventListener('beforeunload', (event) => {
    // 需要做 Boolean 类型的值判断
    if (window.changeConfirm !== true) {
        return null;
    }
    const e = event || window.event;
    if (e) {
        e.returnValue = window.BKApp.$t('离开将会导致未保存信息丢失');
    }
    return window.BKApp.$t('离开将会导致未保存信息丢失');
});

const entryTask = new EntryTask();

/**
 * @desc 根据环境动态判断使用那个入口
 *
 * 通过浏览器直接访问：App
 * 通过 iframe 访问任务详情：IframeApp
 */
let EntryApp = App;

/**
 * @desc 解析路由 scopeType、scopeId
 */
entryTask.add((context) => {
    const pathRoot = window.location.pathname.match(/^\/([^/]+)\/(\d+)\/?/);

    if (pathRoot) {
        // 路由指定了业务id
        [,
            context.scopeType,
            context.scopeId,
        ] = pathRoot;
    } else {
        // 本地缓存
        const {
            scopeType,
            scopeId,
        } = scopeCache.getItem();
        if (scopeType && scopeId) {
            context.scopeType = scopeType;
            context.scopeId = scopeId;
        }
    }
});
/**
 * @desc 完整的业务列表
 */
entryTask.add(context => AppManageService.fetchWholeAppList().then((data) => {
    context.appList = data.data;
    if (!context.scopeType || !context.scopeId) {
        const [
            {
                scopeType,
                scopeId,
            },
        ] = data.data;
        context.scopeType = scopeType;
        context.scopeId = scopeId;
    }
}));
/**
 * @desc 是否是admin用户
 */
entryTask.add(context => QueryGlobalSettingService.fetchAdminIdentity().then((data) => {
    // eslint-disable-next-line no-param-reassign
    context.isAdmin = data;
}));
/**
 * @desc 通过第三方系统查看任务执行详情
 */
const apiExecute = window.location.href.match(/api_execute\/([^/]+)/);
if (apiExecute) {
    // 通过 iframe 访问任务详情入口为 IframeApp
    if (window.frames.length !== parent.frames.length) {
        EntryApp = IframeApp;
    }
    entryTask.add(
        context => TaskExecuteService.fetchTaskInstanceFromAllApp({
            taskInstanceId: apiExecute[1],
        }).then((data) => {
            context.taskData = data;
            context.scopeType = data.scopeType;
            context.scopeId = data.scopeId;
        }),
        (context) => {
            const { taskData } = context;
            if (taskData.isTask) {
                window.BKApp.$router.replace({
                    name: 'historyTask',
                    params: {
                        id: taskData.id,
                    },
                });
            } else {
                window.BKApp.$router.replace({
                    name: 'historyStep',
                    params: {
                        taskInstanceId: taskData.id,
                    },
                });
            }
        },
    );
}
/**
 * @desc 通过第三方系统查看执行方案详情
 */
const apiPlan = window.location.href.match(/api_plan\/([^/]+)/);
if (apiPlan) {
    entryTask.add(
        context => TaskPlanService.fetchPlanData({
            id: apiPlan[1],
        }).then((data) => {
            context.planData = data;
            context.scopeType = data.scopeType;
            context.scopeId = data.scopeId;
        }),
        (context) => {
            const { planData } = context;
            window.BKApp.$router.replace({
                name: 'viewPlan',
                params: {
                    templateId: planData.templateId,
                },
                query: {
                    viewPlanId: planData.id,
                },
            });
        },
    );
}

/**
 * @desc 渲染页面
 */
entryTask.add('', (context) => {
    // 判断是在浏览器访问还是iframe访问，走不同的入口
    const {
        appList,
        isAdmin,
        scopeType,
        scopeId,
    } = context;
    window.PROJECT_CONFIG.SCOPE_TYPE = scopeType;
    window.PROJECT_CONFIG.SCOPE_ID = scopeId;
    scopeCache.setItem({
        scopeType,
        scopeId,
    });

    window.BKApp = new Vue({
        el: '#app',
        router: createRouter({
            appList,
            isAdmin,
            scopeType,
            scopeId,
        }),
        store,
        i18n,
        render: h => h(EntryApp),
    });
});
entryTask.start();
