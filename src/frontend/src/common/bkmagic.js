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

import {
    parseURL,
} from '@utils/assist';

import ApplyPermissionDialog from '@components/apply-permission/apply-dialog';

import PassLogin from '@blueking/paas-login';

// 全量引入
import './fully-import';

const Message = Vue.prototype.$bkMessage;

let messageInstance = null;
let loginInstance = null;

export const messageError = (message, callback, delay = 3000) => {
    messageInstance && messageInstance.close();
    messageInstance = Message({
        message,
        delay: callback ? 500 : delay,
        theme: 'error',
        onClose: callback,
        ellipsisLine: 0,
    });
};

export const messageSuccess = (message, callback, delay = 3000) => {
    messageInstance && messageInstance.close();
    messageInstance = Message({
        message,
        delay: callback ? 500 : delay,
        theme: 'success',
        onClose: callback,
    });
};

export const messageInfo = (message, callback, delay = 3000) => {
    messageInstance && messageInstance.close();
    messageInstance = Message({
        message,
        delay: callback ? 1500 : delay,
        theme: 'primary',
        onClose: callback,
    });
};

export const messageWarn = (message, callback, delay = 3000) => {
    messageInstance && messageInstance.close();
    messageInstance = Message({
        message,
        delay: callback ? 1500 : delay,
        theme: 'warning',
        hasCloseIcon: true,
        onClose: callback,
    });
};

export const loginDialog = (url) => {
    const generatorLoginUrl = (target) => {
        const urlInfo = parseURL(target);
        const { protocol, host, port, search, hash } = urlInfo;
        let { pathname } = urlInfo;

        let res = `${protocol}://${host}`;
        if (port) {
            res += `:${port}`;
        }
        pathname += 'plain/';
        res += pathname;
        
        if (search) {
            res += `?${search}`;
        }
        if (hash) {
            res += `#${hash}`;
        }
        return res;
    };
    
    const loginUrl = generatorLoginUrl(url);

    if (loginInstance) {
        return;
    }
    loginInstance = new Vue(PassLogin).$mount();
    loginInstance.loginUrl = loginUrl;
    loginInstance.successUrl = decodeURIComponent(`${window.location.origin}/static/login_success.html`);
    loginInstance.$nextTick(() => {
        document.body.appendChild(loginInstance.$el);
        loginInstance.show();
    });
};

let permissionInstance;

export const permissionDialog = (authParams = {}, authResult = {}) => {
    if (!permissionInstance) {
        permissionInstance = new Vue(ApplyPermissionDialog).$mount();
        permissionInstance.$watch(() => permissionInstance.isShowDialog, (isShowDialog) => {
            if (!isShowDialog) {
                document.body.removeChild(permissionInstance.$el);
            }
        });
    }
    permissionInstance.authParams = authParams;
    permissionInstance.authResult = authResult;
    permissionInstance.show();
    permissionInstance.$nextTick(() => {
        document.body.appendChild(permissionInstance.$el);
    });
};

Vue.prototype.messageError = messageError;
Vue.prototype.messageSuccess = messageSuccess;
Vue.prototype.messageInfo = messageInfo;
Vue.prototype.messageWarn = messageWarn;
