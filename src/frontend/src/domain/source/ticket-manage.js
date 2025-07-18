/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

class TicketManage extends ModuleBase {
  constructor() {
    super();
    this.module = 'job-manage/web/credentials';
  }

  // 获取凭证列表
  getAll(params = {}) {
    return Request.get(`${this.path}/list`, {
      params,
    });
  }
  // 获取凭证列表
  getBasicInfoList(params = {}) {
    return Request.get(`${this.path}/basicInfo/list`, {
      params,
    });
  }

  // 新建凭证
  create(params = {}) {
    return Request.post(`${this.path}`, {
      params,
    });
  }

  // 修改凭证
  update(params) {
    return Request.put(`${this.path}/${params.id}`, {
      params,
    });
  }

  // 删除凭证
  delete({ id }) {
    return Request.delete(`${this.path}/ids/${id}`);
  }
}

export default new TicketManage();
