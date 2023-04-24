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

class FileSourceManage extends ModuleBase {
  constructor() {
    super();
    this.module = '/job-file-gateway/web/fileSource';
  }

  // 获取可使用的文件源列表
  getAvailableList(params = {}) {
    return Request.get(`${this.path}/available/list`, {
      params,
    });
  }

  // 检查文件源别名是否已存在（可用返回true）
  getAliasCheck(params = {}) {
    return Request.get(`${this.path}/checkAlias`, {
      params,
    });
  }

  // 获取可管理的工作台文件源列表
  getWorkTableList(params = {}) {
    return Request.get(`${this.path}/workTable/list`, {
      params,
    });
  }

  // 新增文件源
  addFileSource(params) {
    return Request.post(`${this.path}`, {
      params,
    });
  }

  // 更新文件源
  updateFileSource(params) {
    return Request.put(`${this.path}`, {
      params,
    });
  }

  // 获取文件源详情
  getFileSourceInfo({ id }) {
    return Request.get(`${this.path}/ids/${id}`);
  }

  // 删除文件源
  removeFileSource(params) {
    return Request.delete(`${this.path}/ids/${params.id}`);
  }

  // 启用/禁用文件源
  toggleEnable(params = {}) {
    return Request.put(`${this.path}/ids/${params.id}/enable?flag=${params.flag}`);
  }

  getParams(params = {}) {
    return Request.get(`${this.path}/fileSourceParams`, {
      params,
    });
  }
}

export default new FileSourceManage();
