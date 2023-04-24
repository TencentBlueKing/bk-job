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

/* eslint-disable max-len */
class TaskPlan extends ModuleBase {
  constructor() {
    super();
    this.module = '/job-manage/web';
  }

  // 获取执行方案基本信息列表
  getAllPlanOfTemplate(params = {}, payload = {}) {
    return Request.get(`${this.path}/task/plan/${params.id}`, {
      payload,
    });
  }

  // 批量获取作业模板的执行方案列表
  getAllTemplatePlan(params = {}) {
    return Request.get(`${this.path}/task/plan:batchGet`, {
      params,
    });
  }

  // 获取执行方案信息
  getDetail(params = {}, payload = {}) {
    const { templateId, id } = params;
    return Request.get(`${this.path}/task/plan/${templateId}/${id}`, {
      payload,
    });
  }

  // 更新执行方案
  update(params = {}) {
    const realParams = { ...params };
    delete realParams.templateId;
    delete realParams.id;

    return Request.put(`${this.path}/task/plan/${params.templateId}/${params.id}`, {
      params: realParams,
    });
  }

  batchUpdateVariable(params = {}) {
    return Request.post(`${this.path}/task/plan/batch_update_variable`, { params });
  }

  // 删除执行方案
  delete(params = {}) {
    const { templateId, id } = params;
    return Request.delete(`${this.path}/task/plan/${templateId}/${id}`, {});
  }

  // 获取调试信息
  getDebugInfo(params = {}, payload = {}) {
    return Request.get(`${this.path}/task/plan/${params.templateId}/debug`, {
      payload,
    });
  }

  // 批量获取执行方案基础信息
  getPlansBasicInfo(params = {}) {
    return Request.get(`${this.path}/task/plan`, { params });
  }

  // 获取业务下的执行方案列表
  getAllPlan(params = {}) {
    return Request.get(`${this.path}/task/plan/list`, { params });
  }

  // 同步执行方案
  updateSyncInfo(params = {}) {
    return Request.post(`${this.path}/task/plan/${params.templateId}/${params.planId}/sync?templateVersion=${params.templateVersion}`, {});
  }

  // 获取执行方案同步信息
  getSyncDataById(params = {}, payload = {}) {
    return Request.get(`${this.path}/task/plan/${params.templateId}/${params.id}/sync_info`, {
      payload,
    });
  }

  // 检查执行方案名称是否已占用
  getCheckResult(payload) {
    const params = { ...payload };
    delete params.templateId;
    delete params.planId;
    return Request.get(`${this.path}/task/plan/${payload.templateId}/${payload.planId}/check_name`, {
      params,
    });
  }

  // 新增收藏
  updateFavorite(params = {}) {
    return Request.put(`${this.path}/task/plan/${params.templateId}/${params.id}/favorite`);
  }

  // 删除收藏
  deleteFavorite(params = {}) {
    return Request.delete(`${this.path}/task/plan/${params.templateId}/${params.id}/favorite`);
  }

  // 根据执行方案 ID 拉基本信息
  getDataById(params = {}) {
    return Request.get(`${this.module}/task/plan/${params.id}`);
  }
}

export default new TaskPlan();
