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

export const prettyDateTimeFormat = (target) => {
  if (!target) {
    return '';
  }
  const formatStr = (str) => {
    if (String(str).length === 1) {
      return `0${str}`;
    }
    return str;
  };
  const d = new Date(target);
  const year = d.getFullYear();
  const month = formatStr(d.getMonth() + 1);
  const date = formatStr(d.getDate());
  const hours = formatStr(d.getHours());
  const minutes = formatStr(d.getMinutes());
  const seconds = formatStr(d.getSeconds());
  return `${year}-${month}-${date} ${hours}:${minutes}:${seconds}`;
};

export const prettyDateFormat = (target) => {
  if (!target) {
    return '';
  }
  const formatStr = (str) => {
    if (String(str).length === 1) {
      return `0${str}`;
    }
    return str;
  };
  const d = new Date(target);
  const year = d.getFullYear();
  const month = formatStr(d.getMonth() + 1);
  const date = formatStr(d.getDate());
  return `${year}-${month}-${date}`;
};

export const generatorDefaultCronTime = () => {
  const nextHourDate = new Date(Date.now() + 3600000);
  const formatStr = (str) => {
    if (String(str).length === 1) {
      return `0${str}`;
    }
    return str;
  };
  const d = new Date(nextHourDate);
  const year = d.getFullYear();
  const month = formatStr(d.getMonth() + 1);
  const date = formatStr(d.getDate());
  const hours = formatStr(d.getHours());

  return `${year}-${month}-${date} ${hours}:00:00`;
};

export const transformTimeFriendly = (target) => {
  const totalTime = parseFloat(target);
  if (totalTime < 60) {
    return `${totalTime}s`;
  }
  const dayUnit = 86400;
  const hourUnit = 3600;
  const minUnit = 60;
  const stack = [];
  const day = Math.floor(target / dayUnit);
  if (day) {
    stack.push(`${day}d`);
  }
  const hour = Math.floor((target % dayUnit) / hourUnit);
  if (hour) {
    stack.push(`${hour}h`);
  }
  const min = Math.floor((target % hourUnit) / minUnit);
  if (min) {
    stack.push(`${min}m`);
  }
  const second = Math.ceil(target % 60);
  stack.push(`${second}s`);
  return stack.join(' ');
};
