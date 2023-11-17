/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.common.esb.sdk;

import com.tencent.bk.job.common.util.json.JsonUtils;
import org.slf4j.Logger;

/**
 * 自定义 BK API 调用日志输出策略
 */
public interface BkApiLogStrategy {
    /**
     * 打印请求
     *
     * @param log     logger
     * @param context BK Open API 调用上下文
     * @param <T>     请求
     * @param <R>     响应对象
     */
    default <T, R> void logReq(Logger log, BkApiContext<T, R> context) {
        if (log.isInfoEnabled()) {
            log.info("Request|method={}|uri={}|reqStr={}", context.getMethod(),
                context.getUri(), JsonUtils.toJsonWithoutSkippedFields(context.getReq()));
        }
    }

    /**
     * 打印响应
     *
     * @param log     logger
     * @param context BK Open API 调用上下文
     * @param <T>     请求
     * @param <R>     响应对象
     */
    default <T, R> void logResp(Logger log, BkApiContext<T, R> context) {
        if (log.isInfoEnabled()) {
            log.info("Response|method={}|uri={}|success={}|costTime={}|resp={}|",
                context.getMethod(), context.getUri(), context.isSuccess(),
                context.getCostTime(), context.getOriginResp());
        }
    }
}
