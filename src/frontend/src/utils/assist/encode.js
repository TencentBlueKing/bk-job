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

/**
 * @desc 正则表达式关键字符转换
 * @param { String } paramStr
 * @returns { String }
 */
export const encodeRegexp = (paramStr) => {
  const regexpKeyword = [
    '\\', '.', '*', '-', '{', '}', '[', ']', '^', '(', ')', '$', '+', '?', '|',
  ];
  const res = regexpKeyword.reduce(
    (result, charItem) => result.replace(new RegExp(`\\${charItem}`, 'g'), `\\${charItem}`),
    paramStr,
  );
  return res;
};

/**
 * @desc 多行文本处理
 * @param { String } text
 * @returns { String }
 */
export const encodeMult = (text) => {
  const temp = document.createElement('textarea');
  temp.value = text;
  return temp.value;
};

/**
 * @desc 格式化用户输入的HTML
 * @param { String } str
 * @returns { String }
 */
export const escapeHTML = str => str.replace(/&/g, '&#38;').replace(/"/g, '&#34;')
  .replace(/'/g, '&#39;')
  .replace(/</g, '&#60;');
