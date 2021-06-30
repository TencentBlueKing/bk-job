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

import {
    makeMap,
} from './make-map';

const busnissPermissionList = [
    'biz/access_business',
];

const templatePermissionList = [
    'job_template/create',
    'job_template/view',
    'job_template/edit',
    'job_template/delete',
    'job_template/clone',
    'job_template/debug',
];
const planPermissionList = [
    'job_plan/create',
    'job_plan/view',
    'job_plan/edit',
    'job_plan/delete',
    'job_plan/execute',
    'job_plan/sync',
];
const scriptPermissionList = [
    'script/create',
    'script/view',
    'script/edit',
    'script/delete',
    'script/execute',
    'script/clone',
];

const publicScriptPermissionList = [
    'public_script/create',
    'public_script/view',
    'public_script/edit',
    'public_script/delete',
    'public_script/execute',
    'public_script/clone',
];

const tagPermissionList = [
    'tag/create',
    'tag/edit',
    'tag/delete',
];

const accountPermissionList = [
    'account/create',
    'account/view',
    'account/edit',
    'account/delete',
    'account/use',
];

const whiteListPermissionList = [
    'whitelist/create',
    'whitelist/view',
    'whitelist/edit',
    'whitelist/delete',
];

const cronPermissionList = [
    'cron/create',
    'cron/view',
    'cron/edit',
    'cron/delete',
    'cron/execute',
];

const executePermissionList = [
    'task_instance/view',
    'task_instance/redo',
];

const fileSourcePermissionList = [
    'file_source/view',
    'file_source/create',
    'file_source/edit',
    'file_source/delete',
];

const ticketPermissionList = [
    'ticket/create',
    'ticket/edit',
    'ticket/delete',
    'ticket/use',
];

const jobManagePermissionMap = makeMap([
    ...busnissPermissionList,
    ...scriptPermissionList,
    ...publicScriptPermissionList,
    ...templatePermissionList,
    ...planPermissionList,
    ...tagPermissionList,
    ...accountPermissionList,
    ...whiteListPermissionList,
]);

const jobCrontabPermissionMap = makeMap(cronPermissionList);

const jobExecutePermissionMap = makeMap(executePermissionList);

const jobFileSourcePermissionMap = makeMap(fileSourcePermissionList);

const jobTickerPermissionMap = makeMap(ticketPermissionList);

export const isScriptPermission = permission => /^script\//.test(permission);

export const isJobManagePermission = permission => !!jobManagePermissionMap[permission];

export const isJobCrontabPermission = permission => !!jobCrontabPermissionMap[permission];

export const isJobExecutePermission = permission => !!jobExecutePermissionMap[permission];

export const isJobFileSourcePermission = permission => !!jobFileSourcePermissionMap[permission];

export const isJobTicketPermission = permission => !!jobTickerPermissionMap[permission];
