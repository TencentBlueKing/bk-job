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

import BaseModel from './base';

export default class ScriptRelated extends BaseModel {
  constructor(payload) {
    super();
    this.scopeType = payload.scopeType;
    this.scopeId = payload.scopeId;
    this.scriptStatus = payload.scriptStatus;
    this.scriptStatusDesc = payload.scriptStatusDesc;
    this.scriptVersion = payload.scriptVersion;
    this.taskTemplateId = payload.taskTemplateId || 0;
    this.taskTemplateName = payload.taskTemplateName;
    this.taskPlanId = payload.taskPlanId || 0;
    this.taskPlanName = payload.taskPlanName;
  }

  /**
     * @desc 同步状态 ICON
     * @returns { HTMLElement }
     */
  get statusHtml() {
    let styles = 'display: inline-block; padding: 0 8px; line-height: 18px; font-size: 12px; border-radius: 2px;';

    switch (this.status) {
      case ScriptRelated.STATUS_INVALID:
        styles += 'background: #F4E3C7; color: #FF9C01';
        break;
      case ScriptRelated.STATUS_ONLINE:
        styles += 'background: #DAEBDE; color: #3FC06D';
        break;
      default:
        styles += 'background: #F0F1F5; color: #979BA5';
    }
    return [
      `<span style="${styles}" data-script-status="${this.status}">`,
      ScriptRelated.STATUS_TEXT_MAP[this.scriptStatus],
      '</span>',
    ].join('');
  }
}
