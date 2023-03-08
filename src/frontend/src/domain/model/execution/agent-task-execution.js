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

import Model from '@model/model';

const AGENT_STATUS_SUCCESS = 9;
const AGENT_STATUS_RUNNING = 7;
const AGENT_STATUS_WAITING = 5;
const AGENT_STATUS_LAST_SUCCESS = 3;

export default class AgentTaskExecutionDetail extends Model {
  constructor(payload) {
    super();
    this.agentId = payload.agentId;
    this.batch = payload.batch;
    this.cloudAreaId = payload.cloudAreaId;
    this.cloudAreaName = payload.cloudAreaName;
    this.displayIp = payload.displayIp;
    this.endTime = payload.endTime;
    this.errorCode = payload.errorCode;
    this.exitCode = payload.exitCode;
    this.hostId = payload.hostId;
    this.ip = payload.ip;
    this.ipv4 = payload.ipv4;
    this.ipv6 = payload.ipv6;
    this.retryCount = payload.retryCount || 0;
    this.startTime = payload.startTime;
    this.status = payload.status;
    this.statusDesc = payload.statusDesc;
    this.tag = payload.tag;
    this.totalTime = payload.totalTime;
  }

  /**
     * @desc 表示 agent 的唯一 key
     * @returns { String }
     */
  get key() {
    return `${this.hostId}_${this.ip}_${this.ipv6}`;
  }

  /**
     * @desc agent 的状态
     * @returns { String }
     */
  get result() {
    if ([
      AGENT_STATUS_SUCCESS,
      AGENT_STATUS_LAST_SUCCESS,
    ].includes(this.status)) {
      return 'success';
    }
    if ([
      AGENT_STATUS_RUNNING,
    ].includes(this.status)) {
      return 'running';
    }
    if ([
      AGENT_STATUS_WAITING,
    ].includes(this.status)) {
      return 'waiting';
    }
    return 'fail';
  }
}
