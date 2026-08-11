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

package com.tencent.bk.job.analysis.approval.executor;

import com.tencent.bk.job.common.api.model.DryRunResult;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.model.InternalResponse;

/**
 * 下游 inner 响应的统一拆包。
 * <p>
 * <b>这里的"抛异常"与"返回 valid=false"是两种截然不同的语义，放行时的落态完全依赖这个区分</b>：
 * <ul>
 *     <li>返回 {@code valid=false} 的 {@link DryRunResult} —— 下游明确的业务失败，确定<b>未</b>执行，
 *     放行阶段可安全落 FAILED 终态；</li>
 *     <li>抛异常（响应体缺失、非成功码、Feign 超时/连接中断）—— <b>结果未知</b>，
 *     放行阶段必须停在 EXECUTING 且不重试，绝不能当成"没执行"。</li>
 * </ul>
 * 因此本方法<b>不得</b>把非成功响应"翻译"成 valid=false 的结果 —— 那等于把未知谎报成确定未执行，
 * 用户会据此重新发起，于是同一个操作可能被执行两次。
 */
public abstract class AbstractOperationExecutor<T> implements OperationExecutor<T> {

    protected DryRunResult<?> unwrap(InternalResponse<? extends DryRunResult<?>> response) {
        if (response == null) {
            throw new InternalException("Empty response from downstream service", ErrorCode.INTERNAL_ERROR);
        }
        if (!response.isSuccess()) {
            throw new InternalException(
                "Downstream service returned failure, code=" + response.getCode()
                    + ", errorMsg=" + response.getErrorMsg(),
                ErrorCode.INTERNAL_ERROR
            );
        }
        DryRunResult<?> result = response.getData();
        if (result == null) {
            throw new InternalException("Empty DryRunResult from downstream service", ErrorCode.INTERNAL_ERROR);
        }
        return result;
    }
}
