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

import I18n from '@/i18n';

// const STATUS_ABNORMAL = 0 // 异常
// const STATUS_NORMAL = 1 // 正常
// const STATUS_UNKNOWN = -1 // 未知

export default class SeverState extends Model {
  constructor(payload) {
    super();
    this.name = payload.name;
    this.instanceList = payload.instanceList;
    this.version = payload.version || '--';
    this.versionConsistent = payload.versionConsistent;
  }

  get versionHtml() {
    /* eslint-disable max-len */
    return this.versionConsistent
      ? `<span>${this.version}</span>`
      : `<i class="job-icon job-icon-script-disable" style="color: #EA3636;" svg /><span style="color: #63656E;">${I18n.t('存在不同版本')}</span>`;
  }
}
