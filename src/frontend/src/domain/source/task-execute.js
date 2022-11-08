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

/* eslint-disable max-len */
class TaskExecute extends ModuleBase {
    constructor () {
        super();
        this.module = '/job-execute/web/execution';
    }

    // 获取作业执行历史列表
    getAll (params = {}) {
        return Request.get(`${this.path}/task-execution-history/list`, {
            params,
        });
    }

    // 获取作业执行情况
    getOneTaskById ({ id }, payload = {}) {
        return Request.get(`${this.path}/task-execution-result/${id}`, {
            payload,
        });
    }

    // 获取作业实例
    getOneTaskInstanceById ({ id }) {
        return Request.get(`${this.path}/task-instance/${id}`);
    }

    // 获取ip对应的日志内容
    getLogByIp (params) {
        const { stepInstanceId, retryCount, ip } = params;
        const realParams = { ...params };
        delete realParams.stepInstanceId;
        delete realParams.retryCount;
        delete realParams.hostId;
        delete realParams.ip;
        return Request.get(`${this.path}/step-execution-result/log-content/${stepInstanceId}/${retryCount}/${ip}`, {
            params: realParams,
        });
    }

    getLogByHostId (params) {
        const { stepInstanceId, retryCount, hostId } = params;
        const realParams = { ...params };
        delete realParams.stepInstanceId;
        delete realParams.retryCount;
        delete realParams.hostId;
        delete realParams.ip;
        return Request.get(`${this.path}/step-execution-result/log-content/${stepInstanceId}/${retryCount}/host/${hostId}`, {
            params: realParams,
        });
    }

    // 获取文件分发步骤IP的日志基本信息
    getFileLogByIP (params = {}) {
        const realParams = { ...params };
        const { stepInstanceId, retryCount, ip } = params;
        delete realParams.stepInstanceId;
        delete realParams.retryCount;
        delete realParams.hostId;
        delete realParams.ip;

        return Request.get(`${this.path}/step-execution-result/log-content/file/${stepInstanceId}/${retryCount}/${ip}`, {
            params: realParams,
        });
    }

    // 获取文件分发步骤IP的日志基本信息
    getFileLogByHostId (params = {}) {
        const realParams = { ...params };
        const { stepInstanceId, retryCount, hostId } = params;
        delete realParams.stepInstanceId;
        delete realParams.retryCount;
        delete realParams.hostId;
        delete realParams.ip;

        return Request.get(`${this.path}/step-execution-result/log-content/file/${stepInstanceId}/${retryCount}/host/${hostId}`, {
            params: realParams,
        });
    }

    // 获取文件分发指定文件的日志内容
    getFileLogByFileId (params = {}) {
        const realParams = { ...params };
        const { stepInstanceId, retryCount } = params;
        delete realParams.stepInstanceId;
        delete realParams.retryCount;

        return Request.post(`${this.path}/step-execution-result/log-content/file/${stepInstanceId}/${retryCount}/query-by-ids`, {
            params: realParams.taskIds,
        });
    }

    // 根据日志搜索结果显示ip
    getIpByLog ({ stepInstanceId, retryCount }) {
        return Request.get(`${this.path}/step-execution-result/agent-execution-list/${stepInstanceId}/${retryCount}`);
    }

    // 获取作业步骤执行情况
    getOneStep (params = {}, payload) {
        const tempParams = {
            ...params,
        };
        delete tempParams.id;
        delete tempParams.retryCount;
        return Request.get(`${this.path}/step-execution-result/${params.id}/${params.retryCount}`, {
            params: tempParams,
            payload,
        });
    }

    // 获取作业步骤实例详情
    getOneStepInstance ({ id }) {
        return Request.get(`${this.path}/task-instance/step_instance/${id}`);
    }

    // 获取作业实例全局参数
    getAllStepInstanceParam ({ id }) {
        return Request.get(`${this.path}/task-instance/task-variables/${id}`);
    }

    // 重做执行作业
    updateRedoTask (params) {
        return Request.post(`${this.path}/task-execution/redo-task`, {
            params,
        });
    }

    // 快速执行脚本
    executeScript (params = {}) {
        return Request.post(`${this.path}/fast-execute-script`, {
            params,
        });
    }

    // 快速分发文件
    pushFile (params = {}) {
        return Request.post(`${this.path}/fast-push-file`, {
            params,
        });
    }

    // 执行作业
    taskExecution (params = {}) {
        return Request.post(`${this.path}/task-execution`, {
            params,
        });
    }

    // 终止作业
    terminateTaskById (params = {}) {
        return Request.post(`${this.path}/taskInstance/${params.taskInstanceId}/terminate`);
    }

    // 上传文件获取内容
    uploadFileGetContent (params = {}, payload = {}) {
        return Request.post('/job-manage/web/upload/localFile', {
            params,
            payload: {
                ...payload,
                timeout: 0,
            },
        });
    }

    // 执行作业步骤操作
    taskExecutionStepOperate (payload) {
        const params = { ...payload };
        delete params.id;
        return Request.post(`${this.path}/do-step-operation/stepInstanceId/${payload.id}`, {
            params,
        });
    }

    // 获取作业操作日志
    getTaskOperationLog ({ id }) {
        return Request.get(`${this.path}/task-instance/operation-log/${id}`);
    }

    // 获取执行步骤主机对应的变量列表
    getStepVariables ({ id, ip }) {
        return Request.get(`${this.path}/step-execution-result/variable/${id}/${ip}`);
    }

    // 请求日志打包结果
    getLogFilePackageInfo (params = {}) {
        const realParams = { ...params };
        delete realParams.stepInstanceId;

        return Request.get(`${this.path}/step-execution-result/${params.stepInstanceId}/log-file`, {
            params: realParams,
        });
    }

    // 下载执行日志文件
    getLogFile (payload) {
        const params = {};
        if (payload.ip) {
            params.ip = payload.ip;
        }
        return Request.download(`${this.path}/step-execution-result/${payload.id}/log-file/download`, {
            params,
        });
    }

    // 获取作业实例基本信息(所有业务下搜索)
    getTaskInstanceFromAllApp (params = {}) {
        return Request.get(`${this.module}/task_instance/${params.taskInstanceId}`);
    }

    // 获取执行结果分组下的主机列表
    getStepGroupHost (params = {}) {
        const realParams = { ...params };
        delete realParams.id;
        delete realParams.retryCount;

        return Request.get(`${this.path}/step-execution-result/hosts/${params.id}/${params.retryCount}`, {
            params: realParams,
        });
    }

    // 获取步骤执行历史
    getStepExecutionHistory (params = {}) {
        const realParams = { ...params };
        delete realParams.stepInstanceId;
        return Request.get(`${this.path}/step-execution-history/${params.stepInstanceId}`, {
            params: realParams,
        });
    }
}

export default new TaskExecute();
