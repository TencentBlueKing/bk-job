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

import GlobalVariableModel from '@model/task/global-variable';
import PlanStepModel from '@model/task/plan-step';

export default class Plan {
  constructor(payload) {
    this.id = payload.id;
    this.name = payload.name;
    this.scopeType = payload.scopeType;
    this.scopeId = payload.scopeId;
    this.favored = Boolean(payload.favored);
    this.cronJobCount = payload.cronJobCount || 0;
    this.hasCronJob = Boolean(payload.hasCronJob);
    this.needUpdate = Boolean(payload.needUpdate);
    this.templateId = payload.templateId;
    this.templateName = payload.templateName;
    this.templateVersion = payload.templateVersion;
    this.version = payload.version;
    this.createTime = payload.createTime;
    this.creator = payload.creator;
    this.lastModifyTime = payload.lastModifyTime;
    this.lastModifyUser = payload.lastModifyUser;

    this.stepList = this.initStep(payload.stepList);
    this.variableList = this.initVariable(payload.variableList);

    // 权限
    this.canDelete = payload.canDelete;
    this.canEdit = payload.canEdit;
    this.canView = payload.canView;
  }

  // 执行方案中使用到的模板步骤
  get enableStepList() {
    return _.filter(this.stepList, item => item.enable);
  }

  // 所有使用到来模板步骤的id
  get enableStepId() {
    return this.stepList.reduce((result, step) => {
      if (step.enable) {
        result.push(step.id);
      }
      return result;
    }, []);
  }

  // 关联作业模板中的步骤数
  get templateStepNums() {
    return this.stepList.length;
  }

  // 执行方案中使用的到模板步骤数
  get enableStepNums() {
    const stepList = _.filter(this.stepList, item => item.enable);
    return stepList.length;
  }

  toggleFavored() {
    this.favored = !this.favored;
  }

  initStep(payload) {
    if (!Array.isArray(payload)) {
      return [];
    }
    return payload.map(item => new PlanStepModel(item));
  }

  initVariable(payload) {
    if (!Array.isArray(payload)) {
      return [];
    }
    return payload.map(item => new GlobalVariableModel(item));
  }
}
