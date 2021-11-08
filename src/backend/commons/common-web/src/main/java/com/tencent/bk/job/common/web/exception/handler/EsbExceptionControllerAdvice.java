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
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.exception.UnauthenticatedException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.model.error.ErrorDetailDTO;
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
import org.springframework.validation.BindingResult;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

/**
 * 处理ESB异常
 */
@ControllerAdvice(annotations = {EsbAPI.class})
@Slf4j
public class EsbExceptionControllerAdvice extends ExceptionControllerAdviceBase {
    private final AuthService authService;

    @Autowired
    public EsbExceptionControllerAdvice(AuthService authService) {
        this.authService = authService;
    }

    @ExceptionHandler(Throwable.class)
    @ResponseBody
    ResponseEntity<?> handleException(HttpServletRequest request, Throwable ex) {
        log.error("Handle exception", ex);
        // esb请求错误统一返回200，具体的错误信息放在返回数据里边
        return new ResponseEntity<>(EsbResp.buildCommonFailResp(ErrorCode.INTERNAL_ERROR),
            HttpStatus.OK);
    }

    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    ResponseEntity<?> handleServiceException(HttpServletRequest request, ServiceException ex) {
        log.error("Handle ServiceException", ex);
        // esb请求错误统一返回200，具体的错误信息放在返回数据里边
        return new ResponseEntity<>(EsbResp.buildCommonFailResp(ex), HttpStatus.OK);
    }

    @ExceptionHandler(InternalException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    ResponseEntity<?> handleInternalException(HttpServletRequest request, InternalException ex) {
        String errorMsg = "Handle InternalException, uri: " + request.getRequestURI();
        log.error(errorMsg, ex);
        return new ResponseEntity<>(EsbResp.buildCommonFailResp(ex.getErrorCode()), HttpStatus.OK);
    }

    @ExceptionHandler(PermissionDeniedException.class)
    @ResponseBody
    ResponseEntity<?> handlePermissionDeniedException(HttpServletRequest request, PermissionDeniedException ex) {
        log.info("Handle PermissionDeniedException", ex);
        // esb请求错误统一返回200，具体的错误信息放在返回数据里边
        return new ResponseEntity<>(authService.buildEsbAuthFailResp(ex),
            HttpStatus.OK);
    }

    @ExceptionHandler({InvalidParamException.class})
    @ResponseBody
    ResponseEntity<?> handleInvalidParamException(HttpServletRequest request, InvalidParamException ex) {
        String errorMsg = "Handle InvalidParamException, uri: " + request.getRequestURI();
        log.warn(errorMsg, ex);
        return new ResponseEntity<>(EsbResp.buildCommonFailResp(ex.getErrorCode()), HttpStatus.OK);
    }

    @ExceptionHandler(FailedPreconditionException.class)
    @ResponseBody
    ResponseEntity<?> handleFailedPreconditionException(HttpServletRequest request, FailedPreconditionException ex) {
        String errorMsg = "Handle FailedPreconditionException, uri: " + request.getRequestURI();
        log.info(errorMsg, ex);
        return new ResponseEntity<>(EsbResp.buildCommonFailResp(ex.getErrorCode()), HttpStatus.OK);
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseBody
    ResponseEntity<?> handleNotFoundException(HttpServletRequest request, NotFoundException ex) {
        String errorMsg = "Handle NotFoundException, uri: " + request.getRequestURI();
        log.info(errorMsg, ex);
        return new ResponseEntity<>(EsbResp.buildCommonFailResp(ex.getErrorCode()), HttpStatus.OK);
    }

    @ExceptionHandler(AlreadyExistsException.class)
    @ResponseBody
    ResponseEntity<?> handleAlreadyExistsException(HttpServletRequest request, AlreadyExistsException ex) {
        String errorMsg = "Handle AlreadyExistsException, uri: " + request.getRequestURI();
        log.info(errorMsg, ex);
        return new ResponseEntity<>(EsbResp.buildCommonFailResp(ex.getErrorCode()), HttpStatus.OK);
    }

    @ExceptionHandler(UnauthenticatedException.class)
    @ResponseBody
    ResponseEntity<?> handleUnauthenticatedException(HttpServletRequest request, UnauthenticatedException ex) {
        String errorMsg = "Handle UnauthenticatedException, uri: " + request.getRequestURI();
        log.error(errorMsg, ex);
        return new ResponseEntity<>(EsbResp.buildCommonFailResp(ex.getErrorCode()), HttpStatus.OK);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    ResponseEntity<?> handleConstraintViolationException(HttpServletRequest request,
                                                         ConstraintViolationException ex) {
        ErrorDetailDTO errorDetail = buildErrorDetail(ex);
        log.warn("handleConstraintViolationException - errorDetail: {}", errorDetail);
        EsbResp<?> resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                         HttpHeaders headers, HttpStatus status,
                                                                         WebRequest request) {
        log.warn("Handle HttpRequestMethodNotSupportedException", ex);
        EsbResp<?> resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                     HttpHeaders headers, HttpStatus status,
                                                                     WebRequest request) {
        log.warn("Handle HttpMediaTypeNotSupportedException", ex);
        EsbResp<?> resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
                                                                      HttpHeaders headers, HttpStatus status,
                                                                      WebRequest request) {
        log.warn("Handle HttpMediaTypeNotAcceptableException", ex);
        EsbResp<?> resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleMissingPathVariable(MissingPathVariableException ex, HttpHeaders headers,
                                                               HttpStatus status, WebRequest request) {
        log.warn("Handle MissingPathVariableException", ex);
        EsbResp<?> resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers, HttpStatus status,
                                                                          WebRequest request) {
        log.warn("Handle MissingServletRequestParameterException", ex);
        EsbResp<?> resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException ex,
                                                                          HttpHeaders headers, HttpStatus status,
                                                                          WebRequest request) {
        log.warn("Handle ServletRequestBindingException", ex);
        EsbResp<?> resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleConversionNotSupported(ConversionNotSupportedException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        log.warn("Handle ConversionNotSupportedException", ex);
        EsbResp<?> resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatus status, WebRequest request) {
        log.warn("Handle TypeMismatchException", ex);
        EsbResp<?> resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        log.warn("Handle HttpMessageNotReadableException", ex);
        EsbResp<?> resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        log.warn("Handle HttpMessageNotWritableException", ex);
        EsbResp<?> resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        BindingResult bindingResult = ex.getBindingResult();
        ErrorDetailDTO errorDetail = buildErrorDetail(ex);
        log.warn("HandleMethodArgumentNotValid - errorDetail: {}", errorDetail);
        EsbResp<?> resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleMissingServletRequestPart(MissingServletRequestPartException ex,
                                                                     HttpHeaders headers, HttpStatus status,
                                                                     WebRequest request) {
        log.warn("Handle MissingServletRequestPartException", ex);
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers, HttpStatus status,
                                                         WebRequest request) {
        log.warn("Handle BindException", ex);
        EsbResp resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }



    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers,
                                                                   HttpStatus status, WebRequest request) {
        log.warn("Handle NoHandlerFoundException", ex);
        EsbResp<?> resp = EsbResp.buildCommonFailResp(ErrorCode.BAD_REQUEST);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex,
                                                                        HttpHeaders headers, HttpStatus status,
                                                                        WebRequest webRequest) {
        log.error("Handle AsyncRequestTimeoutException", ex);
        EsbResp<?> resp = EsbResp.buildCommonFailResp(ErrorCode.INTERNAL_ERROR);
        return new ResponseEntity<>(resp, HttpStatus.OK);
    }
}
