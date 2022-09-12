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

class ScriptManage extends ModuleBase {
    constructor () {
        super();
        this.module = 'job-manage/web/script';
    }

    // 获取脚本列表
    getAll (params = {}, payload = {}) {
        return Request.get(`${this.path}/script/list`, {
            params,
            payload,
        });
    }

    // 更新脚本
    update (params = {}) {
        return Request.post(`${this.path}/script`, {
            params,
        });
    }

    // 通过脚本id获取脚本详情
    getDataByScriptId ({ id }, payload = {}) {
        return Request.get(`${this.path}/script/${id}`, {
            payload,
        });
    }
    
    // 删除脚本
    deleteById ({ id }) {
        return Request.delete(`${this.path}/script/${id}`, {
            prefixPath: this.prefixPath,
        });
    }

    //  更新脚本元数据（脚本描述，名称，标签）
    updateMeta (payload = {}) {
        const params = {
            ...payload,
        };
        delete params.id;
        return Request.put(`${this.path}/script/${payload.id}/info`, {
            params,
        });
    }

    // 获取脚本的所有版本
    getAllVersion (params = {}, payload = {}) {
        return Request.get(`${this.path}/script/${params.id}/scriptVersion/list`, {
            payload,
        });
    }
    
    // 获取脚本名称列表
    getName (params) {
        return Request.get(`${this.path}/scriptNames`, {
            params,
        });
    }

    // 获取业务下面的已在线脚本列表
    getAllOnline (params) {
        return Request.get(`${this.path}/scripts/online`, {
            params,
        });
    }

    // 根据脚本ID获取已上线脚本
    getOneOnlineByScriptId (payload) {
        const params = { ...payload };
        delete params.id;
        return Request.get(`${this.path}/scriptVersion/online/${payload.id}`, {
            params,
        });
    }

    // 根据脚本版本id获取脚本详情
    getDataByVersionId ({ id }, payload) {
        return Request.get(`${this.path}/scriptVersion/${id}`, {
            payload,
        });
    }

    // 删除某个版本的脚本
    deleteVersionByVersionId ({ versionId }) {
        return Request.delete(`${this.path}/scriptVersion/${versionId}`);
    }

    // 上线某个版本的脚本状态
    updateVersionStatusOnline ({ id, versionId }) {
        return Request.put(`${this.path}/script/${id}/scriptVersion/${versionId}/publish`);
    }

    // 下线某个版本的脚本状态
    updateVersionStatusOffline ({ id, versionId }) {
        return Request.put(`${this.path}/script/${id}/scriptVersion/${versionId}/disable`);
    }

    // 检查脚本内容
    getValidation (params) {
        return Request.put(`${this.module}/check`, {
            params,
        });
    }

    // 上传脚本获取内容
    uploadGetContent (params) {
        return Request.post(`${this.module}/upload`, {
            params,
        });
    }

    // 获取引用脚本的模板与步骤信息
    getRefTemplateSteps ({ id, scriptVersionId }) {
        return Request.get(`${this.path}/script/${id}/scriptVersion/${scriptVersionId}/syncTemplateSteps`);
    }

    // 同步脚本
    syncScriptVersion (payload = {}) {
        const params = {
            ...payload,
        };
        delete params.scriptId;
        delete params.scriptVersionId;
        return Request.post(`${this.path}/script/${payload.scriptId}/scriptVersion/${payload.scriptVersionId}/sync`, {
            params,
        });
    }

    // 根据脚本ID/脚本版本ID获取脚本引用信息
    getCiteInfo (params = {}) {
        return Request.get(`${this.path}/citeInfo`, {
            params,
        });
    }

    // 根据脚本ID获取脚本基本信息
    getBasiceInfoById (params = {}) {
        return Request.get(`${this.path}/script/basic/${params.id}`);
    }

    // 通过ID批量获取脚本基本信息
    getBatchBasiceInfoByIds (params = {}) {
        return Request.get(`${this.path}/script/basic/list`, {
            params,
        });
    }

    // 批量更新脚本 tag
    batchUpdateTag (params = {}) {
        return Request.put(`${this.path}/tag`, {
            params,
        });
    }

    // 获取业务下标签关联的模版数量
    getTagCount (params = {}) {
        return Request.get(`${this.path}/tag/count`, {
            params,
        });
    }
}

export default new ScriptManage();
