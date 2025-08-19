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

package com.tencent.bk.job.common.web.exception.handler;

import com.google.common.util.concurrent.UncheckedExecutionException;
import com.tencent.bk.job.common.annotation.EsbV4API;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.esb.model.v4.EsbV4Response;
import com.tencent.bk.job.common.esb.model.v4.V4ErrorCodeEnum;
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.MissingParameterException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ResourceExhaustedException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.exception.UnauthenticatedException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.model.error.ErrorDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;
import org.springframework.beans.ConversionNotSupportedException;
import org.springframework.beans.TypeMismatchException;
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
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

/**
 * ESB V4接口的全局异常处理器
 */
@ControllerAdvice(annotations = {EsbV4API.class})
@Slf4j
public class EsbV4ExceptionControllerAdvice extends ExceptionControllerAdviceBase {

    private final AuthService authService;

    public EsbV4ExceptionControllerAdvice(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 当所有异常处理函数都没捕获到异常，最宽泛的异常处理器
     */
    @ExceptionHandler({Throwable.class})
    public ResponseEntity<?> handleException(Throwable ex, HttpServletRequest request) {
        String errorMsg = MessageFormatter.format(
            "Handle exception, uri={}", request.getRequestURI()
        ).getMessage();
        log.error(errorMsg, ex);
        if (ex instanceof UncheckedExecutionException) {
            if (ex.getCause() instanceof ServiceException) {
                ServiceException e = (ServiceException) ex.getCause();
                return new ResponseEntity<>(
                    EsbV4Response.buildFailedResponse(
                        V4ErrorCodeEnum.INTERNAL,
                        e.getErrorCode(),
                        e.getErrorParams()
                    ),
                    HttpStatus.INTERNAL_SERVER_ERROR
                );
            }
        }
        // 未被Job处理过的异常，属于内部异常
        return new ResponseEntity<>(
            EsbV4Response.buildFailedResponse(V4ErrorCodeEnum.INTERNAL, ErrorCode.INTERNAL_ERROR),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    /**
     * 运行时异常，500
     * 若是非服务端的自定义异常继承于ServiceException，确保能被更具体的异常处理器处理
     */
    @ExceptionHandler(ServiceException.class)
    ResponseEntity<?> handleServiceException(HttpServletRequest request, ServiceException ex) {
        String errorMsg = MessageFormatter.format(
            "Handle serviceException, uri={}", request.getRequestURI()
        ).getMessage();
        log.error(errorMsg, ex);
        EsbV4Response<Object> resp = EsbV4Response.buildFailedResponse(
            V4ErrorCodeEnum.INTERNAL,
            ex.getErrorCode(),
            ex.getErrorParams()
        );
        return new ResponseEntity<>(resp, HttpStatus.INTERNAL_SERVER_ERROR);

    }

    /**
     * 内部服务器异常
     */
    @ExceptionHandler(InternalException.class)
    ResponseEntity<?> handleInternalException(HttpServletRequest request, InternalException ex) {
        String errorMsg = "Handle InternalException, uri: " + request.getRequestURI();
        log.error(errorMsg, ex);
        return new ResponseEntity<>(
            EsbV4Response.buildFailedResponse(V4ErrorCodeEnum.INTERNAL, ex.getErrorCode(), ex.getErrorParams()),
            HttpStatus.INTERNAL_SERVER_ERROR
        );
    }

    /**
     * 参数不合法
     */
    @ExceptionHandler({InvalidParamException.class})
    public ResponseEntity<?> handleInvalidParamException(InvalidParamException ex, HttpServletRequest request) {
        String errorMsg = "Handle InvalidParamException, uri: " + request.getRequestURI();
        log.warn(errorMsg, ex);
        EsbV4Response<Object> body = EsbV4Response.buildFailedResponse(
            V4ErrorCodeEnum.INVALID_ARGUMENT,
            ex.getErrorCode(),
            ex.getErrorParams()
        );
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * 权限校验不通过
     */
    @ExceptionHandler({PermissionDeniedException.class})
    public ResponseEntity<?> handlePermissionDeniedException(PermissionDeniedException ex,
                                                             HttpServletRequest request) {
        String errorMsg = MessageFormatter.format(
            "Handle PermissionDeniedException, uri={}",
            request.getRequestURI()
        ).getMessage();
        log.warn(errorMsg, ex);
        EsbV4Response<Object> body = EsbV4Response.buildPermissionDeniedResponse(
            authService.buildPermissionDetailByPermissionApplyDTO(ex)
        );
        return new ResponseEntity<>(body, HttpStatus.FORBIDDEN);
    }

    /**
     * 当前操作无法在当前系统状态下执行，400
     */
    @ExceptionHandler(FailedPreconditionException.class)
    ResponseEntity<?> handleFailedPreconditionException(HttpServletRequest request, FailedPreconditionException ex) {
        String errorMsg = "Handle FailedPreconditionException, uri: " + request.getRequestURI();
        log.info(errorMsg, ex);
        EsbV4Response<Object> body = EsbV4Response.buildFailedResponse(
            V4ErrorCodeEnum.FAILED_PRECONDITION,
            ex.getErrorCode(),
            ex.getErrorParams()
        );
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * 资源不存在，404
     */
    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<?> handleNotFoundException(HttpServletRequest request, NotFoundException ex) {
        String errorMsg = "Handle NotFoundException, uri: " + request.getRequestURI();
        log.info(errorMsg, ex);
        EsbV4Response<Object> body = EsbV4Response.buildFailedResponse(
            V4ErrorCodeEnum.NOT_FOUND,
            ex.getErrorCode(), ex.getErrorParams()
        );
        return new ResponseEntity<>(body, HttpStatus.NOT_FOUND);
    }

    /**
     * 资源已存在，409
     */
    @ExceptionHandler(AlreadyExistsException.class)
    ResponseEntity<?> handleAlreadyExistsException(HttpServletRequest request, AlreadyExistsException ex) {
        String errorMsg = "Handle AlreadyExistsException, uri: " + request.getRequestURI();
        log.info(errorMsg, ex);
        EsbV4Response<Object> body = EsbV4Response.buildFailedResponse(
            V4ErrorCodeEnum.ALREADY_EXISTS,
            ex.getErrorCode(),
            ex.getErrorParams()
        );
        return new ResponseEntity<>(body, HttpStatus.CONFLICT);
    }

    /**
     * 未认证，401
     */
    @ExceptionHandler(UnauthenticatedException.class)
    ResponseEntity<?> handleUnauthenticatedException(HttpServletRequest request, UnauthenticatedException ex) {
        String errorMsg = "Handle UnauthenticatedException, uri: " + request.getRequestURI();
        log.warn(errorMsg, ex);
        EsbV4Response<Object> body = EsbV4Response.buildFailedResponse(
            V4ErrorCodeEnum.UNAUTHENTICATED,
            ex.getErrorCode(),
            ex.getErrorParams()
        );
        return new ResponseEntity<>(body, HttpStatus.UNAUTHORIZED);
    }

    /**
     * 缺少参数，400
     */
    @ExceptionHandler(MissingParameterException.class)
    ResponseEntity<?> handleMissingParameterException(HttpServletRequest request,
                                                      MissingParameterException ex) {
        String errorMsg = "Handle MissingParameterException , uri: " + request.getRequestURI();
        log.info(errorMsg, ex);
        EsbV4Response<Object> body = EsbV4Response.buildFailedResponse(
            V4ErrorCodeEnum.INVALID_ARGUMENT,
            ex.getErrorCode(),
            ex.getErrorParams()
        );
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * 参数校验失败，400
     */
    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<?> handleConstraintViolationException(HttpServletRequest request, ConstraintViolationException ex) {
        ErrorDetailDTO errorDetail = buildErrorDetail(ex);
        log.info(
            "handle ConstraintViolationException, uri: {}, errorDetail: {}",
            request.getRequestURI(),
            errorDetail
        );
        EsbV4Response<?> resp = EsbV4Response.paramValidateFail(V4ErrorCodeEnum.INVALID_ARGUMENT, errorDetail);
        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }

    /**
     * 业务使用资源超出配额，429
     */
    @ExceptionHandler({ResourceExhaustedException.class})
    ResponseEntity<?> handleResourceExhaustedException(HttpServletRequest request, ResourceExhaustedException ex) {
        String errorMsg = "Handle ResourceExhaustedException, uri: " + request.getRequestURI();
        if (log.isDebugEnabled()) {
            log.debug(errorMsg, ex);
        } else {
            log.info(errorMsg);
        }
        EsbV4Response<Object> body = EsbV4Response.buildFailedResponse(
            V4ErrorCodeEnum.RESOURCE_EXHAUSTED,
            ex.getErrorCode(),
            ex.getErrorParams()
        );
        return new ResponseEntity<>(body, HttpStatus.TOO_MANY_REQUESTS);
    }


    /**
     * Spring控制器方法入参校验失败
     */
    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        ErrorDetailDTO errorDetail = buildErrorDetail(ex);
        log.info("Handle MethodArgumentNotValid, errorDetail: {}", errorDetail);
        EsbV4Response<?> resp = EsbV4Response.paramValidateFail(V4ErrorCodeEnum.INVALID_ARGUMENT, errorDetail);
        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException ex,
                                                                         HttpHeaders headers, HttpStatus status,
                                                                         WebRequest request) {
        log.info("Handle HttpRequestMethodNotSupportedException", ex);
        EsbV4Response<?> resp = EsbV4Response.badRequestResponse(ErrorCode.NOT_SUPPORTED_HTTP_REQUEST_METHOD);
        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex,
                                                                     HttpHeaders headers, HttpStatus status,
                                                                     WebRequest request) {
        log.warn("Handle HttpMediaTypeNotSupportedException", ex);
        EsbV4Response<?> resp = EsbV4Response.badRequestResponse(ErrorCode.NOT_SUPPORTED_MEDIA_TYPE);
        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(HttpMediaTypeNotAcceptableException ex,
                                                                      HttpHeaders headers, HttpStatus status,
                                                                      WebRequest request) {
        log.warn("Handle HttpMediaTypeNotAcceptableException", ex);
        EsbV4Response<?> resp = EsbV4Response.badRequestResponse();
        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleMissingPathVariable(MissingPathVariableException ex, HttpHeaders headers,
                                                               HttpStatus status, WebRequest request) {
        log.warn("Handle MissingPathVariableException", ex);
        EsbV4Response<?> resp = EsbV4Response.badRequestResponse(
            ErrorCode.MISSING_PATH_VARIABLE,
            new String[]{ex.getVariableName()}
        );
        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException ex,
                                                                          HttpHeaders headers, HttpStatus status,
                                                                          WebRequest request) {
        log.info("Handle MissingServletRequestParameterException", ex);
        EsbV4Response<?> resp = EsbV4Response.badRequestResponse(
            ErrorCode.MISSING_PARAM_WITH_PARAM_NAME,
            new String[]{ex.getParameterName()}
        );
        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException ex,
                                                                          HttpHeaders headers, HttpStatus status,
                                                                          WebRequest request) {
        log.warn("Handle ServletRequestBindingException", ex);
        EsbV4Response<?> resp = EsbV4Response.badRequestResponse();
        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleConversionNotSupported(ConversionNotSupportedException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        log.warn("Handle ConversionNotSupportedException", ex);
        EsbV4Response<?> resp = EsbV4Response.badRequestResponse();
        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatus status, WebRequest request) {
        log.warn("Handle TypeMismatchException", ex);
        EsbV4Response<?> resp = EsbV4Response.badRequestResponse(
            ErrorCode.PARAMETER_TYPE_ERROR,
            new String[]{ex.getPropertyName()}
        );
        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        log.warn("Handle HttpMessageNotReadableException", ex);
        EsbV4Response<?> resp = EsbV4Response.badRequestResponse();
        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleHttpMessageNotWritable(HttpMessageNotWritableException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        log.warn("Handle HttpMessageNotWritableException", ex);
        EsbV4Response<?> resp = EsbV4Response.badRequestResponse();
        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleMissingServletRequestPart(MissingServletRequestPartException ex,
                                                                     HttpHeaders headers, HttpStatus status,
                                                                     WebRequest request) {
        log.warn("Handle MissingServletRequestPartException", ex);
        EsbV4Response<?> resp = EsbV4Response.badRequestResponse();
        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleBindException(BindException ex, HttpHeaders headers, HttpStatus status,
                                                         WebRequest request) {
        log.warn("Handle BindException", ex);
        EsbV4Response<?> resp = EsbV4Response.badRequestResponse();
        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpHeaders headers,
                                                                   HttpStatus status, WebRequest request) {
        log.warn("Handle NoHandlerFoundException", ex);
        EsbV4Response<?> resp = EsbV4Response.badRequestResponse();
        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }

    @Override
    @SuppressWarnings("all")
    protected ResponseEntity<Object> handleAsyncRequestTimeoutException(AsyncRequestTimeoutException ex,
                                                                        HttpHeaders headers, HttpStatus status,
                                                                        WebRequest webRequest) {
        log.error("Handle AsyncRequestTimeoutException", ex);
        EsbV4Response<?> resp = EsbV4Response.buildFailedResponse(V4ErrorCodeEnum.INTERNAL, ErrorCode.INTERNAL_ERROR);
        return new ResponseEntity<>(resp, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
