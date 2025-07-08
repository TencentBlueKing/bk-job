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

import Node from './Node';

export const parsetext = (expression) => {
  const stack = [];
  const rangReg = /-/;
  const repeatReg = /\//;
  const atoms = (`${expression}`).trim().split(',');
  let index = -1;
  // eslint-disable-next-line no-plusplus
  while (++index < atoms.length) {
    const enumValue = atoms[index];
    if (rangReg.test(enumValue) && repeatReg.test(enumValue)) {
      // 在指定区间重复
      const [rang, repeatInterval] = enumValue.split('/');
      const [min, max] = rang.split('-');
      stack.push(new Node({
        type: Node.TYPE_RANG_REPEAT,
        min,
        max,
        repeatInterval,
      }));
      continue;
    } else if (repeatReg.test(enumValue)) {
      // 从指定起始位置重复
      const [value, repeatInterval] = enumValue.split('/');
      stack.push(new Node({
        type: Node.TYPE_REPEAT,
        value,
        repeatInterval,
      }));
      continue;
    } else if (rangReg.test(enumValue)) {
      // 指定区间
      const [min, max] = enumValue.split('-');
      stack.push(new Node({
        type: Node.TYPE_RANG,
        min,
        max,
      }));
      continue;
    } else {
      stack.push(new Node({
        type: Node.TYPE_ENUM,
        value: enumValue,
      }));
    }
  }
  return stack;
};
