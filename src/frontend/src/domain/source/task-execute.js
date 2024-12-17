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
  constructor() {
    super();
    this.module = '/job-execute/web/execution';
  }

  // 获取作业执行历史列表
  getAll(params = {}) {
    return Request.get(`${this.path}/task-execution-history/list`, {
      params,
    });
  }

  // 获取作业执行情况
  getOneTaskById({ id }, payload = {}) {
    return Request.get(`${this.path}/task-execution-result/${id}`, {
      payload,
    });
  }

  // 获取作业实例
  getOneTaskInstanceById({ id }) {
    return Request.get(`${this.path}/task-instance/${id}`);
  }

  getLogByHostId(params) {
    const {
      taskInstanceId,
      stepInstanceId,
      executeObjectType,
      executeObjectResourceId,
    } = params;

    const realParams = { ...params };
    delete realParams.taskInstanceId;
    delete realParams.stepInstanceId;
    delete realParams.executeObjectType;
    delete realParams.executeObjectResourceId;

    return Request.get(`${this.path}/taskInstance/${taskInstanceId}/stepInstance/${stepInstanceId}/executeObject/${executeObjectType}/${executeObjectResourceId}/scriptLog`, {
      params: realParams,
    });
  }

  // 获取文件分发步骤IP的日志基本信息
  getFileLogByHostId(params = {}) {
    const realParams = { ...params };
    const {
      taskInstanceId,
      stepInstanceId,
      executeObjectType,
      executeObjectResourceId,
    } = params;
    delete realParams.taskInstanceId;
    delete realParams.stepInstanceId;
    delete realParams.executeObjectType;
    delete realParams.executeObjectResourceId;

    return Request.get(`${this.path}/taskInstance/${taskInstanceId}/stepInstance/${stepInstanceId}/executeObject/${executeObjectType}/${executeObjectResourceId}/fileLog`, {
      params: realParams,
    });
  }

  // 获取文件分发指定文件的日志内容
  getFileLogByFileId(params = {}) {
    const {
      taskInstanceId,
      stepInstanceId,
    } = params;

    return Request.post(`${this.path}/taskInstance/${taskInstanceId}/stepInstance/${stepInstanceId}/fileLog/queryByIds`, {
      params: params.taskIds,
    });
  }

  // 获取作业步骤执行情况
  getOneStep(params = {}, payload) {
    const tempParams = {
      ...params,
    };
    delete tempParams.taskInstanceId;
    delete tempParams.id;

    return Request.get(`${this.path}/taskInstance/${params.taskInstanceId}/stepInstance/${params.id}/stepExecutionResult`, {
      params: tempParams,
      payload,
    });
  }

  // 获取作业步骤实例详情
  getOneStepInstance(params) {
    return Request.get(`${this.path}/taskInstance/${params.taskInstanceId}/stepInstance/${params.id}/detail`);
  }

  // 获取作业实例全局参数
  getAllStepInstanceParam({ id }) {
    return Request.get(`${this.path}/task-instance/task-variables/${id}`);
  }

  // 重做执行作业
  updateRedoTask(params) {
    return Request.post(`${this.path}/task-execution/redo-task`, {
      params,
    });
  }

  // 快速执行脚本
  executeScript(params = {}) {
    return Request.post(`${this.path}/fast-execute-script`, {
      params,
    });
  }

  // 快速分发文件
  pushFile(params = {}) {
    return Request.post(`${this.path}/fast-push-file`, {
      params,
    });
  }

  // 执行作业
  taskExecution(params = {}) {
    return Request.post(`${this.path}/task-execution`, {
      params,
    });
  }

  // 终止作业
  terminateTaskById(params = {}) {
    return Request.post(`${this.path}/taskInstance/${params.taskInstanceId}/terminate`);
  }

  // 上传文件获取内容
  uploadFileGetContent(params = {}, payload = {}) {
    return Request.post('/job-manage/web/upload/localFile', {
      params,
      payload: {
        ...payload,
        timeout: 0,
      },
    });
  }

  // 执行作业步骤操作
  taskExecutionStepOperate(payload) {
    const params = { ...payload };
    delete params.id;
    delete params.taskInstanceId;
    return Request.post(`${this.path}/taskInstance/${payload.taskInstanceId}/stepInstance/${payload.id}/operate`, {
      params,
    });
  }

  // 获取作业操作日志
  getTaskOperationLog({ id }) {
    return Request.get(`${this.path}/task-instance/operation-log/${id}`);
  }

  // 获取执行步骤主机对应的变量列表
  getStepVariables(params) {
    const realParams = { ...params };
    delete realParams.stepInstanceId;
    delete realParams.executeObjectType;
    delete realParams.executeObjectResourceId;
    return Request.get(`${this.path}/taskInstance/${params.taskInstanceId}/stepInstance/${params.stepInstanceId}/executeObject/${params.executeObjectType}/${params.executeObjectResourceId}/variables`, {
      params: realParams,
    });
  }

  // 请求日志打包结果
  getLogFilePackageInfo(params = {}) {
    const {
      taskInstanceId,
      stepInstanceId,
    } = params;

    const realParams = { ...params };
    delete realParams.taskInstanceId;
    delete realParams.stepInstanceId;

    return Request.get(`${this.path}/taskInstance/${taskInstanceId}/stepInstance/${stepInstanceId}/requestDownloadLogFile`, {
      params: realParams,
    });
  }

  // 下载执行日志文件
  getLogFile(params) {
    const {
      taskInstanceId,
      stepInstanceId,
    } = params;

    const realParams = {
      ...params,
    };
    delete realParams.taskInstanceId;
    delete realParams.stepInstanceId;

    return Request.download(`${this.path}/taskInstance/${taskInstanceId}/stepInstance/${stepInstanceId}/downloadLogFile`, {
      params: realParams,
    });
  }

  // 获取作业实例基本信息(所有业务下搜索)
  getTaskInstanceFromAllApp(params = {}) {
    return Request.get(`${this.module}/task_instance/${params.taskInstanceId}`);
  }

  // 获取执行结果分组下的主机列表
  getStepGroupHost(params = {}) {
    const {
      taskInstanceId,
      id,
      executeCount,
    } = params;

    const realParams = { ...params };
    delete realParams.taskInstanceId;
    delete realParams.id;
    delete realParams.executeCount;

    return Request.get(`${this.path}/taskInstance/${taskInstanceId}/stepInstance/${id}/${executeCount}/executeObjects`, {
      params: realParams,
    });
  }

  // 获取步骤执行历史
  getStepExecutionHistory(params = {}) {
    const realParams = { ...params };
    delete realParams.taskInstanceId;
    delete realParams.stepInstanceId;
    return Request.get(`${this.path}/taskInstance/${params.taskInstanceId}/stepInstance/${params.stepInstanceId}/stepExecutionHistory`, {
      params: realParams,
    });
  }
}

export default new TaskExecute();
