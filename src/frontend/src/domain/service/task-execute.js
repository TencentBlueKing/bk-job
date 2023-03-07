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
import OperationLogModel from '@model/execution/operation-log';
import StepExecutionResultModel from '@model/execution/step-execution-result';
import TaskExecutionResultModel from '@model/execution/task-execution-result';
import TaskInstanceModel from '@model/execution/task-instance';
import TaskInstanceDetailModel from '@model/execution/task-instance-detail';
import GlobalVariableModel from '@model/task/global-variable';

import TaskExecuteSource from '../source/task-execute';

export default {
  taskExecution(params) {
    return TaskExecuteSource.taskExecution(params)
      .then(({ data }) => data);
  },
  executeScript(params) {
    return TaskExecuteSource.executeScript(params)
      .then(({ data }) => data);
  },
  pushFile(params) {
    return TaskExecuteSource.pushFile(params)
      .then(({ data }) => data);
  },
  redoTask(params) {
    return TaskExecuteSource.updateRedoTask(params)
      .then(({ data }) => data);
  },
  fetchExecutionHistoryList(params) {
    return TaskExecuteSource.getAll(params)
      .then(({ data }) => {
        data.data = data.data.map(item => Object.freeze(new TaskInstanceModel(item)));
        return data;
      });
  },
  fetchTaskExecutionResult(params, payload) {
    return TaskExecuteSource.getOneTaskById(params, payload)
      .then(({ data }) => Object.freeze(new TaskExecutionResultModel(data)));
  },
  fetchTaskInstance(params) {
    return TaskExecuteSource.getOneTaskInstanceById(params)
      .then(({ data }) => new TaskInstanceDetailModel(data));
  },
  fetchStepExecutionResult(params, payload) {
    return TaskExecuteSource.getOneStep(params, payload)
      .then(({ data }) => new StepExecutionResultModel(data));
  },
  fetchLogContentOfIp(params) {
    return TaskExecuteSource.getLogByIp(params)
      .then(({ data }) => data);
  },
  fetchLogContentOfHostId(params) {
    return TaskExecuteSource.getLogByHostId(params)
      .then(({ data }) => data);
  },
  fetchFileLogOfIp(params = {}) {
    return TaskExecuteSource.getFileLogByIP(params)
      .then(({ data }) => data);
  },
  fetchFileLogOfHostId(params = {}) {
    return TaskExecuteSource.getFileLogByHostId(params)
      .then(({ data }) => data);
  },
  fetchFileLogOfFile(params = {}) {
    return TaskExecuteSource.getFileLogByFileId(params)
      .then(({ data }) => data);
  },
  fetchStepInstance(params = {}) {
    return TaskExecuteSource.getOneStepInstance(params)
      .then(({ data }) => data);
  },
  fetchStepInstanceParam(params) {
    return TaskExecuteSource.getAllStepInstanceParam(params)
      .then(({ data }) => data);
  },
  getUploadFileContent(params, payload) {
    return TaskExecuteSource.uploadFileGetContent(params, payload)
      .then(({ data }) => Object.freeze(data));
  },
  updateTaskExecutionStepOperate(params) {
    return TaskExecuteSource.taskExecutionStepOperate(params)
      .then(({ data }) => Object.freeze(data));
  },
  updateTaskExecutionStepOperateTerminate(params) {
    return TaskExecuteSource.terminateTaskById(params)
      .then(({ data }) => data);
  },
  fetchTaskOperationLog(params) {
    return TaskExecuteSource.getTaskOperationLog(params)
      .then(({ data }) => Object.freeze(data.map(item => new OperationLogModel(item))));
  },
  fetchStepVariables(params) {
    return TaskExecuteSource.getStepVariables(params)
      .then(({ data }) => data.map(_ => new GlobalVariableModel(_)));
  },
  fetchLogFilePackageResult(params) {
    return TaskExecuteSource.getLogFilePackageInfo(params)
      .then(({ data }) => data);
  },
  fetchStepExecutionLogFile(params) {
    return TaskExecuteSource.getLogFile(params);
  },
  fetchTaskInstanceFromAllApp(params) {
    return TaskExecuteSource.getTaskInstanceFromAllApp(params)
      .then(({ data }) => new TaskInstanceModel(data));
  },
  fetchStepGroupHost(params) {
    return TaskExecuteSource.getStepGroupHost(params)
      .then(({ data }) => data);
  },
  fetchStepExecutionHistory(params) {
    return TaskExecuteSource.getStepExecutionHistory(params)
      .then(({ data }) => data);
  },
};
