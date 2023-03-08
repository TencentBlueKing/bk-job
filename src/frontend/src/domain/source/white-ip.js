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

class WhiteIp {
  constructor() {
    this.module = '/job-manage/web/whiteIP';
  }

  // 新增/更新IP白名单
  update(params) {
    return Request.post(`${this.module}/`, {
      params,
    });
  }

  // 获取业务下云区域列表
  getAllCloudArea() {
    return Request.get(`${this.module}/cloudAreas/list`);
  }

  // 获取IP白名单记录详情
  getDataById({ id }) {
    return Request.get(`${this.module}/ids/${id}`);
  }

  // 删除IP白名单
  deleteDataById({ id }) {
    return Request.delete(`${this.module}/ids/${id}`);
  }

  // 获取IP白名单列表
  getAll(params, payload = {}) {
    return Request.get(`${this.module}/list`, {
      params,
      payload,
    });
  }

  // 获取生效范围列表
  getActionScope() {
    return Request.get(`${this.module}/actionScope/list`);
  }
}

export default new WhiteIp();
