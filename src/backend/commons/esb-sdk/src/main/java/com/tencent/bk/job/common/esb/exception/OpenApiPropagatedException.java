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

package com.tencent.bk.job.common.esb.exception;

import com.tencent.bk.job.common.esb.model.v4.EsbV4RespError;
import com.tencent.bk.job.common.esb.model.v4.V4ErrorCodeEnum;
import lombok.Getter;

/**
 * 服务间调用下游 OpenAPI 时，下游已构造好的错误响应体，原样带回给最上层调用方。
 * <p>
 * <b>不能改用普通的 ServiceException 承载</b>：异常处理器一律拿 errorCode + errorParams 重新渲染消息，
 * 而 OpenAPI 错误体里只有渲染完的 message，errorParams 早已丢失，重渲染只会得到一条占位符没填的消息。
 * 用户填的参数错在哪，必须原样告诉用户。
 */
@Getter
public class OpenApiPropagatedException extends RuntimeException {

    /**
     * 下游返回的错误体，原样回吐，不重新渲染
     */
    private final EsbV4RespError error;

    /**
     * 下游响应的 HTTP 状态码。
     * <p>
     * <b>调用方据此区分“下游已明确拒绝”与“结果未知”</b>：4xx 表示下游在产生任何副作用之前就拒绝了，
     * 可安全按“确定未执行”处理；5xx 说明下游内部出错，操作是否已生效并不确定。
     */
    private final int httpStatus;

    public OpenApiPropagatedException(EsbV4RespError error, int httpStatus, Throwable cause) {
        super(error == null ? null : error.getMessage(), cause);
        this.error = error;
        this.httpStatus = httpStatus;
    }

    /**
     * 下游是否在未产生任何副作用的情况下明确拒绝了本次请求
     */
    public boolean isRejectedByDownstream() {
        return httpStatus >= 400 && httpStatus < 500;
    }

    /**
     * 还原下游的语义化错误码，无法识别时按内部错误处理
     */
    public V4ErrorCodeEnum getV4ErrorCode() {
        if (error == null || error.getCode() == null) {
            return V4ErrorCodeEnum.INTERNAL;
        }
        for (V4ErrorCodeEnum errorCode : V4ErrorCodeEnum.values()) {
            if (errorCode.getCode().equals(error.getCode())) {
                return errorCode;
            }
        }
        return V4ErrorCodeEnum.INTERNAL;
    }
}
