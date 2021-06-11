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

package com.tencent.bk.job.common.web.exception.handler;

import com.tencent.bk.job.common.annotation.WebAPI;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.exception.InSufficientPermissionException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.web.exception.HttpStatusServiceException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

@ControllerAdvice(annotations = {WebAPI.class})
@Slf4j
public class WebExceptionControllerAdvice extends ResponseEntityExceptionHandler {
    private final MessageI18nService i18nService;
    private final WebAuthService webAuthService;

    @Autowired
    public WebExceptionControllerAdvice(MessageI18nService i18nService, WebAuthService webAuthService) {
        this.i18nService = i18nService;
        this.webAuthService = webAuthService;
    }

    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    ResponseEntity<?> handleControllerServiceException(HttpServletRequest request, ServiceException ex) {
        log.warn("Handle service exception", ex);
        if (ex instanceof HttpStatusServiceException) {
            HttpStatusServiceException httpStatusServiceException = (HttpStatusServiceException) ex;
            return new ResponseEntity<>(ServiceResponse.buildCommonFailResp(httpStatusServiceException, i18nService),
                httpStatusServiceException.getHttpStatus());
        } else if (ex instanceof InSufficientPermissionException) {
            InSufficientPermissionException inSufficientPermissionException = (InSufficientPermissionException) ex;
            AuthResult authResult = inSufficientPermissionException.getAuthResult();
            log.debug("Insufficient permission, authResult: {}", authResult);
            if (StringUtils.isEmpty(authResult.getApplyUrl())) {
                authResult.setApplyUrl(webAuthService.getApplyUrl(authResult.getRequiredActionResources()));
            }
            return new ResponseEntity<>(ServiceResponse.buildAuthFailResp(
                webAuthService.toAuthResultVO(inSufficientPermissionException.getAuthResult())), HttpStatus.OK);
        } else {
            return new ResponseEntity<>(ServiceResponse.buildCommonFailResp(ex, i18nService),
                HttpStatus.OK);
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

    private HttpStatus getStatus(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        if (statusCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.valueOf(statusCode);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    ResponseEntity<?> handleConstraintViolationException(HttpServletRequest request,
                                                         ConstraintViolationException ex) {
        log.warn("Handle ConstraintViolationException", ex);
        ServiceResponse resp = ServiceResponse.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                         HttpHeaders headers, HttpStatus status,
                                                                         WebRequest request) {
        log.warn("Handle HttpRequestMethodNotSupportedException", ex);
        ServiceResponse resp = ServiceResponse.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                     HttpHeaders headers, HttpStatus status,
                                                                     WebRequest request) {
        log.warn("Handle HttpMediaTypeNotSupportedException", ex);
        ServiceResponse resp = ServiceResponse.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
                                                                      HttpHeaders headers, HttpStatus status,
                                                                      WebRequest request) {
        log.warn("Handle HttpMediaTypeNotAcceptableException", ex);
        ServiceResponse resp = ServiceResponse.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleMissingPathVariable(MissingPathVariableException ex, HttpHeaders headers,
                                                               HttpStatus status, WebRequest request) {
        log.warn("Handle MissingPathVariableException", ex);
        ServiceResponse resp = ServiceResponse.buildCommonFailResp(ErrorCode.SERVICE_INTERNAL_ERROR, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers, HttpStatus status,
                                                                          WebRequest request) {
        log.warn("Handle MissingServletRequestParameterException", ex);
        ServiceResponse resp = ServiceResponse.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException ex,
                                                                          HttpHeaders headers, HttpStatus status,
                                                                          WebRequest request) {
        log.warn("Handle ServletRequestBindingException", ex);
        ServiceResponse resp = ServiceResponse.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleConversionNotSupported(ConversionNotSupportedException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        log.warn("Handle ConversionNotSupportedException", ex);
        ServiceResponse resp = ServiceResponse.buildCommonFailResp(ErrorCode.SERVICE_INTERNAL_ERROR, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatus status, WebRequest request) {
        log.warn("Handle TypeMismatchException", ex);
        ServiceResponse resp = ServiceResponse.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        log.warn("Handle HttpMessageNotReadableException", ex);
        ServiceResponse resp = ServiceResponse.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        log.warn("Handle HttpMessageNotWritableException", ex);
        ServiceResponse resp = ServiceResponse.buildCommonFailResp(ErrorCode.SERVICE_INTERNAL_ERROR, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        log.warn("Handle MethodArgumentNotValidException", ex);
        ServiceResponse resp = ServiceResponse.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(MissingServletRequestPartException ex,
                                                                     HttpHeaders headers, HttpStatus status,
                                                                     WebRequest request) {
        log.warn("Handle MissingServletRequestPartException", ex);
        ServiceResponse resp = ServiceResponse.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers, HttpStatus status,
                                                         WebRequest request) {
        log.warn("Handle BindException", ex);
        ServiceResponse resp = ServiceResponse.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers,
                                                                   HttpStatus status, WebRequest request) {
        log.warn("Handle NoHandlerFoundException", ex);
        ServiceResponse resp = ServiceResponse.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex,
                                                                        HttpHeaders headers, HttpStatus status,
                                                                        WebRequest webRequest) {
        log.warn("Handle AsyncRequestTimeoutException", ex);
        ServiceResponse resp = ServiceResponse.buildCommonFailResp(ErrorCode.SERVICE_INTERNAL_ERROR, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

}
