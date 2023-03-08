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

import TaskHostNodeModel from '@model/task-host-node';

import I18n from '@/i18n';

const transferModeMap = {
  1: I18n.t('严谨模式'),
  2: I18n.t('强制模式'),
  3: I18n.t('保险模式'),
};

export default class TaskFileStep {
  static TYPE_SERVER = 1;
  static TYPE_LOCAL = 2;
  static TYPE_SOURCE = 3;
  constructor(payload = {}) {
    this.timeout = payload.timeout;
    this.uploadSpeedLimit = payload.uploadSpeedLimit || 0;
    this.downloadSpeedLimit = payload.downloadSpeedLimit || 0;
    this.notExistPathHandler = payload.notExistPathHandler;
    this.duplicateHandler = payload.duplicateHandler;
    this.transferMode = payload.transferMode || 1;
    this.ignoreError = payload.ignoreError || 0;

    this.fileDestination = this.initFileDestination(payload.fileDestination);
    this.fileSourceList = this.initFileSourceList(payload.fileSourceList);
  }

  get uploadSpeedLimitText() {
    if (this.uploadSpeedLimit < 1) {
      return I18n.t('否');
    }
    return `${this.uploadSpeedLimit} (MB/s)`;
  }

  get downloadSpeedLimitText() {
    if (this.downloadSpeedLimit < 1) {
      return I18n.t('否');
    }
    return `${this.downloadSpeedLimit} (MB/s)`;
  }

  get ignoreErrorText() {
    return this.ignoreError === 0 ? I18n.t('不忽略') : I18n.t('自动忽略错误');
  }

  get transferModeText() {
    return transferModeMap[this.transferMode];
  }

  initFileDestination(fileDestination) {
    if (!_.isObject(fileDestination)) {
      return {
        account: '',
        path: '',
        server: new TaskHostNodeModel({}),
      };
    }
    const {
      account,
      path,
      server,
    } = fileDestination;
    return {
      account: account || '',
      path: path || '',
      server: new TaskHostNodeModel(server || {}),
    };
  }

  initFileSourceList(fileSourceList) {
    if (!_.isArray(fileSourceList)) {
      return [];
    }
    return fileSourceList.map(item => ({
      id: item.id,
      fileType: item.fileType,
      fileLocation: item.fileLocation || [],
      fileHash: item.fileHash,
      fileSize: parseInt(item.fileSize, 10) || 0,
      fileSourceId: item.fileSourceId || 0,
      host: new TaskHostNodeModel(item.host || {}),
      account: item.account || 0,
    }));
  }
}
