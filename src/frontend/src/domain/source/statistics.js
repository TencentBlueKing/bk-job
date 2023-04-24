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

class Statistics extends ModuleBase {
  constructor() {
    super();
    this.module = '/job-analysis/web/statistics';
  }

  // 查询某个统计量的分布信息
  getDistributionMetrics(params = {}) {
    const realParams = { ...params };
    delete realParams.metric;
    return Request.get(`${this.module}/distribution/metrics/${params.metric}`, {
      params: realParams,
    });
  }

  // 查询某个统计量的逐业务统计列表
  getListByPerAppMetrics(params = {}) {
    const realParams = { ...params };
    delete realParams.metric;
    return Request.get(`${this.module}/listByPerApp/metrics/${params.metric}`, {
      params: realParams,
    });
  }

  // 查询某种资源某个维度下的每日统计详情
  getReourcesDimensions(params = {}) {
    const realParams = { ...params };
    delete realParams.resource;
    delete realParams.dimension;
    return Request.get(`${this.module}/resources/${params.resource}/dimensions/${params.dimension}`, {
      params: realParams,
    });
  }

  // 查询某个统计量的统计信息
  getTotalMetrics(params = {}) {
    const realParams = { ...params };
    delete realParams.metric;
    return Request.get(`${this.module}/total/metrics/${params.metric}`, {
      params: realParams,
    });
  }

  // 查询某个统计量的趋势
  getTrendsMetrics(params = {}) {
    const realParams = { ...params };
    delete realParams.metric;
    return Request.get(`${this.module}/trends/metrics/${params.metric}`, {
      params: realParams,
    });
  }

  // 查询脚本引用统计信息
  getScriptCiteInfo(params = {}) {
    return Request.get(`${this.module}/script/citeInfo`, {
      params,
    });
  }

  // 查询统计数据起始日期
  getDateInfo(params = {}, payload = {}) {
    return Request.get(`${this.module}/info`, {
      params,
      payload,
    });
  }
}

export default new Statistics();
