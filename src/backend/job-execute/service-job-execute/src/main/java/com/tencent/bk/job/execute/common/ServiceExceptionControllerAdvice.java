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

package com.tencent.bk.job.execute.common;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.i18n.MessageI18nService;
import com.tencent.bk.job.common.iam.exception.InSufficientPermissionException;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.web.exception.HttpStatusServiceException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;

@ControllerAdvice({"com.tencent.bk.job.execute.api.inner"})
@Slf4j
public class ServiceExceptionControllerAdvice extends ResponseEntityExceptionHandler {
    private final MessageI18nService i18nService;

    @Autowired
    public ServiceExceptionControllerAdvice(MessageI18nService i18nService) {
        this.i18nService = i18nService;
    }

    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    ResponseEntity<?> handleControllerServiceException(HttpServletRequest request, ServiceException ex) {
        String exceptionInfo = "Handle service exception, exception: " + ex.toString();
        log.warn(exceptionInfo, ex);
        if (ex instanceof HttpStatusServiceException) {
            HttpStatusServiceException httpStatusServiceException = (HttpStatusServiceException) ex;
            return new ResponseEntity<>(ServiceResponse.buildCommonFailResp(httpStatusServiceException,
                i18nService), httpStatusServiceException.getHttpStatus());
        } else if (ex instanceof InSufficientPermissionException) {
            InSufficientPermissionException inSufficientPermissionException = (InSufficientPermissionException) ex;
            log.debug("Insufficient permission, authResult: {}", inSufficientPermissionException.getAuthResult());
            return new ResponseEntity<>(ServiceResponse.buildCommonFailResp(ErrorCode.USER_NO_PERMISSION_COMMON,
                i18nService), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(ServiceResponse.buildCommonFailResp(ex, i18nService), HttpStatus.OK);
        }
    }

    @ExceptionHandler(Throwable.class)
    @ResponseBody
    ResponseEntity<?> handleControllerException(HttpServletRequest request, Throwable ex) {
        log.warn("Handle exception", ex);
        // 默认处理
        HttpStatus status = getStatus(request);
        return new ResponseEntity<>(ServiceResponse.buildCommonFailResp(ErrorCode.SERVICE_INTERNAL_ERROR,
            i18nService), status);
    }

    @ExceptionHandler(FeignException.class)
    @ResponseBody
    ResponseEntity<?> handleFeignException(HttpServletRequest request, FeignException ex) {
        log.warn("Handle feign exception", ex);
        if (ex.status() == HttpStatus.UNAUTHORIZED.value()) {
            return new ResponseEntity<>(ServiceResponse.buildCommonFailResp(ErrorCode.SERVICE_AUTH_FAIL, i18nService)
                , HttpStatus.UNAUTHORIZED);
        } else {
            return new ResponseEntity<>(ServiceResponse.buildCommonFailResp(ErrorCode.SERVICE_INTERNAL_ERROR,
                i18nService), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private HttpStatus getStatus(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        if (statusCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.valueOf(statusCode);
    }

}
