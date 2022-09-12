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

import Vue from 'vue';

import I18n from '@/i18n';

/**
 * @desc 页面编辑状态未保存离开确认
 * @param { String } message
 * @returns { Promise }
 */
export const leaveConfirm = (message = I18n.t('离开将会导致未保存信息丢失')) => {
    if (!window.changeConfirm || window.changeConfirm === 'dialog') {
        return Promise.resolve(true);
    }
    const vm = new Vue();
    const h = vm.$createElement;
    return new Promise((resolve, reject) => {
        vm.$bkInfo({
            title: I18n.t('确认离开当前页？'),
            subHeader: h('p', {
                style: {
                    color: '#63656e',
                    fontSize: '14px',
                    textAlign: 'center',
                },
            }, message),
            confirmFn: () => {
                window.changeConfirm = false;
                resolve(true);
            },
            cancelFn: () => {
                reject(Error('cancel'));
            },
        });
    });
};
