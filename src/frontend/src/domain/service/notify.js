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
import NotifySource from '../source/notify';

export default {
  fetchBlacklist(params) {
    return NotifySource.getBlacklist(params)
      .then(({ data }) => data);
  },
  blacklistUpdate(params) {
    return NotifySource.updateBlacklist(params);
  },
  blacklistDelete(params) {
    return NotifySource.deleteBlacklistMember(params);
  },
  fetchPoliciesList(params) {
    return NotifySource.getPoliciesList(params)
      .then(({ data }) => data);
  },
  defaultPoliciesUpdate(params) {
    return NotifySource.updateDefaultPolicies(params);
  },
  fetchAllUsers(params = {}) {
    return NotifySource.getAllUsers(params)
      .then(({ data }) => data);
  },
  /**
     * @desc 用户搜索的用户列表
     * @param { String } prefixStr
     */
  fetchUsersOfSearch(prefixStr = '') {
    return NotifySource.getAllUsers({
      prefixStr,
      offset: 0,
      limit: 10,
    }).then(({ data }) => data.map(_ => ({
      id: _.englishName,
      name: _.englishName,
    })));
  },
  fetchPageTemplate() {
    return NotifySource.getPageTemplate()
      .then(({ data }) => data);
  },
  fetchAllChannel() {
    return NotifySource.getAllChannel()
      .then(({ data }) => data);
  },
  fetchAvailableChannel() {
    return NotifySource.getAvailableChannel()
      .then(({ data }) => data);
  },
  fetchRoleList() {
    return NotifySource.getAllRole()
      .then(({ data }) => data);
  },
};
