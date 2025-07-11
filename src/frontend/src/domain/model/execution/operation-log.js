/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

const STATUS_FAIL_RETRY = 1;
const STATUS_ALL_RETRY = 5;

// 步骤执行过程中的操作记录
export default class OperationLog {
  constructor(payload) {
    this.id = payload.id;
    this.batch = payload.batch;
    this.taskInstanceId = payload.taskInstanceId;
    this.operator = payload.operator;
    this.operationName = payload.operationName;
    this.operationCode = payload.operationCode;
    this.stepInstanceId = payload.stepInstanceId;
    this.retry = payload.retry || 0;
    this.stepName = payload.stepName;
    this.createTime = payload.createTime;
    this.detail = payload.detail;
  }

  /**
     * @desc 该记录支持查看详情
     * @returns { Boolean }
     */
  get detailEnable() {
    return [
      STATUS_FAIL_RETRY,
      STATUS_ALL_RETRY,
    ].includes(this.operationCode);
  }
}
