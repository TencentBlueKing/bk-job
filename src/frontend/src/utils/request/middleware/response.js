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

import AuthResultModel from '@model/auth-result';

import EventBus from '@utils/event-bus';

import RequestError from '../lib/request-error';

import {
    loginDialog,
    messageError,
    permissionDialog,
} from '@/common/bkmagic';

// 标记已经登录过状态
// 第一次登录跳转登录页面，之后弹框登录
let hasLogined = false;

// 监听用户主动退出登录事件
EventBus.$on('logout', () => {
    hasLogined = false;
});

export default (interceptors) => {
    interceptors.use((response) => {
    // 处理http响应成功，后端返回逻辑
        switch (response.data.code) {
            // 后端业务逻辑处理成功
            case 0:
                hasLogined = true;
                return response.data;
            default: {
                // 后端逻辑处理报错
                const { code, errorMsg = '系统错误' } = response.data;
                throw new RequestError(code, errorMsg, response);
            }
        }
    }, (error) => {
        // 超时取消
        if (error.__CANCEL__) { // eslint-disable-line no-underscore-dangle
            return Promise.reject(new RequestError('CANCEL', '请求已取消'));
        }
        // 处理 http 错误响应逻辑
        if (error.response) {
            // 登录状态失效
            if (error.response.status === 401
                && error.response.headers['x-login-url']) {
                return Promise.reject(new RequestError(401, error.response.headers['x-login-url']));
            }
            // 默认使用 http 错误描述，
            // 如果 response body 里面有自定义错误描述优先使用
            let errorMessage = error.response.statusText;
            if (error.response.data && error.response.data.errorMsg) {
                errorMessage = error.response.data.errorMsg;
            }
            return Promise.reject(new RequestError(
                error.response.status || -1,
                errorMessage,
                error.response,
            ));
        }
        return Promise.reject(new RequestError(-1, `${window.PROJECT_CONFIG.AJAX_URL_PREFIX} 无法访问`));
    });

    // 统一错误处理逻辑
    interceptors.use(undefined, (error) => {
        switch (error.code) {
            // 未登陆
            case 401:
                if (hasLogined) {
                    loginDialog(error.message);
                } else {
                    window.location.href = `${error.message}&c_url=${decodeURIComponent(window.location.href)}`;
                }
                break;
                // 没权限
            case 403: {
                const requestPayload = error.response.config.payload;
                const authResult = new AuthResultModel(error.response.data.authResult || {});

                if (requestPayload.permission === 'page') {
                    // 配合 jb-router-view（@components/jb-router-view）全局展示没权限提示
                    EventBus.$emit('permission-page', authResult);
                } else if (requestPayload.permission === 'catch') {
                    // 配合 apply-section （@components/apply-permission/apply-section）使用，局部展示没权限提示
                    EventBus.$emit('permission-catch', authResult);
                } else {
                    // 弹框展示没权限提示
                    permissionDialog('', authResult);
                }
                break;
            }
            case 'CANCEL':
                break;
                // 网络超时
            case 'ECONNABORTED':
                messageError('请求超时');
                break;
            default:
                messageError(error.message);
        }
        return Promise.reject(error);
    });
};
