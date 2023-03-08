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

const STATUS_DRAFT = 0; // 未上线
const STATUS_ONLINE = 1; // 已上线
const STATUS_OFFLINE = 2; // 已下线
const STATUS_DISABLED = 3; // 已禁用

const SYNC_STATUS_SUCCESS = 1;
const SYNC_STATUS_FAILED = 2;

export default class ScriptSync extends Model {
  static STATUS_TEXT_MAP = {
    [STATUS_DRAFT]: I18n.t('未上线'),
    [STATUS_ONLINE]: I18n.t('已上线'),
    [STATUS_OFFLINE]: I18n.t('已下线'),
    [STATUS_DISABLED]: I18n.t('已禁用'),
  };

  static SYNC_STATUS_ICON_MAP = {
    [SYNC_STATUS_SUCCESS]: 'sync-success',
    [SYNC_STATUS_FAILED]: 'sync-failed',
  };

  constructor(payload) {
    super();
    this.templateId = payload.templateId;
    this.templateName = payload.templateName;
    this.scriptId = payload.scriptId;
    this.scriptName = payload.scriptName;
    this.scriptStatus = payload.scriptStatus;
    this.scriptStatusDesc = payload.scriptStatusDesc;
    this.canEdit = payload.canEdit;
    this.scriptVersion = payload.scriptVersion;
    this.scriptVersionId = payload.scriptVersionId;
    this.stepId = payload.stepId;
    this.stepName = payload.stepName;
    this.scopeType = payload.scopeType;
    this.scopeId = payload.scopeId;
    this.failMsg = payload.failMsg || '';
    this.syncStatus = payload.syncStatus;
  }

  /**
     * @desc 同步失败
     * @returns { Boolean }
     */
  get isSyncFailed() {
    return this.syncStatus === SYNC_STATUS_FAILED;
  }

  /**
     * @desc 脚本状态 tag
     * @returns { HTMLElement }
     */
  get statusHtml() {
    let styles = 'display: inline-block; padding: 0 6px; line-height: 18px; font-size: 12px; border-radius: 2px;';
    if (this.scriptStatus === STATUS_ONLINE) {
      styles += 'background: #E5F6EA; color: #3FC06D';
    } else {
      styles += 'background: #F0F1F5; color: #979BA5';
    }
    return `<span style="${styles}">${this.scriptStatusDesc}</span>`;
  }

  /**
     * @desc 同步状态icon展示
     * @returns { String }
     */
  get syncIcon() {
    return ScriptSync.SYNC_STATUS_ICON_MAP[this.syncStatus];
  }

  /**
     * @desc 同步状态
     * @returns { String }
     */
  get syncStatusMsg() {
    if (this.syncStatus === 1) {
      return I18n.t('同步成功');
    }
    if (this.syncStatus === 2) {
      return I18n.t('同步失败');
    }
    return I18n.t('正在同步');
  }

  /**
     * @desc 同步失败重试按钮展示
     * @returns { String }
     */
  get syncTry() {
    return [
      this.syncStatus === 2 ? 'try-show' : 'try-hide',
    ];
  }
}
