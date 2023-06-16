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

import Node from './Node';

export const optimze = (fieldMap) => {
  const isAllValue = node => node.length === 1
    && node[0].type === Node.TYPE_ENUM
    && (node[0].value === '*' || node[0].value === '?');
  const prettyMap = {};

  prettyMap.month = isAllValue(fieldMap.month) ? [] : fieldMap.month;

  if (isAllValue(fieldMap.dayOfMonth) && isAllValue(fieldMap.month) && isAllValue(fieldMap.dayOfWeek)) {
    prettyMap.dayOfMonth = [];
    delete prettyMap.month;
  } else {
    if (!isAllValue(fieldMap.dayOfWeek)) {
      prettyMap.dayOfWeek = fieldMap.dayOfWeek;
    }
    if (!isAllValue(fieldMap.dayOfMonth)) {
      prettyMap.dayOfMonth = fieldMap.dayOfMonth;
    }
    if (!prettyMap.dayOfMonth && !prettyMap.dayOfWeek && prettyMap.month.length > 0) {
      prettyMap.dayOfMonth = [];
    }
  }
  prettyMap.hour = isAllValue(fieldMap.hour) ? [] : fieldMap.hour;
  if (prettyMap.hour.length < 1 && prettyMap.dayOfMonth && prettyMap.dayOfMonth.length < 1) {
    delete prettyMap.dayOfMonth;
  }
  prettyMap.minute = isAllValue(fieldMap.minute) ? [] : fieldMap.minute;
  if (prettyMap.minute.length < 1 && prettyMap.hour.length < 1) {
    delete prettyMap.hour;
  }
  return prettyMap;
};
