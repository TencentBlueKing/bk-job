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
import DangerousRuleModel from '@model/dangerous-rule';

import I18n from '@/i18n';

import DangerousRuleSource from '../source/dangerous-rule';

export default {
  fetchList(params = {}, payload = {}) {
    return DangerousRuleSource.getData(params, payload)
      .then(({ data }) => data.map(item => new DangerousRuleModel(item)));
  },
  update(params) {
    return DangerousRuleSource.update(params)
      .then(({ data }) => data);
  },
  remove(params) {
    return DangerousRuleSource.removeById(params)
      .then(({ data }) => data);
  },
  updateSort(params) {
    return DangerousRuleSource.move(params)
      .then(({ data }) => data);
  },
  fetchActionList() {
    return Promise.resolve([
      {
        id: 1,
        name: I18n.t('扫描'),
      },
      {
        id: 2,
        name: I18n.t('拦截'),
      },
    ]);
  },
};
