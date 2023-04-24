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

/* eslint-disable no-underscore-dangle */
import I18n from '@/i18n';

const STATUS_CRON_JOB_LOADING = -1;
const STATUS_DEFAULT = 0;
const STATUS_CONFIRM_QUEUE = 2;
const STATUS_CONFIRM_PENDGING = 3;
const STATUS_CONFIRM_FAILED = 4;
const STATUS_CONFIRMED = 5;
const STATUS_SYNC_QUEUE = 6;
const STATUS_SYNC_PENDING = 7;
const STATUS_SYNC_FAILED = 8;
const STATUS_SYNCED = 9;
const STATUS_PERMISSION = 10;

export default class SyncPlan {
  static STATUS_CRON_JOB_LOADING = STATUS_CRON_JOB_LOADING;
  static STATUS_DEFAULT = STATUS_DEFAULT;
  static STATUS_CONFIRM_QUEUE = STATUS_CONFIRM_QUEUE;
  static STATUS_CONFIRM_PENDGING = STATUS_CONFIRM_PENDGING;
  static STATUS_CONFIRM_FAILED = STATUS_CONFIRM_FAILED;
  static STATUS_CONFIRMED = STATUS_CONFIRMED;
  static STATUS_SYNC_QUEUE = STATUS_SYNC_QUEUE;
  static STATUS_SYNC_PENDING = STATUS_SYNC_PENDING;
  static STATUS_SYNC_FAILED = STATUS_SYNC_FAILED;
  static STATUS_SYNCED = STATUS_SYNCED;
  static STATUS_PERMISSION = STATUS_PERMISSION;

  static STATUS_TEXT_MAP = {
    [STATUS_CRON_JOB_LOADING]: '',
    [STATUS_DEFAULT]: I18n.t('未就绪'),
    [STATUS_CONFIRM_QUEUE]: I18n.t('等待确认中'),
    [STATUS_CONFIRM_PENDGING]: I18n.t('确认中'),
    [STATUS_CONFIRM_FAILED]: I18n.t('未就绪，确认失败'),
    [STATUS_CONFIRMED]: I18n.t('已就绪'),
    [STATUS_SYNC_QUEUE]: I18n.t('等待同步中'),
    [STATUS_SYNC_PENDING]: I18n.t('同步中'),
    [STATUS_SYNC_FAILED]: I18n.t('同步失败'),
    [STATUS_SYNCED]: I18n.t('同步成功'),
    [STATUS_PERMISSION]: I18n.t('无同步权限'),
  };

  static STATUS_ICON_MAP = {
    [STATUS_CRON_JOB_LOADING]: 'sync-pending',
    [STATUS_DEFAULT]: 'sync-default',
    [STATUS_CONFIRM_QUEUE]: 'sync-default',
    [STATUS_CONFIRM_PENDGING]: 'sync-pending',
    [STATUS_CONFIRM_FAILED]: 'sync-failed',
    [STATUS_CONFIRMED]: 'sync-success',
    [STATUS_SYNC_QUEUE]: 'sync-default',
    [STATUS_SYNC_PENDING]: 'sync-pending',
    [STATUS_SYNC_FAILED]: 'sync-failed',
    [STATUS_SYNCED]: 'sync-success',
    [STATUS_PERMISSION]: 'sync-default',
  };

  constructor(payload) {
    this.id = payload.id;
    this.name = payload.name;
    this.createTime = payload.createTime;
    this.creator = payload.creator;
    this.cronJobCount = payload.cronJobCount || 0;
    this.hasCronJob = payload.hasCronJob || false;
    this.lastModifyTime = payload.lastModifyTime;
    this.lastModifyUser = payload.lastModifyUser;
    this.needUpdate = payload.needUpdate;
    this.stepList = payload.stepList || [];
    this.templateId = payload.templateId;
    this.templateName = payload.templateName;
    this.templateVersion = payload.templateVersion;
    this.variableList = Object.freeze(payload.variableList || []);
    this.version = payload.version;
    // 权限
    this.canDelete = payload.canDelete;
    this.canEdit = payload.canEdit;
    this.canView = payload.canView;

    // 私有属性
    this._cronJonList = []; // 确认的定时任务信息
    this._error = '';
    this._status = SyncPlan.STATUS_DEFAULT;
    if (this.cronJobCount < 1 || !this.needUpdate) {
      this._status = SyncPlan.STATUS_CONFIRMED;
    }
    if (!this.canEdit) {
      this._status = SyncPlan.STATUS_PERMISSION;
    }
  }

  get cronJobInfoList() {
    return this._cronJonList;
  }

  set cronJobInfoList(value) {
    this._cronJonList = value;
  }

  get error() {
    return this._error;
  }

  set error(value) {
    this._error = value;
  }

  // 执行方案同步状态
  get status() {
    return SyncPlan.STATUS_TEXT_MAP[this._status];
  }

  set status(value) {
    // 没有编辑权限
    if (!this.canEdit) {
      return;
    }
    // 不需要同步，状态不支持变更
    if (!this.needUpdate) {
      return;
    }
    // 非错误状态——重置错误信息
    if (![
      SyncPlan.STATUS_CONFIRM_FAILED,
      SyncPlan.STATUS_SYNC_FAILED,
    ].includes(value)) {
      this._error = '';
    }
    // 如果执行方案无需确认定时任务或者已经确认过——不更新确认阶段的状态
    if ([
      STATUS_CONFIRMED,
    ].includes(this._status)
            && [
              SyncPlan.STATUS_CONFIRM_QUEUE,
              SyncPlan.STATUS_CONFIRM_PENDGING,
              SyncPlan.STATUS_CONFIRM_FAILED,
            ].includes(value)) {
      return;
    }
    this._status = value;
  }

  // 状态描述显示内容
  get statusHtml() {
    let errorInfo = '';
    if ([
      SyncPlan.STATUS_CONFIRM_FAILED,
      SyncPlan.STATUS_SYNC_FAILED,
    ].includes(this._status) && this._error) {
      errorInfo = `tippy-tips="${this._error}"`;
    }
    return `<span ${errorInfo}>${SyncPlan.STATUS_TEXT_MAP[this._status]}</span>`;
  }

  // 状态icon
  get statusIcon() {
    return SyncPlan.STATUS_ICON_MAP[this._status];
  }

  // 执行方案的定时任务加载中
  get isCronJobLoading() {
    return this._status === SyncPlan.STATUS_CRON_JOB_LOADING;
  }

  // 无需确认
  get isPassConfirm() {
    // 没有定时任务或者执行方案不需要同步——无需确认
    return this.cronJobCount < 1 || !this.needUpdate;
  }

  // 无需同步
  get isSyncDisabled() {
    return !this.needUpdate;
  }

  // 已确认
  get isConfirmed() {
    return ![
      SyncPlan.STATUS_DEFAULT,
      SyncPlan.STATUS_CONFIRM_QUEUE,
      SyncPlan.STATUS_CONFIRM_PENDGING,
      SyncPlan.STATUS_CONFIRM_FAILED,
    ].includes(this._status);
  }

  // 失败重试
  get isRetryEnable() {
    return this._status === SyncPlan.STATUS_SYNC_FAILED;
  }

  // 不能查看差异的原因
  get disableDiffTips() {
    if (!this.needUpdate) {
      return I18n.t('方案已是最新版，无差异');
    }
    return '';
  }

  // 无需确认的原因
  get disableConfirmTips() {
    if (!this.needUpdate) {
      return I18n.t('该方案已同步，无需处理');
    }
    if (this.cronJobCount < 1) {
      return I18n.t('无关联定时任务，系统自动确认');
    }
    return '';
  }

  // 确认定时任务进度
  get confirmProcessText() {
    if (this.isConfirmed) {
      return `${this.cronJobCount}/${this.cronJobCount}`;
    }
    let hasConfirmedNums = 0;
    this._cronJonList.forEach((currentCronJob) => {
      if (currentCronJob.hasConfirm || !currentCronJob.enable) {
        hasConfirmedNums += 1;
      }
    });

    return `${hasConfirmedNums}/${this.cronJobCount}`;
  }
}
