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

export default class WhiteIp extends Model {
  constructor(payload) {
    super();
    this.actionScopeList = payload.actionScopeList;
    this.scopeList = payload.scopeList || [];
    this.allScope = Boolean(payload.allScope);
    this.createTime = payload.createTime;
    this.creator = payload.creator;
    this.id = payload.id;
    this.hostList = payload.hostList || [];
    this.lastModifier = payload.lastModifier;
    this.lastModifyTime = payload.lastModifyTime;
    this.remark = payload.remark;
    this.canManage = payload.canManage;
  }

  get scopeText() {
    return this.actionScopeList.map(item => item.name).join('，');
  }

  get appText() {
    if (this.scopeList.length < 1) {
      return I18n.t('全业务');
    }
    return this.scopeList.map(_ => _.name).join('，');
  }
}
