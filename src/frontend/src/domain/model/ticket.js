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

const APP_ID_SECRET_KEY = 'APP_ID_SECRET_KEY';
const PASSWORD = 'PASSWORD';
const USERNAME_PASSWORD = 'USERNAME_PASSWORD';
const SECRET_KEY = 'SECRET_KEY';

export default class Ticket extends Model {
  static VOUCHER_TYPE_MAP = {
    [APP_ID_SECRET_KEY]: I18n.t('AppID+SecretKey'),
    [PASSWORD]: I18n.t('单一密码'),
    [USERNAME_PASSWORD]: I18n.t('用户名+密码'),
    [SECRET_KEY]: I18n.t('单一SecretKey'),
  };

  constructor(payload) {
    super();
    this.description = payload.description;
    this.id = payload.id;
    this.name = payload.name;
    this.createTime = payload.createTime;
    this.creator = payload.creator;
    this.lastModifyTime = payload.lastModifyTime;
    this.lastModifyUser = payload.lastModifyUser;
    this.type = payload.type;
    this.value1 = payload.value1;
    this.value2 = payload.value2;
    this.value3 = payload.value3;
    this.relatedNums = payload.relatedNums || 0;
    // 权限
    this.canManage = payload.canManage;
    this.canUse = payload.canUse;
    // 异步获取引用数量
    this.isRelatedLoading = true;
  }
}
