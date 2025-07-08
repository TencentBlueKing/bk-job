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

package com.tencent.bk.job.common.constant;

/**
 * Job 拦截器 ORDER 定义
 */
public interface InterceptorOrder {

    /**
     * 对请求进行预处理（检查、限流、记录、修改请求等）
     */
    interface Init {
        int HIGHEST = 1;
        int LOWEST = 100;
        /**
         * 检查请求合法性
         */
        int CHECK_VALID = HIGHEST;
        /**
         * 日志记录
         */
        int LOG = HIGHEST + 1;
        /**
         * 请求统计
         */
        int METRICS = HIGHEST + 2;
        /**
         * 请求处理
         */
        int REWRITE_REQUEST = HIGHEST + 2;
    }

    /**
     * 用户认证
     */
    interface Identification {
        int HIGHEST = 101;
        int LOWEST = 200;
    }

    /**
     * 用户鉴权
     */
    interface AUTH {
        int HIGHEST = 201;
        int LOWEST = 300;
        /**
         * 全局鉴权
         */
        int AUTH_GLOBAL = HIGHEST;
        /**
         * 普通鉴权
         */
        int AUTH_COMMON = HIGHEST + 50;
    }

    /**
     * 业务逻辑
     */
    interface Business {
        int HIGHEST = 301;
        int LOWEST = 400;
    }

    /**
     * 请求处理完成
     */
    interface Completion {
        int HIGHEST = 401;
        int LOWEST = 500;
    }
}
