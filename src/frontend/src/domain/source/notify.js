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

import Request from '@utils/request';

import ModuleBase from './module-base';

class NotifyManage extends ModuleBase {
    constructor () {
        super();
        this.module = '/job-manage/web/notify';
    }

    // 获取现有通知黑名单用户列表
    getBlacklist (params = {}) {
        return Request.get(`${this.module}/users/blacklist`, {
            params,
        });
    }

    // 更新通知黑名单
    updateBlacklist (params = {}) {
        return Request.post(`${this.module}/`, {
            params,
        });
    }

    // 删除黑名单成员
    deleteBlacklistMember (params = {}) {
        return Request.delete(`${this.module}/`, {
            params,
        });
    }

    // 获取业务通知策略列表
    getPoliciesList (params) {
        return Request.get(`${this.path}/policies/listDefault`, { params });
    }

    // 保存业务下默认通知策略
    updateDefaultPolicies (params = {}) {
        return Request.post(`${this.path}/saveAppDefaultPolicies`, { params });
    }

    // 拉取全量用户列表（不包括黑名单内用户）
    getAllUsers (params) {
        return Request.get(`${this.module}/users/list`, { params });
    }

    // 获取页面模板
    getPageTemplate () {
        return Request.get(`${this.module}/pageTemplate`);
    }

    // 获取ESB支持的所有通知渠道列表
    getAllChannel () {
        return Request.get(`${this.module}/notifyChannel/listAll`);
    }

    // 获取可用的通知渠道列表
    getAvailableChannel () {
        return Request.get(`${this.module}/notifyChannel/listAvailable`);
    }

    getAllRole () {
        return Request.get(`${this.module}/roles/list`);
    }
}

export default new NotifyManage();
