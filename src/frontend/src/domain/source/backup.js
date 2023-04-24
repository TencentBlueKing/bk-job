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

class Backup extends ModuleBase {
  constructor() {
    super();
    this.module = 'job-backup/web';
  }

  // 获取当前用户的导入/导出任务列表
  getData() {
    return Request.get(`${this.path}/backup`);
  }

  // 开始导出
  export(params = {}) {
    return Request.post(`${this.path}/backup/export`, {
      params,
    });
  }

  // 获取导出任务信息
  getExportById(params = {}) {
    return Request.get(`${this.path}/backup/export/${params.id}`);
  }

  // 终止导出任务
  deleteExportById(params = {}) {
    return Request.delete(`${this.path}/backup/export/${params.id}`);
  }

  // 完成导出任务
  exportComplete(params = {}) {
    return Request.post(`${this.path}/backup/export/${params.id}/complete`, {
      params,
    });
  }

  // 获取导入信息
  getImportById(params = {}) {
    return Request.get(`${this.path}/backup/import/${params.id}`);
  }

  // 开始导入
  import(params = {}) {
    const bodyParams = {
      ...params,
    };
    delete bodyParams.id;
    return Request.post(`${this.path}/backup/import/${params.id}`, {
      params: bodyParams,
    });
  }

  // 导入校验密码
  checkImportPassword(params = {}) {
    const realParams = { ...params };
    delete realParams.id;
    return Request.post(`${this.path}/backup/import/${params.id}/password`, {
      params: realParams,
    });
  }

  // 获取导入文件信息
  putImportFile(params = {}, payload = {}) {
    return Request.post(`${this.path}/backup/import/file`, {
      params,
      payload: {
        ...payload,
        timeout: 0,
      },
    });
  }

  // 下载导出文件
  getImportFile(params = {}) {
    return Request.download(`${this.path}/backup/export/${params.id}/download`);
  }
}

export default new Backup();
