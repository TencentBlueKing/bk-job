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

import BKMagicBox from 'bk-magic-vue';
import Cookie from 'js-cookie';
import Vue from 'vue';
import VueI18n from 'vue-i18n';

import enUS from './language/en.json';
import zhCN from './language/zh.json';


Vue.use(VueI18n);

const { locale, lang } = BKMagicBox;
const BLUEKINNG_LANGUAGE = 'blueking_language';

// 解析cookie，默认语言
let localeLanguage = 'zh-CN';
const bluekingLanguage = Cookie.get(BLUEKINNG_LANGUAGE);
if (bluekingLanguage && bluekingLanguage.toLowerCase() === 'en') {
  localeLanguage = 'en-US';
}

document.querySelector('html').setAttribute('lang', localeLanguage);

// bk-magic-vue 基础语言包
const messages = {
  'en-US': Object.assign({}, enUS),
  'zh-CN': Object.assign({}, zhCN),
};

const i18n = new VueI18n({
  locale: localeLanguage,
  fallbackLocale: 'zh-CN',
  messages,
});

if (localeLanguage === 'zh-CN') {
  locale.use(lang.zhCN);
} else {
  locale.use(lang.enUS);
}

export const loadLanguage = (localPackage) => {
  Object.keys(localPackage).forEach((namespace) => {
    Object.keys(localPackage[namespace]).forEach((local) => {
      i18n.mergeLocaleMessage(local, {
        [namespace]: localPackage[namespace][local],
      });
    });
  });
};

export const setLocale = (locale) => {
  const realLocal = locale === 'en' ? 'en-US' : 'zh-CN';
  if (Object.prototype.hasOwnProperty.call(messages, realLocal)) {
    i18n._vm.locale = realLocal; // eslint-disable-line no-underscore-dangle
    document.querySelector('html').setAttribute('lang', realLocal);
  }
};

export default i18n;

export const useI18n = () => ({
  t: key => i18n.t(key), // eslint-disable-line no-underscore-dangle
  locale: i18n.locale,
});


