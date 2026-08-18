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

package com.tencent.bk.job.common.api.model;

import com.tencent.bk.job.common.model.ResolvedSummary;
import lombok.Data;

/**
 * 内部（inner）接口的预检/执行统一返回契约。
 * <p>
 * <b>硬性契约：校验失败必须作为正常返回值（HTTP 200）传回，不得靠抛异常传播。</b>
 * 原因是 FeignErrorDecoder 会把下游的 INVALID_PARAM 一律转成 InternalException，
 * 丢掉具体的校验信息；而这里的参数是用户填的，必须原样告诉用户。
 *
 * @param <T> dryRun=false 时的执行结果类型
 */
@Data
public class DryRunResult<T> {

    /**
     * 校验是否通过
     */
    private boolean valid;

    /**
     * 校验失败时的错误码（ErrorCode 常量）
     */
    private Integer errorCode;

    /**
     * 错误码占位参数，供调用方按调用方语言渲染
     */
    private Object[] errorParams;

    /**
     * 校验失败的错误类型（ErrorType 的 type 值），供调用方映射到对外错误响应的语义分类，
     * 不必再按错误码逐个猜是参数问题还是资源不存在
     */
    private Integer errorType;

    /**
     * dryRun 解析出的操作概要，用于渲染审批单据的概要区
     */
    private ResolvedSummary resolvedSummary;

    /**
     * dryRun=false 时的执行结果
     */
    private T executeResult;

    public static <T> DryRunResult<T> valid(ResolvedSummary resolvedSummary, T executeResult) {
        DryRunResult<T> result = new DryRunResult<>();
        result.setValid(true);
        result.setResolvedSummary(resolvedSummary);
        result.setExecuteResult(executeResult);
        return result;
    }

    public static <T> DryRunResult<T> invalid(Integer errorCode, Object[] errorParams) {
        return invalid(errorCode, errorParams, null);
    }

    public static <T> DryRunResult<T> invalid(Integer errorCode, Object[] errorParams, Integer errorType) {
        DryRunResult<T> result = new DryRunResult<>();
        result.setValid(false);
        result.setErrorCode(errorCode);
        result.setErrorParams(errorParams);
        result.setErrorType(errorType);
        return result;
    }
}
