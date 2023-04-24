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

import {
  prettyDateTimeFormat,
} from '@utils/assist';

import I18n from '@/i18n';

export default class DangerousRecord {
  static scriptTypeMap = {
    1: 'Shell',
    2: 'Bat',
    3: 'Perl',
    4: 'Python',
    5: 'Powershell',
    6: 'Sql',
  };

  static actionMap = {
    1: `${I18n.t('detectRecords.扫描')}`,
    2: `${I18n.t('detectRecords.拦截')}`,
  };

  static startupModeMap = {
    1: 'Web',
    2: 'API',
    3: 'Cron',
  };

  constructor(payload) {
    this.action = payload.action;
    this.scopeType = payload.scopeType;
    this.scopeId = payload.scopeId;
    this.scopeName = payload.scopeName;
    this.checkResultItems = payload.checkResultItems;
    this.client = payload.client;
    this.createTime = payload.createTime;
    this.id = payload.id;
    this.operator = payload.operator;
    this.ruleExpression = payload.ruleExpression;
    this.ruleId = payload.ruleId;
    this.scriptContent = payload.scriptContent;
    this.scriptLanguage = payload.scriptLanguage;
    this.startupMode = payload.startupMode;
  }

  get getSctiptTypeHtml() {
    return DangerousRecord.scriptTypeMap[this.scriptLanguage];
  }

  get getActionHtml() {
    let styles = 'display: inline-block; padding: 0 5px; line-height: 16px; font-size: 12px;';
    if (this.action === 1) {
      styles += 'background: #f0f1f5; color: #979ba5';
    } else {
      styles += 'background: #ffebeb; color: #ea3636';
    }
    return `<span style="${styles}">${DangerousRecord.actionMap[this.action]}<span>`;
  }

  get getCreatTimes() {
    return prettyDateTimeFormat(this.createTime);
  }

  get getStartupModeHtml() {
    return DangerousRecord.startupModeMap[this.startupMode];
  }
}
