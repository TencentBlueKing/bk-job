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

// const fs  = require('fs');
// const path = require('path');
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const recur = async (target) => {
  const state = fs.statSync(target);
  if (state.isFile()) {
    if (/local.js$/.test(target)) {
      console.log(`JAVASCRIPT: ${target}`);
      const distDir = path.join(path.dirname(target), 'language');
      const stats = fs.statSync(distDir);
      if (!stats.isDirectory()) {
        await fs.mkdirSync(distDir);
      }


      const data = await import(target);

      const { message, namespace } = data.default;
      const znData = {};
      const enData = {};
      Object.keys(message).forEach((key) => {
        if (typeof message[key] === 'string') {
          znData[key] = key;
          enData[key] = message[key];
        } else if (typeof message[key] === 'object') {
          Object.keys(message[key]).forEach((key2) => {
            znData[`${key}_${key2}`] = key;
            enData[`${key}_${key2}`] = message[key][key2];
          });
        }
      });
      fs.writeFileSync(
        path.join(distDir, 'index.js'),
        `
import zhCN from './zh.json';
import enUS from './en.json';

export default {
    ${namespace}: {
      'zh-CN': zhCN,
      'en-US': enUS,
    }
}
      `,
      );
      fs.writeFileSync(path.join(distDir, 'zh.json'), JSON.stringify(znData, null, 2));
      fs.writeFileSync(path.join(distDir, 'en.json'), JSON.stringify(enData, null, 2));
      console.log('lang = ', znData, enData, '\n');
    }
  } else if (state.isDirectory()) {
    const dirList = fs.readdirSync(target);
    dirList.forEach((item) => {
      recur(path.join(target, item));
    });
  }
};

recur(path.join(path.dirname(fileURLToPath(import.meta.url)), './src/views'));

const { default: message } = await import(path.join(path.dirname(fileURLToPath(import.meta.url)), './src/i18n/local.js'));


const znData = {};
const enData = {};
Object.keys(message).forEach((key) => {
  if (typeof message[key] === 'string') {
    znData[key] = key;
    enData[key] = message[key];
  } else if (typeof message[key] === 'object') {
    Object.keys(message[key]).forEach((key2) => {
      znData[`${key}_${key2}`] = key;
      enData[`${key}_${key2}`] = message[key][key2];
    });
  }
});

fs.writeFileSync(path.join(path.join(path.dirname(fileURLToPath(import.meta.url)), 'src/i18n/language'), 'zh.json'), JSON.stringify(znData, null, 2));
fs.writeFileSync(path.join(path.join(path.dirname(fileURLToPath(import.meta.url)), 'src/i18n/language'), 'en.json'), JSON.stringify(enData, null, 2));
