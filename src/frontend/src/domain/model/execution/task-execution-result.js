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

import _ from 'lodash';

import StepExecutionModel from '@model/execution/task-execution-result-step';
import TaskExecutionModel from '@model/execution/task-execution-result-task';

// 作业执行详情页面（包含作业执行状态、作业中步骤执行状态）
export default class TaskExecutionResult {
  constructor(payload) {
    this.finished = payload.finished;
    this.taskExecution = this.initTaskExecution(payload.taskExecution);
    this.stepExecution = this.initStepExecution(payload.stepExecution);
  }

  /**
     * @desc 作业包含的步骤总数
     * @returns { Number }
     */
  get totalStep() {
    return this.stepExecution.length || 0;
  }

  /**
     * @desc 正在执行的步骤的排序
     * @returns { Number }
     */
  get currentStepRunningOrder() {
    return _.findIndex(this.stepExecution, _ => _.currentStepRunning) + 1;
  }

  /**
     * @desc 处理作业的执行信息
     * @param { Object } taskInstance
     * @returns { Object }
     */
  initTaskExecution(taskInstance) {
    if (!taskInstance || !_.isObject(taskInstance)) {
      return Object.freeze(new TaskExecutionModel({}));
    }
    return Object.freeze(new TaskExecutionModel(taskInstance));
  }

  /**
     * @desc 处理作业包含的步骤执行信息
     * @param { Array } stepExecution
     * @returns { Array }
     */
  initStepExecution(stepExecution) {
    if (!stepExecution || !_.isArray(stepExecution)) {
      return [];
    }
    return stepExecution.map(_ => Object.freeze(new StepExecutionModel(_)));
  }
}
