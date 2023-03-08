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

/* eslint-disable no-param-reassign */
import AccountRuleModel from '@model/account-rule';

import QueryGlobalSettingSource from '../source/query-global-setting';

export default {
  fetchAllNameRule() {
    return QueryGlobalSettingSource.getAllNameRule()
      .then(({ data }) => {
        let { currentRules, defaultRules } = data;
        if (currentRules.length < 1) {
          currentRules = defaultRules;
        }
        currentRules = currentRules.map(item => new AccountRuleModel(item));
        defaultRules = defaultRules.map(item => new AccountRuleModel(item));
        return {
          currentRules,
          defaultRules,
        };
      });
  },
  fetchActiveNotifyChannel() {
    return QueryGlobalSettingSource.getAllNotifyChannel()
      .then(({ data }) => data.filter(_ => _.isActive));
  },
  fetchDangerousRules() {
    return QueryGlobalSettingSource.getAllDangerousRules()
      .then(({ data }) => data);
  },
  fetchAdminIdentity() {
    return QueryGlobalSettingSource.getAdminIdentity()
      .then(({ data }) => data);
  },
  fetchApplyBusinessUrl(params) {
    return QueryGlobalSettingSource.getApplyBusinessUrl(params)
      .then(({ data }) => data);
  },
  fetchCMDBAppIndexUrl() {
    return QueryGlobalSettingSource.getCMDBAppIndexUrl()
      .then(({ data }) => data);
  },
  fetchCMDBUrl() {
    return QueryGlobalSettingSource.getCMDBUrl()
      .then(({ data }) => data);
  },
  fetchFooterConfig() {
    return QueryGlobalSettingSource.getTitleAndFooter()
      .then(({ data }) => {
        const { footerLink, footerCopyRight } = data;
        return {
          footerLink,
          footerCopyRight,
        };
      });
  },
  fetchTitleConfig() {
    return QueryGlobalSettingSource.getTitleAndFooter()
      .then(({ data }) => {
        const { titleHead, titleSeparator } = data;
        return {
          titleHead,
          titleSeparator,
        };
      });
  },
  fetchRelatedSystemUrls() {
    return QueryGlobalSettingSource.getRelatedSystemUrls()
      .then(({ data }) => ({
        ...data,
        BK_CMDB_APP_INDEX_URL: data.BK_CMDB_APP_INDEX_URL.replace('{appId}', window.PROJECT_CONFIG.SCOPE_ID),
      }));
  },
  fetchJobConfig(params = {}) {
    return QueryGlobalSettingSource.getJobConfig(params)
      .then(({ data }) => data);
  },
};
