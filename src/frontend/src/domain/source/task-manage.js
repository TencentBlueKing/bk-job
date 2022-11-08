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

class TaskManage extends ModuleBase {
    constructor () {
        super();
        this.module = '/job-manage/web';
    }

    // 获取模板列表
    getAll (params) {
        return Request.get(`${this.path}/task/template/`, {
            params,
        });
    }

    // 根据模板id获取模板信息
    getDataById ({ id }, payload) {
        return Request.get(`${this.path}/task/template/${id}`, {
            payload,
        });
    }

    // 更新模板
    update (payload = {}) {
        const params = { ...payload };
        delete params.id;
        return Request.put(`${this.path}/task/template/${payload.id}`, {
            params,
        });
    }

    // 删除模板
    deleteById ({ id }) {
        return Request.delete(`${this.path}/task/template/${id}`);
    }

    // 更新模板元数据
    updateBasic (payload = {}) {
        const params = { ...payload };
        return Request.put(`${this.path}/task/template/${params.id}/basic`, {
            params,
        });
    }

    // 新增收藏
    updateFavorite (payload = {}) {
        const params = { ...payload };
        return Request.put(`${this.path}/task/template/${params.id}/favorite`);
    }

    // 删除收藏
    deleteFavorite (payload = {}) {
        const params = { ...payload };
        return Request.delete(`${this.path}/task/template/${params.id}/favorite`);
    }

    // 检查作业模板名称是否已占用
    getCheckResult (payload) {
        const params = { ...payload };
        delete params.id;
        return Request.get(`${this.path}/task/template/${payload.id}/check_name`, {
            params,
        });
    }

    // 根据ID获取作业模板
    getBasicById (params = {}) {
        return Request.get(`${this.path}/task/template/basic`, {
            params,
        });
    }

    // 批量更新 tag
    batchUpdateTag (params = {}) {
        return Request.put(`${this.path}/task/template/tag`, {
            params,
        });
    }

    // 获取业务下标签关联的模版数量
    getTagCount (params = {}) {
        return Request.get(`${this.path}/task/template/tag/count`, {
            params,
        });
    }
}

export default new TaskManage();
