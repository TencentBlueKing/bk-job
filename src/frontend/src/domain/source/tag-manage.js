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

class TagManage extends ModuleBase {
  constructor() {
    super();
    this.module = '/job-manage/web/tag';
  }

  // 获取所有tag
  getAll(params = {}) {
    return Request.get(`${this.path}/tag/list`, {
      params,
      cache: 2000,
    });
  }

  getAllWithBasic(params = {}) {
    return Request.get(`${this.path}/tag/basic/list`, {
      params,
      cache: 2000,
    });
  }

  update(params = {}) {
    const realParams = { ...params };
    delete realParams.id;

    return Request.put(`${this.path}/tag/${params.id}`, {
      params: realParams,
    });
  }

  create(params = {}) {
    return Request.post(`${this.path}/tag`, {
      params,
    });
  }

  remove(params = {}) {
    return Request.delete(`${this.path}/tag/${params.id}`);
  }

  batchUpdate(params = {}) {
    const realParams = { ...params };
    delete realParams.id;

    return Request.put(`${this.path}/tag/${params.id}/resources`, {
      params: realParams,
    });
  }

  checkName(params = {}) {
    const realParams = { ...params };
    delete realParams.id;

    return Request.get(`${this.path}/tag/${params.id}/checkName`, {
      params: realParams,
    });
  }
}

export default new TagManage();
