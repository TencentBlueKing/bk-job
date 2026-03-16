/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
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

package com.tencent.bk.job.common.iam.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * jakarta.servlet → javax.servlet 请求桥接。
 * IAM SDK (iam-sdk-1.0.0.jar) 仍使用 javax.servlet API，
 * 通过动态代理将 jakarta.servlet.http.HttpServletRequest 适配为 javax 版本。
 * 待 IAM SDK 升级至 Jakarta Servlet 后可移除此桥接类。
 */
public class JavaxServletRequestBridge {

    public static javax.servlet.http.HttpServletRequest toJavax(
        jakarta.servlet.http.HttpServletRequest jakartaRequest) {
        return (javax.servlet.http.HttpServletRequest) Proxy.newProxyInstance(
            javax.servlet.http.HttpServletRequest.class.getClassLoader(),
            new Class<?>[]{javax.servlet.http.HttpServletRequest.class},
            new JakartaToJavaxHandler(jakartaRequest)
        );
    }

    private static class JakartaToJavaxHandler implements InvocationHandler {
        private final jakarta.servlet.http.HttpServletRequest delegate;

        JakartaToJavaxHandler(jakarta.servlet.http.HttpServletRequest delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Method delegateMethod = delegate.getClass().getMethod(
                method.getName(), method.getParameterTypes());
            return delegateMethod.invoke(delegate, args);
        }
    }
}
