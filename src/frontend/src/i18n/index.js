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
import _ from 'lodash';
import Vue from 'vue';
import VueI18n from 'vue-i18n';

import Local from './local';

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
  'en-US': Object.assign({}, lang.enUS),
  'zh-CN': Object.assign({}, lang.zhCN),
};

const namespaceMap = {};
const parseLanguagePackage = (localPackage) => {
  let cn = {};
  let en = {};
  const formatCnPackage = (message) => {
    const msgCn = {};
    Object.keys(message).forEach((key) => {
      if (_.isPlainObject(message[key])) {
        msgCn[key] = {};
        Object.keys(message[key]).forEach((childkey) => {
          msgCn[key][childkey] = key;
        });
      } else {
        msgCn[key] = key;
      }
    });

    return msgCn;
  };

  if (_.has(localPackage, 'namespace')) {
    const curNamespace = localPackage.namespace;
    if (!namespaceMap[curNamespace]) {
      namespaceMap[curNamespace] = true;
      cn[curNamespace] = formatCnPackage(localPackage.message);
      en[curNamespace] = localPackage.message;
    }
  } else {
    cn = formatCnPackage(localPackage);
    en = localPackage;
  }
  messages['zh-CN'] = Object.freeze(Object.assign({}, messages['zh-CN'], cn));
  messages['en-US'] = Object.freeze(Object.assign({}, messages['en-US'], en));
};

parseLanguagePackage(Local);

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

// 重写插值方法
const parseLangVariable = (sentence, varObj) => {
  /* eslint-disable no-iterator, no-restricted-syntax */
  let target = sentence;
  for (const varkey in varObj) {
    target = target.replace(`{${varkey}}`, varObj[varkey]).toString();
  }
  return target;
};

const i18nt = i18n._t; // eslint-disable-line no-underscore-dangle
i18n._t = function (key, ...values) { // eslint-disable-line no-underscore-dangle
  let namespace = '';
  // 解析命名空间
  Object.keys(namespaceMap).forEach((namespacekey) => {
    const reg = new RegExp(`^${namespacekey}\\.`);
    if (reg.test(key)) {
      namespace = namespacekey;
    }
  });
  if (namespace) {
    // 匹配到命名空间
    const localMessage = i18n._getMessages()[i18n.locale][namespace]; // eslint-disable-line no-underscore-dangle,max-len
    const reg = new RegExp(`^(?:${namespace}\\.)(.*)`);
    const localPath = key.match(reg)[1];// eslint-disable-line prefer-destructuring
    // 单层路径
    if (_.has(localMessage, localPath)) {
      if (values[3]) {
        return parseLangVariable(localMessage[localPath], values[3]);
      }
      return localMessage[localPath];
    }

    const splitIndex = localPath.lastIndexOf('.');
    if (splitIndex < 0) {
      // 不匹配多层路径，当前的key不存在
      console.log('i18n path error', key);
      return key;
    }

    const firstPath = localPath.substring(0, splitIndex);
    // 多层路径，第一层不存在
    if (!Object.prototype.hasOwnProperty.call(localMessage, firstPath)) {
      console.log(localMessage, firstPath);
      console.log('i18n first path error', key, firstPath);
      return key;
    }

    const lastPath = localPath.substring(splitIndex + 1, localPath.length);
    // 多层路径，第二层不存在
    if (!Object.prototype.hasOwnProperty.call(localMessage[firstPath], lastPath)) {
      console.log('i18n last path error', key, firstPath);
      return key;
    }

    if (values[3]) {
      return parseLangVariable(localMessage[firstPath][lastPath], values[3]);
    }
    return localMessage[firstPath][lastPath];
  }
  // eslint-disable-next-line no-underscore-dangle
  return i18nt.call(i18n, key, i18n.locale, i18n._getMessages(), this, ...values);
};
i18n._t.bind(i18n); // eslint-disable-line no-underscore-dangle

export const loadLanguage = (localPackage) => {
  parseLanguagePackage(localPackage);
};

export const setLocale = (locale) => {
  const realLocal = locale === 'en' ? 'en-US' : 'zh-CN';
  if (Object.prototype.hasOwnProperty.call(messages, realLocal)) {
    i18n.locale = realLocal;
    document.querySelector('html').setAttribute('lang', realLocal);
  }
};

export default i18n;
