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

import TagModel from '@model/tag';

import {
  formatScriptTypeValue,
} from '@utils/assist';

import BaseModel from './base';

export default class Script extends BaseModel {
  constructor(payload) {
    super();
    this.scopeType = payload.scopeType;
    this.scopeId = payload.scopeId;
    this.id = payload.id || 0;
    this.name = payload.name;
    this.category = payload.category;
    this.content = payload.content;
    this.createTime = payload.createTime;
    this.creator = payload.creator;
    this.description = payload.description;
    this.typeName = payload.typeName;
    this.lastModifyTime = payload.lastModifyTime;
    this.lastModifyUser = payload.lastModifyUser;
    this.publicScript = Boolean(payload.publicScript);
    this.relatedTaskPlanNum = payload.relatedTaskPlanNum || 0;
    this.relatedTaskTemplateNum = payload.relatedTaskTemplateNum || 0;
    this.scriptVersionId = payload.scriptVersionId;
    this.status = payload.status || 0;
    this.statusDesc = payload.statusDesc;
    this.syncEnabled = Boolean(payload.syncEnabled);
    this.type = payload.type || 0;
    this.typeName = formatScriptTypeValue(this.type);
    this.version = payload.version;
    this.versionDesc = payload.versionDesc;
    this.scriptVersions = this.initScriptVersion(payload.scriptVersions);
    this.tags = this.initTag(payload.tags);

    // 权限
    this.canClone = payload.canClone;
    this.canManage = payload.canManage;
    this.canView = payload.canView;
  }

  /**
     * @desc 脚本版本显示文本
     * @returns { String }
     */
  get versionText() {
    return this.getDefaultValue(this.version);
  }

  /**
     * @desc 已上线
     * @returns { Boolean }
     */
  get isOnline() {
    return this.status === Script.STATUS_ONLINE;
  }

  /**
     * @desc 未上线
     * @returns { Boolean }
     */
  get isDraft() {
    return this.status === Script.STATUS_DRAFT;
  }

  /**
     * @desc 禁用
     * @returns { Boolean }
     */
  get isDisabled() {
    return this.status === Script.STATUS_DISABLED;
  }

  /**
     * @desc 无法执行上线操作
     * @returns { Boolean }
     */
  get isDisabledOnline() {
    return [
      Script.STATUS_ONLINE, Script.STATUS_DISABLED,
    ].includes(this.status);
  }

  /**
     * @desc 脚本是否可以被删除
     * @returns { Boolean }
     */
  get isEnableRemove() {
    return this.relatedTaskPlanNum < 1 && this.relatedTaskTemplateNum < 1;
  }

  /**
     * @desc 脚本版本是否可以本删除
     * @returns { Boolean }
     */
  get isVersionEnableRemove() {
    return ![
      Script.STATUS_ONLINE,
    ].includes(this.status);
  }

  /**
     * @desc 是否可以执行
     * @returns { Boolean }
     */
  get isExecuteDisable() {
    return !this.version;
  }

  /**
     * @desc 是否可禁用
     * @returns { Boolean }
     */
  get isBanable() {
    return [
      Script.STATUS_ONLINE,
      Script.STATUS_OFFLINE,
    ].includes(this.status);
  }

  /**
     * @desc tag 显示文本
     * @returns { String }
     */
  get tagsText() {
    return this.getDefaultValue(this.tags.map(tag => tag.name).join('，'));
  }

  /**
     * @desc 脚本状态 TAG
     * @returns { HTMLElement }
     */
  get statusHtml() {
    let styles = 'display: inline-block; padding: 0 8px; line-height: 18px; font-size: 12px; border-radius: 2px;';

    switch (this.status) {
      case Script.STATUS_INVALID:
        styles += 'background: #FFE8C3; color: #FF9C01';
        break;
      case Script.STATUS_ONLINE:
        styles += 'background: #E5F6EA; color: #3FC06D';
        break;
      default:
        styles += 'background: #F0F1F5; color: #979BA5';
    }
    return [
      `<span style="${styles}" data-script-status="${this.status}">`,
      Script.STATUS_TEXT_MAP[this.status],
      '</span>',
    ].join('');
  }

  /**
     * @desc 处理脚本 TAG 数据
     * @param { Array } tags
     * @returns { Array }
     */
  initTag(tags) {
    if (!tags) {
      return [];
    }
    return tags.map(item => new TagModel(item));
  }

  /**
     * @desc 处理脚本版本数据
     * @param { Array } versions
     * @returns { Array }
     */
  initScriptVersion(versions) {
    if (!versions) {
      return [];
    }
    return versions.map(item => new Script(item));
  }
}
