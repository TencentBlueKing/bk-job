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

package com.tencent.bk.job.common.esb.sdk;

import com.tencent.bk.job.common.esb.model.EsbResp;
import lombok.Data;

/**
 * BK Open API 调用上下文
 *
 * @param <T> 请求
 * @param <R> 响应对象
 */
@Data
public class BkApiContext<T, R> {

    /**
     * HTTP 请求方法
     */
    private String method;
    private String uri;
    private T req;
    /**
     * 原始的 API 响应
     */
    private String originResp;
    /**
     * 反序列化之后的 API 响应
     */
    private EsbResp<R> resp;
    /**
     * API 调用耗时
     */
    private long costTime;

    /**
     * API 响应耗时
     */
    private long requestCostTime;
    /**
     * 响应数据反序列化耗时
     */
    private long deserializeCostTime;
    /**
     * API 是否调用成功并正确响应
     */
    private boolean success;
    /**
     * 网关 request_id
     */
    private String requestId;

    public BkApiContext(String method,
                        String uri,
                        T req,
                        String originResp,
                        EsbResp<R> resp,
                        long costTime,
                        boolean success) {
        this.method = method;
        this.uri = uri;
        this.req = req;
        this.originResp = originResp;
        this.resp = resp;
        this.costTime = costTime;
        this.success = success;
    }
}
