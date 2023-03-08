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

import I18n from '@/i18n';

export default class TaskHostNode {
  static isHostNodeInfoEmpty(hostNodeInfo) {
    const {
      dynamicGroupList,
      hostList,
      nodeList,
    } = hostNodeInfo;
    return dynamicGroupList.length < 1
            && hostList.length < 1
            && nodeList.length < 1;
  }

  constructor(payload = {}) {
    this.variable = payload.variable || '';
    this.hostNodeInfo = this.initHostNodeInfo(payload.hostNodeInfo || {});
  }

  get isEmpty() {
    const {
      dynamicGroupList,
      hostList,
      nodeList,
    } = this.hostNodeInfo;

    return !this.variable
            && dynamicGroupList.length < 1
            && hostList.length < 1
            && nodeList.length < 1;
  }

  get text() {
    const {
      dynamicGroupList,
      hostList,
      nodeList,
    } = this.hostNodeInfo;
    const strs = [];
    if (hostList.length > 0) {
      strs.push(`${hostList.length} ${I18n.t('台主机.result')}`);
    }
    if (nodeList.length > 0) {
      strs.push(`${nodeList.length} ${I18n.t('个节点.result')}`);
    }
    if (dynamicGroupList.length > 0) {
      strs.push(`${dynamicGroupList.length} ${I18n.t('个分组.result')}`);
    }
    return strs.length > 0 ? strs.join('，') : '--';
  }

  initHostNodeInfo(hostNodeInfo) {
    const {
      hostList,
      dynamicGroupList,
      nodeList,
    } = hostNodeInfo;

    return Object.freeze({
      hostList: hostList || [],
      dynamicGroupList: dynamicGroupList || [],
      nodeList: nodeList || [],
    });
  }
}
