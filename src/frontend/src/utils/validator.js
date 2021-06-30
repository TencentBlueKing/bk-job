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

import I18n from '@/i18n';

export const scriptNameRule = {
    validator: value => /^[^\\|/:*<>"?]{1,60}$/.test(value),
    message: `${I18n.t('不允许包含这些字符')}：\\ | / : * < > " ?`,
};

export const scriptVersionRule = {
    validator: value => /^[A-Za-z0-9\-_#@.]{1,30}$/.test(value),
    message: `${I18n.t('只允许包含这些字符')}：A-Z a-z 0-9 _ - # @ .`,
};

export const taskTemplateName = {
    validator: value => /^[^\\|/:*<>"?]{1,60}$/.test(value),
    message: `${I18n.t('不允许包含这些字符')}：\\ | / : * < > " ?`,
};

export const planNameRule = {
    validator: value => /^[^\\|/:*<>"?]{1,60}$/.test(value),
    message: `${I18n.t('不允许包含这些字符')}：\\ | / : * < > " ?`,
};

export const timeTaskNameRule = {
    validator: value => /^[^\\|/:*<>"?]{1,60}$/.test(value),
    message: `${I18n.t('不允许包含这些字符')}：\\ | / : * < > " ?`,
};

export const accountAliasNameRule = {
    validator: value => /^[^\\|/:*<>"?]{1,32}$/.test(value),
    message: `${I18n.t('不允许包含这些字符')}：\\ | / : * < > " ?`,
};

export const filePathRule = {
    // eslint-disable-next-line max-len
    validator: value => /(^([a-z]|[A-Z]):(?=\\(?![\0-\37<>:"/\\|?*])|\/(?![\0-\37<>:"/\\|?*])|$)|^\\(?=[\\/][^\0-\37<>:"/\\|?*]+)|^(?=(\\|\/)$)|^\.(?=(\\|\/)$)|^\.\.(?=(\\|\/)$)|^(?=(\\|\/)[^\0-\37<>:"/\\|?*]+)|^\.(?=(\\|\/)[^\0-\37<>:"/\\|?*]+)|^\.\.(?=(\\|\/)[^\0-\37<>:"/\\|?*]+))((\\|\/)[^\0-\37<>:"/\\|?*]+|(\\|\/)$)*()|(^\/$|(^(?=\/)|^\.|^\.\.)(\/(?=[^/\0])[^/\0]+)*\/?)$/.test(value)
            || /^\$\{/.test(value),
    message: I18n.t('目标路径格式错误'),
};

export const tagNameRule = {
    validator: value => /^[\u4e00-\u9fa5A-Za-z0-9\-_!#@$&%^~=+.]{1,20}$/.test(value),
    message: I18n.t('标签名支持：汉字 A-Z a-z 0-9 _ - ! # @ $ & % ^ ~ = + .'),
};

export const globalVariableNameRule = {
    validator: value => /^[a-zA-Z_][0-9a-zA-Z_-]{0,29}$/.test(value),
    message: I18n.t('变量名称：以英文字符、下划线开头；只允许英文字符、数字、下划线、和 -'),
};

export const IPRule = {
    validator: value => /([0,1]?\d{1,2}|2([0-4][0-9]|5[0-5]))(\.([0,1]?\d{1,2}|2([0-4][0-9]|5[0-5]))){3}/.test(value),
    message: I18n.t('IP 格式不正确'),
};

export const fileSourceAliasNameRule = {
    validator: value => /^[^\\|/:*<>"?]{1,32}$/.test(value),
    message: `${I18n.t('不允许包含这些字符')}：\\ | / : * < > " ?`,
};
