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

import VariableModel from '@model/task/global-variable';
import TaskStepModel from '@model/task/task-step';

import I18n from '@/i18n';

export const ipStatistics = (hostNodeList) => {
  const stack = [];
  if (hostNodeList.hostList.length > 0) {
    stack.push(`${hostNodeList.hostList.length}${I18n.t('template.个主机')}`);
  }
  if (hostNodeList.nodeList.length > 0) {
    stack.push(`${hostNodeList.nodeList.length}${I18n.t('template.个节点')}`);
  }
  if (hostNodeList.dynamicGroupList.length > 0) {
    stack.push(`${hostNodeList.dynamicGroupList.length}${I18n.t('template.个分组')}`);
  }

  return stack.join('，');
};

export const findStep = (target, id) => {
  const step = _.find(target, _ => _.realId === id);
  if (step) {
    return step;
  }
  return new TaskStepModel({
    approvalStepInfo: {},
    fileStepInfo: {},
    scriptStepInfo: {},
  });
};

export const findVariable = (target, name) => {
  const step = _.find(target, _ => _.name === name);
  if (step) {
    return step;
  }
  return new VariableModel({
    type: 3,
  });
};
