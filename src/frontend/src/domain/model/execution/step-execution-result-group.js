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

import AgentTaskModel from '@model/execution/agent-task-execution';

const STATUS_PENDING = 5;
const STATUS_DOING = 7;

// 步骤执行详情页
// ——执行结果分组实例
export default class ResultGroup {
  constructor(payload) {
    this.resultType = payload.resultType;
    this.resultTypeDesc = payload.resultTypeDesc;
    this.tag = payload.tag || '';
    this.agentTaskExecutionDetail = this.initAgentTaskExecution(payload.agentTaskExecutionDetail);
    this.agentTaskSize = payload.agentTaskSize || 0;
  }

  /**
     * @desc 分组名
     * @returns { String }
     */
  get groupName() {
    let name = `${this.resultTypeDesc}`;
    if (this.tag) {
      name += `(${this.tag})`;
    }
    return name;
  }

  /**
     * @desc 最大长度的 tag (256)
     * @returns { Boolean }
     */
  get tagMaxLength() {
    return this.tag.length >= 256;
  }

  /**
     * @desc 分组结果的数据统计
     * @returns { Number }
     */
  get groupNums() {
    return this.agentTaskExecutionDetail.length;
  }

  /**
     * @desc 当前分组处于 loading 状态
     * @returns { Boolean }
     */
  get isLoading() {
    return [
      STATUS_PENDING,
      STATUS_DOING,
    ].includes(this.resultType);
  }

  /**
     * @desc 执行结果的 agent 实例
     * @param { Array } agentTaskExecutionDetail
     * @returns { Array }
     */
  initAgentTaskExecution = (agentTaskExecutionDetail) => {
    if (!_.isArray(agentTaskExecutionDetail)) {
      return [];
    }
    return agentTaskExecutionDetail.map(item => Object.freeze(new AgentTaskModel(item)));
  };
}
