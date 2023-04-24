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

import GlobalSettingSource from '../source/global-setting';

export default {
  fetchAllNameRule(params, payload) {
    return GlobalSettingSource.getAllNameRule(params, payload)
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
  updateNameRules(params, payload) {
    return GlobalSettingSource.updateNameRules(params, payload)
      .then(({ data }) => data);
  },
  fetchHistroyExpire(params, payload) {
    return GlobalSettingSource.getHistroyExpire(params, payload)
      .then(({ data }) => data);
  },
  updateHistroyExpire(params = {}) {
    return GlobalSettingSource.updateHistroyExpire(params)
      .then(({ data }) => data);
  },
  fetchAllUserBlacklist(params, payload) {
    return GlobalSettingSource.getAllUserBlacklist(params, payload)
      .then(({ data }) => data);
  },
  updateUserBlacklist(params, payload) {
    return GlobalSettingSource.updateUserBlacklist(params, payload);
  },
  fetchAllNotifyChannel(params, payload) {
    return GlobalSettingSource.getAllNotifyChannel(params, payload)
      .then(({ data }) => data);
  },
  fetchActiveNotifyChannel(params, payload) {
    return GlobalSettingSource.getAllNotifyChannel(params, payload)
      .then(({ data }) => data.filter(_ => _.isActive));
  },
  updateNotifyChannel(params = {}) {
    return GlobalSettingSource.updateNotifyChannel(params);
  },

  fetchUserList(params, payload) {
    return GlobalSettingSource.getUserByName(params, payload)
      .then(({ data }) => data);
  },
  fetchTitleAndFooterConfig(params, payload) {
    return GlobalSettingSource.getTitleAndFooterWithDefault(params, payload)
      .then(({ data }) => data);
  },
  updateTitleAndFooterConfig(params = {}) {
    return GlobalSettingSource.updateTitleAndFooterConfig(params);
  },
  fetchAllNotifyChannelConfig(params, payload) {
    return GlobalSettingSource.getAllNotifyChannelConfig(params, payload)
      .then(({ data }) => data);
  },
  fetchChannelTemplate(params, payload) {
    return GlobalSettingSource.getChannelTemplate(params, payload)
      .then(({ data }) => data);
  },
  updateNotifyTemplate(params = {}) {
    return GlobalSettingSource.updateNotifyTemplate(params)
      .then(({ data }) => data);
  },
  sendNotifyPreview(params = {}) {
    return GlobalSettingSource.sendNotifyPreview(params)
      .then(({ data }) => data);
  },
  fetchFileUpload(params) {
    return GlobalSettingSource.getFileUpload(params)
      .then(({ data }) => data);
  },
  saveFileUpload(params) {
    return GlobalSettingSource.saveFileUpload(params)
      .then(({ data }) => data);
  },
};
