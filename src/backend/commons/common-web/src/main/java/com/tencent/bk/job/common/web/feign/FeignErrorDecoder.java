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

package com.tencent.bk.job.common.web.feign;

import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.error.ErrorType;
import com.tencent.bk.job.common.util.json.JsonUtils;
import feign.FeignException;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public class FeignErrorDecoder extends ErrorDecoder.Default {

    @Override
    public Exception decode(String methodKey, Response response) {
        Exception exception = super.decode(methodKey, response);
        log.debug("Decode feign error, methodKey: {}, response: {}", methodKey, response);

        if (exception instanceof RetryableException) {
            return exception;
        }

        try {
            if (exception instanceof FeignException) {
                FeignException feignException = (FeignException) exception;
                String responseBody = feignException.contentUTF8();

                if (StringUtils.isNotEmpty(responseBody)) {
                    InternalResponse<?> serviceResponse = JsonUtils.fromJson(responseBody, InternalResponse.class);
                    if (serviceResponse != null && serviceResponse.getCode() != null) {
                        return decodeErrorCode(feignException, serviceResponse);
                    }
                }
            }
        } catch (Throwable e) {
            log.error(e.getMessage(), e);
        }
        return exception;
    }

    private Exception decodeErrorCode(FeignException exception, InternalResponse<?> response) {
        Integer errorType = response.getErrorType();
        Integer errorCode = response.getCode();
        String errorMsg = response.getErrorMsg();
        if (errorType == null || errorCode == null) {
            return exception;
        }

        log.debug("Decode error code, errorType: {}, errorCode: {}, errorMsg: {}", errorType, errorCode, errorMsg);

        ErrorType type = ErrorType.valOf(errorType);
        switch (type) {
            case INVALID_PARAM:
            case UNAUTHENTICATED:
            case ABORTED:
            case RESOURCE_EXHAUSTED:
            case UNIMPLEMENTED:
            case INTERNAL:
            case UNAVAILABLE:
            case TIMEOUT:
                return new InternalException(exception, errorCode, errorMsg);
            case PERMISSION_DENIED:
                return new PermissionDeniedException(AuthResult.fromAuthResultDTO(response.getAuthResult()));
            case NOT_FOUND:
                return new NotFoundException(exception, errorCode, errorMsg);
            case ALREADY_EXISTS:
                return new AlreadyExistsException(exception, errorCode, errorMsg);
            case FAILED_PRECONDITION:
                return new FailedPreconditionException(errorCode, errorMsg);
            default:
                return new ServiceException(type, errorCode);
        }

    }
}
