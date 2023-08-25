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

import Request from '@utils/request';

import ModuleBase from './module-base';

class QueryGlobalSetting extends ModuleBase {
  constructor() {
    super();
    this.module = '/job-manage/web/queryGlobalSettings';
  }

  // 获取账号命名规则
  getAllNameRule() {
    return Request.get(`${this.module}/account/nameRules`);
  }

  // 获取通知渠道列表及生效状态
  getAllNotifyChannel() {
    return Request.get(`${this.module}/notify/listChannels`, {
      cache: 5000,
    });
  }

  // 获取高危语句规则列表
  getAllDangerousRules() {
    return Request.get(`${this.module}/security/dangerousRules/list`, {
      cache: 5000,
    });
  }

  // 判断用户是否为超级管理员
  getAdminIdentity() {
    return Request.get(`${this.module}/isAdmin`, {
      cache: true,
    });
  }

  // 获取页面申请业务地址
  getApplyBusinessUrl(params = {}) {
    return Request.get(`${this.module}/applyBusinessUrl`, {
      params,
    });
  }

  // 获取CMDB业务首页地址
  getCMDBAppIndexUrl() {
    return Request.get(`${this.path}/cmdbAppIndexUrl`);
  }

  // 获取CMDB服务跳转地址
  getCMDBUrl() {
    return Request.get(`${this.module}/cmdbServerUrl`);
  }

  // 获取Title与Footer
  getTitleAndFooter(params, payload) {
    return Request.get(`${this.module}/titleFooter`, {
      cache: true,
      ...payload,
    });
  }

  // 周边系统跳转路径
  getRelatedSystemUrls(params = {}) {
    return Request.get(`${this.module}/relatedSystemUrls`, {
      params,
      cache: true,
    });
  }

  // 公开配置
  getJobConfig(params = {}) {
    return Request.get(`${this.module}/jobConfig`, {
      params,
      cache: 3000,
    });
  }
}

export default new QueryGlobalSetting();
