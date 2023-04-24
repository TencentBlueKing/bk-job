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

import TaskHostNodeModel from '@model/task-host-node';

import I18n from '@/i18n';

export default class TaskInstanceDetailStepScript {
  static STATUS_SCRIPT_NEED_UPDATE = 1;
  static STATUS_SCRIPT_DISABLED = 2;

  static TYPE_SOURCE_LOCAL = 1;
  static TYPE_SOURCE_BUSINESS = 2;
  static TYPE_SOURCE_PUBLIC = 3;

  constructor(payload = {}) {
    this.scriptId = payload.scriptId;
    this.ignoreError = payload.ignoreError || 0;
    this.scriptParam = payload.scriptParam || '';
    this.timeout = payload.timeout;
    this.scriptSource = payload.scriptSource;
    this.scriptVersionId = payload.scriptVersionId;
    this.secureParam = payload.secureParam;
    this.content = payload.content;
    this.scriptLanguage = payload.scriptLanguage || 1;
    this.account = payload.account;
    this.status = payload.status;
    this.rollingEnabled = Boolean(payload.rollingEnabled);

    this.executeTarget = new TaskHostNodeModel(payload.executeTarget || {});
    this.rollingConfig = this.initRollingConfig(payload.rollingConfig);
  }

  /**
     * @desc 脚本内容引用的公共脚本
     * @returns { Boolean }
     */
  get isReferPublicScript() {
    return this.scriptSource === TaskInstanceDetailStepScript.TYPE_SOURCE_PUBLIC;
  }

  /**
     * @desc 引用脚本被禁用
     * @returns { Boolean }
     */
  get isDisabled() {
    return this.status === TaskInstanceDetailStepScript.STATUS_SCRIPT_DISABLED;
  }

  /**
     * @desc 引用脚本有更新
     * @returns { Boolean }
     */
  get isNeedUpdate() {
    return this.status === TaskInstanceDetailStepScript.STATUS_SCRIPT_NEED_UPDATE;
  }

  /**
     * @desc 脚本状态展示文本
     * @returns { String }
     */
  get scriptStatusHtml() {
    if (this.status === TaskInstanceDetailStepScript.STATUS_SCRIPT_NEED_UPDATE) {
      // eslint-disable-next-line max-len
      return `<i class="job-icon job-icon-script-update" tippy-tips="${I18n.t('引用脚本待更新')}" style="color: #EA3636"></i>`;
    }
    if (this.status === TaskInstanceDetailStepScript.STATUS_SCRIPT_DISABLED) {
      // eslint-disable-next-line max-len
      return `<i class="job-icon job-icon-script-disable" tippy-tips="${I18n.t('引用脚本被禁用')}" style="color: #EA3636"></i>`;
    }
    return '';
  }

  /**
     * @desc 脚本来源展示文本
     * @returns { String }
     */
  get scriptSourceText() {
    return this.scriptSource === TaskInstanceDetailStepScript.TYPE_SOURCE_LOCAL ? I18n.t('手工录入') : I18n.t('脚本引用');
  }

  /**
     * @desc 忽略错误展示文本
     * @returns { String }
     */
  get ignoreErrorText() {
    return this.ignoreError === 0 ? I18n.t('不忽略') : I18n.t('自动忽略错误');
  }

  /**
     * @desc 处理滚动执行配置
     * @param { Object } rollingConfig 滚动执行配置
     * @returns { Object }
     */
  initRollingConfig(rollingConfig) {
    const config = {
      expr: '',
      mode: 1,
    };
    if (rollingConfig) {
      const {
        expr = '',
        mode = 0,
      } = rollingConfig;
      Object.assign(config, {
        expr,
        mode,
      });
    }
    return config;
  }
}
