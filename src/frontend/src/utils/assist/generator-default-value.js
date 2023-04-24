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

import Cookie from 'js-cookie';

export const defaultValue = value => value || '--';

/**
 * @desc 生成默认名称
 * @param { String } prefixStr 名称前缀
 * @returns { String }
 */
export const genDefaultName = (prefixStr = 'auto') => {
  const formatStr = (str) => {
    if (String(str).length === 1) {
      return `0${str}`;
    }
    return str;
  };
  const d = new Date();
  const month = d.getMonth() + 1;

  const temp = [
    d.getFullYear(),
    formatStr(month),
    formatStr(d.getDate()),
    formatStr(d.getHours()),
    formatStr(d.getMinutes()),
    formatStr(d.getSeconds()),
    d.getMilliseconds(),
  ];
  return `${prefixStr}_${temp.join('')}`;
};

/**
 * @desc 生成默认脚本版本号
 * @returns { String }
 */
export const genDefaultScriptVersion = () => {
  const uid = Cookie.get('job_user');
  const formatStr = (str) => {
    if (String(str).length === 1) {
      return `0${str}`;
    }
    return str;
  };
  const d = new Date();

  const month = formatStr(d.getMonth() + 1);
  const date = formatStr(d.getDate());
  const hours = formatStr(d.getHours());
  const minutes = formatStr(d.getMinutes());
  const seconds = formatStr(d.getSeconds());
  const millSeconds = formatStr(d.getMilliseconds());

  const temp = [
    d.getFullYear(),
    month,
    date,
    hours,
    minutes,
    seconds,
    millSeconds,
  ];

  return `${uid}.${temp.join('')}`.slice(0, 30);
};
