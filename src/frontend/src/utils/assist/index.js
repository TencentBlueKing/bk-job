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

import { messageSuccess } from '@/common/bkmagic';
import I18n from '@/i18n';

export * from './permission';
export * from './leave-confirm';
export * from './time';
export * from './url';
export * from './encode';
export * from './generator-default-value';
export * from './dom';
export * from './business';
export * from './script-error-confirm';
export * from './byte-pretty';
export * from './format';
export * from './calc-text-width';
export * from './ordinal-suffix-of';

export const isMac = () => /macintosh|mac os x/i.test(navigator.userAgent);

export const formatScriptTypeValue = (value) => {
    const key = String(value).toLowerCase();
    const typeMap = {
        shell: 1,
        bat: 2,
        perl: 3,
        python: 4,
        powershell: 5,
        sql: 6,
        1: 'Shell',
        2: 'Bat',
        3: 'Perl',
        4: 'Python',
        5: 'Powershell',
        6: 'SQL',
    };
    return typeMap[key] || key;
};

export const execCopy = (value, message = I18n.t('复制成功')) => {
    const textarea = document.createElement('textarea');
    document.body.appendChild(textarea);
    textarea.value = value;
    textarea.select();
    if (document.execCommand('copy')) {
        document.execCommand('copy');
        messageSuccess(message);
    }
    document.body.removeChild(textarea);
};

export const downloadUrl = (url) => {
    // 创建隐藏的可下载链接

    const eleLink = document.createElement('a');
    eleLink.style.display = 'none';
    eleLink.href = url;
    
    // 触发点击
    document.body.appendChild(eleLink);
    const { changeAlert } = window;
    window.changeConfirm = false;
    eleLink.click();
    setTimeout(() => {
        window.changeConfirm = changeAlert;
    });
    
    // 然后移除
    document.body.removeChild(eleLink);
};

export const formatNumber = (target, short = false) => {
    const format = val => `${val}`.replace(/(\d{1,3})(?=(\d{3})+$)/g, '$1,');
    if (!short) {
        return format(target);
    }
    if (target < 10000) {
        return format(target);
    }
    if (target < 1000000) {
        return `${parseFloat(format(target / 1000), 2)} K`;
    }
    return `${parseFloat(format(target / 1000000), 2)} M`;
};
