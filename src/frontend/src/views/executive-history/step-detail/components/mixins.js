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

export default {
  created() {
    this.stopPollingRun = false;
    this._pollingTaskQueue = []; // eslint-disable-line no-underscore-dangle
    this._stepTimer = [ // eslint-disable-line no-underscore-dangle
      1000, 1000, 1000, 1000, 2000, 2000, 2000,
    ];
    this.startPollingQueueRun();
  },
  beforeDestroy() {
    this.stopPollingQueueRun();
  },
  methods: {
    $pollingQueueRun(task) {
      this._pollingTaskQueue = [ // eslint-disable-line no-underscore-dangle
        task,
      ];
    },
    startPollingQueueRun() {
      if (this.stopPollingRun) {
        return;
      }
      let timer = 1000;
      if (this._pollingTaskQueue.length > 0) { // eslint-disable-line no-underscore-dangle
        const nextStep = this._pollingTaskQueue.shift(); // eslint-disable-line no-underscore-dangle
        nextStep();
        timer = this._stepTimer.shift(); // eslint-disable-line no-underscore-dangle
      }
      if (!timer) {
        timer = 3000;
      }
      setTimeout(() => {
        this.startPollingQueueRun();
      }, timer);
    },
    stopPollingQueueRun() {
      this.stopPollingRun = true;
      this._pollingTaskQueue = []; // eslint-disable-line no-underscore-dangle
    },
  },
};
