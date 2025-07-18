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

class ScriptTemplate {
  constructor() {
    this.module = 'job-manage/web/customSettings/scriptTemplate';
  }

  // 获取用户自定义的脚本模板
  getOriginalData(params = {}) {
    return Request.get(`${this.module}`, {
      params,
    });
  }

  // 保存用户自定义的脚本模板
  updateOriginalData(params = {}) {
    return Request.post(`${this.module}`, {
      params,
    });
  }

  // 渲染自定义的脚本模板
  getRenderDataWithVariable(params = {}) {
    return Request.post(`${this.module}/render`, {
      params,
    });
  }

  // 获取渲染后的用户自定义的脚本模板
  getData(params = {}) {
    return Request.get(`${this.module}/rendered`, {
      params: {
        ...params,
        scopeType: window.PROJECT_CONFIG.SCOPE_TYPE,
        scopeId: window.PROJECT_CONFIG.SCOPE_ID,
      },
    });
  }

  // 获取用户自定义的脚本模板变量
  getVariables(params = {}) {
    return Request.get(`${this.module}/variables`, {
      params,
    });
  }
}

export default new ScriptTemplate();
