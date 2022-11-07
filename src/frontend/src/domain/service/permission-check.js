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

/* eslint-disable no-param-reassign */
import AuthResultModel from '@model/auth-result';

import {
    checkPublicScript,
    isJobCrontabPermission,
    isJobExecutePermission,
    isJobFileSourcePermission,
    isJobManagePermission,
    isJobTicketPermission,
    isScriptPermission,
} from '@utils/assist';

import PermissionCheckSource from '../source/permission-check';

const permissionCheckService = {
    fetchJobManagePermission (params = {}) {
        return PermissionCheckSource.jobManage(params)
            .then(({ data }) => new AuthResultModel(data));
    },
    fetchJobCrontabPermission (params = {}) {
        return PermissionCheckSource.jobCrontab(params)
            .then(({ data }) => new AuthResultModel(data));
    },
    fetchJobExecutePermission (params) {
        return PermissionCheckSource.jobExecute(params)
            .then(({ data }) => new AuthResultModel(data));
    },
    fetchJobFileSourcePermission (params) {
        return PermissionCheckSource.jobFileSource(params)
            .then(({ data }) => new AuthResultModel(data));
    },
    fetchJobTickerPermissionn (params) {
        return PermissionCheckSource.jobTicket(params)
            .then(({ data }) => new AuthResultModel(data));
    },
    fetchPermission (params) {
        let requestService = () => Promise.resolve(new AuthResultModel({}));
        if (isJobManagePermission(params.operation)) {
            requestService = permissionCheckService.fetchJobManagePermission;
        } else if (isJobCrontabPermission(params.operation)) {
            requestService = permissionCheckService.fetchJobCrontabPermission;
        } else if (isJobExecutePermission(params.operation)) {
            requestService = permissionCheckService.fetchJobExecutePermission;
        } else if (isJobFileSourcePermission(params.operation)) {
            requestService = permissionCheckService.fetchJobFileSourcePermission;
        } else if (isJobTicketPermission(params.operation)) {
            requestService = permissionCheckService.fetchJobTickerPermissionn;
        }
        params = { ...params };
        // 公共脚本权限需要拼接一个public_
        if (isScriptPermission(params.operation) && checkPublicScript(window.BKApp.$route)) {
            params.operation = `public_${params.operation}`;
        }

        return requestService(params);
    },
};

export default permissionCheckService;
