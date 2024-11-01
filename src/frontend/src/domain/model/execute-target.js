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

export default class ExecuteObjectsInfo {
  static isExecuteObjectsInfoEmpty(executeObjectsInfo) {
    const {
      dynamicGroupList,
      hostList,
      nodeList,
      containerList = [],
    } = executeObjectsInfo;

    return dynamicGroupList.length < 1
            && hostList.length < 1
            && nodeList.length < 1
            && containerList.length < 1;
  }

  static cloneExecuteObjectsInfo(executeObjectsInfo) {
    const {
      dynamicGroupList,
      hostList,
      nodeList,
      containerList = [],
    } = executeObjectsInfo;

    return {
      dynamicGroupList: [...dynamicGroupList],
      hostList: [...hostList],
      nodeList: [...nodeList],
      containerList: [...containerList],
    };
  }

  constructor(payload = {}) {
    this.variable = payload.variable || '';
    this.executeObjectsInfo = this.initExecuteObjectsInfo(payload.executeObjectsInfo || {});
  }

  get isEmpty() {
    return !this.variable && ExecuteObjectsInfo.isExecuteObjectsInfoEmpty(this.executeObjectsInfo);
  }

  get text() {
    const {
      dynamicGroupList,
      hostList,
      nodeList,
      containerList = [],
    } = this.executeObjectsInfo;
    const strs = [];
    if (hostList.length > 0) {
      strs.push(`${hostList.length} ${I18n.t('台主机_result')}`);
    }
    if (nodeList.length > 0) {
      strs.push(`${nodeList.length} ${I18n.t('个节点_result')}`);
    }
    if (dynamicGroupList.length > 0) {
      strs.push(`${dynamicGroupList.length} ${I18n.t('个分组_result')}`);
    }
    if (containerList.length > 0) {
      strs.push(`${containerList.length} ${I18n.t('个容器_result')}`);
    }
    return strs.length > 0 ? strs.join('，') : '--';
  }

  initExecuteObjectsInfo(payload) {
    const {
      hostList,
      dynamicGroupList,
      nodeList,
      containerList,
    } = payload;

    return Object.freeze({
      hostList: hostList || [],
      dynamicGroupList: dynamicGroupList || [],
      nodeList: nodeList || [],
      containerList: containerList || [],
    });
  }
}
