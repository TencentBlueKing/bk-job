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

package com.tencent.bk.job.crontab.common;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.i18n.MessageI18nService;
import lombok.extern.slf4j.Slf4j;
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

/**
 * 处理ESB异常
 */
@ControllerAdvice({"com.tencent.bk.job.crontab.api.esb"})
@Slf4j
public class EsbExceptionControllerAdvice extends ResponseEntityExceptionHandler {
    private final MessageI18nService i18nService;

    @Autowired
    public EsbExceptionControllerAdvice(MessageI18nService i18nService) {
        this.i18nService = i18nService;
    }

    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    ResponseEntity<?> handleControllerServiceException(HttpServletRequest request, ServiceException ex) {
        log.warn("Handle service exception", ex);
        // esb请求错误统一返回200，具体的错误信息放在返回数据里边
        return new ResponseEntity<>(EsbResp.buildCommonFailResp(ex.getErrorCode(), i18nService), HttpStatus.OK);
    }

    @ExceptionHandler(Throwable.class)
    @ResponseBody
    ResponseEntity<?> handleControllerException(HttpServletRequest request, Throwable ex) {
        log.warn("Handle exception", ex);
        // esb请求错误统一返回200，具体的错误信息放在返回数据里边
        return new ResponseEntity<>(EsbResp.buildCommonFailResp(ErrorCode.SERVICE_INTERNAL_ERROR, i18nService),
            HttpStatus.OK);
    }


    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                         HttpHeaders headers, HttpStatus status,
                                                                         WebRequest request) {
        log.warn("Catch HttpRequestMethodNotSupportedException, cause:{}", ex.getMessage());
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                     HttpHeaders headers, HttpStatus status,
                                                                     WebRequest request) {
        log.warn("Catch HttpMediaTypeNotSupportedException, cause:{}", ex.getMessage());
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
                                                                      HttpHeaders headers, HttpStatus status,
                                                                      WebRequest request) {
        log.warn("Catch HttpMediaTypeNotAcceptableException, cause:{}", ex.getMessage());
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleMissingPathVariable(MissingPathVariableException ex, HttpHeaders headers,
                                                               HttpStatus status, WebRequest request) {
        log.warn("Catch MissingPathVariableException, cause:{}", ex.getMessage());
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.SERVICE_INTERNAL_ERROR, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers, HttpStatus status,
                                                                          WebRequest request) {
        log.warn("Catch MissingServletRequestParameterException, cause:{}", ex.getMessage());
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException ex,
                                                                          HttpHeaders headers, HttpStatus status,
                                                                          WebRequest request) {
        log.warn("Catch ServletRequestBindingException, cause:{}", ex.getMessage());
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleConversionNotSupported(ConversionNotSupportedException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        log.warn("Catch ConversionNotSupportedException, cause:{}", ex.getMessage());
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.SERVICE_INTERNAL_ERROR, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatus status, WebRequest request) {
        log.warn("Catch TypeMismatchException, cause:{}", ex.getMessage());
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        log.warn("Catch HttpMessageNotReadableException, cause:{}", ex.getMessage());
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        log.warn("Catch HttpMessageNotWritableException, cause:{}", ex.getMessage());
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.SERVICE_INTERNAL_ERROR, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        log.warn("Catch MethodArgumentNotValidException, cause:{}", ex.getMessage());
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(MissingServletRequestPartException ex,
                                                                     HttpHeaders headers, HttpStatus status,
                                                                     WebRequest request) {
        log.warn("Catch MissingServletRequestPartException, cause:{}", ex.getMessage());
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers, HttpStatus status,
                                                         WebRequest request) {
        log.warn("Catch BindException, cause:{}", ex.getMessage());
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers,
                                                                   HttpStatus status, WebRequest request) {
        log.warn("Catch NoHandlerFoundException, cause:{}", ex.getMessage());
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex,
                                                                        HttpHeaders headers, HttpStatus status,
                                                                        WebRequest webRequest) {
        log.warn("Catch AsyncRequestTimeoutException, cause:{}", ex.getMessage());
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.SERVICE_INTERNAL_ERROR, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }
}
