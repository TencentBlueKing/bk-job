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

import com.tencent.bk.job.common.annotation.EsbAPI;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.EsbResp;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.model.error.BadRequestDetail;
import com.tencent.bk.job.common.model.error.ErrorDetail;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.spi.nodenameprovider.JavaBeanProperty;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
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

/**
 * 处理ESB异常
 */
@ControllerAdvice(annotations = {EsbAPI.class})
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
        return new ResponseEntity<>(EsbResp.buildCommonFailResp(ex.getErrorCode(), i18nService, ex.getErrorParams()),
            HttpStatus.OK);
    }

    @ExceptionHandler(Throwable.class)
    @ResponseBody
    ResponseEntity<?> handleControllerException(HttpServletRequest request, Throwable ex) {
        log.warn("Handle exception", ex);
        // esb请求错误统一返回200，具体的错误信息放在返回数据里边
        return new ResponseEntity<>(EsbResp.buildCommonFailResp(ErrorCode.SERVICE_INTERNAL_ERROR, i18nService),
            HttpStatus.OK);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    ResponseEntity<?> handleConstraintViolationException(HttpServletRequest request,
                                                         ConstraintViolationException ex) {
        log.warn("Handle ConstraintViolationException", ex);
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                         HttpHeaders headers, HttpStatus status,
                                                                         WebRequest request) {
        log.warn("Handle HttpRequestMethodNotSupportedException", ex);
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                     HttpHeaders headers, HttpStatus status,
                                                                     WebRequest request) {
        log.warn("Handle HttpMediaTypeNotSupportedException", ex);
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
                                                                      HttpHeaders headers, HttpStatus status,
                                                                      WebRequest request) {
        log.warn("Handle HttpMediaTypeNotAcceptableException", ex);
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleMissingPathVariable(MissingPathVariableException ex, HttpHeaders headers,
                                                               HttpStatus status, WebRequest request) {
        log.warn("Handle MissingPathVariableException", ex);
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.SERVICE_INTERNAL_ERROR, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers, HttpStatus status,
                                                                          WebRequest request) {
        log.warn("Handle MissingServletRequestParameterException", ex);
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException ex,
                                                                          HttpHeaders headers, HttpStatus status,
                                                                          WebRequest request) {
        log.warn("Handle ServletRequestBindingException", ex);
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleConversionNotSupported(ConversionNotSupportedException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        log.warn("Handle ConversionNotSupportedException", ex);
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.SERVICE_INTERNAL_ERROR, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatus status, WebRequest request) {
        log.warn("Handle TypeMismatchException", ex);
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        log.warn("Handle HttpMessageNotReadableException", ex);
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        log.warn("Handle HttpMessageNotWritableException", ex);
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.SERVICE_INTERNAL_ERROR, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        log.warn("Handle MethodArgumentNotValidException", ex);
        BindingResult bindingResult = ex.getBindingResult();

        EsbResp resp = null;
        for (FieldError fieldError : bindingResult.getFieldErrors()) {
            // fail fast
            BadRequestDetail badRequestDetail = new BadRequestDetail(fieldError.getField(),
                null,
                fieldError.getDefaultMessage());
            ErrorDetail errorDetail = new ErrorDetail(badRequestDetail);
            resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, errorDetail, i18nService);
            break;
        }
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    private String getFieldName(String field) {
        if (!field.contains(".")) {
            return field;
        }
        String[] fieldPath = field.split("\\.");
        if (fieldPath.length == 0) {
            return field;
        }
        return fieldPath[fieldPath.length - 1];
    }

    private static class JavaBeanPropertyImpl implements JavaBeanProperty {
        private final Class<?> declaringClass;
        private final String name;

        private JavaBeanPropertyImpl(Class<?> declaringClass, String name) {
            this.declaringClass = declaringClass;
            this.name = name;
        }

        @Override
        public Class<?> getDeclaringClass() {
            return declaringClass;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestPart(MissingServletRequestPartException ex,
                                                                     HttpHeaders headers, HttpStatus status,
                                                                     WebRequest request) {
        log.warn("Handle MissingServletRequestPartException", ex);
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers, HttpStatus status,
                                                         WebRequest request) {
        log.warn("Handle BindException", ex);
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers,
                                                                   HttpStatus status, WebRequest request) {
        log.warn("Handle NoHandlerFoundException", ex);
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<Object> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex,
                                                                        HttpHeaders headers, HttpStatus status,
                                                                        WebRequest webRequest) {
        log.warn("Handle AsyncRequestTimeoutException", ex);
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.SERVICE_INTERNAL_ERROR, i18nService);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }
}
