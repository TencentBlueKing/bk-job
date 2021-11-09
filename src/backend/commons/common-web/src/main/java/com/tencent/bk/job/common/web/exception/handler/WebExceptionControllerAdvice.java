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
import com.tencent.bk.job.common.exception.AlreadyExistsException;
import com.tencent.bk.job.common.exception.FailedPreconditionException;
import com.tencent.bk.job.common.exception.InternalException;
import com.tencent.bk.job.common.exception.InvalidParamException;
import com.tencent.bk.job.common.exception.NotFoundException;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.exception.UnauthenticatedException;
import com.tencent.bk.job.common.iam.exception.PermissionDeniedException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.Response;
import com.tencent.bk.job.common.model.error.ErrorDetailDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

@ControllerAdvice(annotations = {WebAPI.class})
@Slf4j
public class WebExceptionControllerAdvice extends ExceptionControllerAdviceBase {
    private final WebAuthService webAuthService;

    @Autowired
    public WebExceptionControllerAdvice(WebAuthService webAuthService) {
        this.webAuthService = webAuthService;
    }

    @ExceptionHandler(Throwable.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    Response<?> handleException(HttpServletRequest request, Throwable ex) {
        String errorMsg = "Handle Exception, uri: " + request.getRequestURI();
        log.error(errorMsg, ex);
        return Response.buildCommonFailResp(ErrorCode.INTERNAL_ERROR);
    }

    @ExceptionHandler(PermissionDeniedException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN)
    Response<?> handlePermissionDeniedException(HttpServletRequest request,
                                                PermissionDeniedException ex) {
        AuthResult authResult = ex.getAuthResult();
        log.info("Handle PermissionDeniedException, uri: {}, authResult: {}",
            request.getRequestURI(), authResult);
        if (StringUtils.isEmpty(authResult.getApplyUrl())) {
            authResult.setApplyUrl(webAuthService.getApplyUrl(authResult.getRequiredActionResources()));
        }
        return Response.buildAuthFailResp(webAuthService.toAuthResultVO(ex.getAuthResult()));
    }

    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    ResponseEntity<?> handleServiceException(HttpServletRequest request, ServiceException ex) {
        String errorMsg = "Handle ServiceException, uri: " + request.getRequestURI();
        log.error(errorMsg, ex);
        return new ResponseEntity<>(Response.buildCommonFailResp(ex), HttpStatus.OK);
    }

    @ExceptionHandler(InternalException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    Response<?> handleInternalException(HttpServletRequest request, InternalException ex) {
        String errorMsg = "Handle InternalException, uri: " + request.getRequestURI();
        log.error(errorMsg, ex);
        return Response.buildCommonFailResp(ex.getErrorCode());
    }

    @ExceptionHandler({InvalidParamException.class})
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    Response<?> handleInvalidParamException(HttpServletRequest request, InvalidParamException ex) {
        String errorMsg = "Handle InvalidParamException, uri: " + request.getRequestURI();
        log.warn(errorMsg, ex);
        return Response.buildCommonFailResp(ex.getErrorCode());
    }

    @ExceptionHandler(FailedPreconditionException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    Response<?> handleBusinessException(HttpServletRequest request, FailedPreconditionException ex) {
        String errorMsg = "Handle FailedPreconditionException, uri: " + request.getRequestURI();
        log.info(errorMsg, ex);
        return Response.buildCommonFailResp(ex.getErrorCode());
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    Response<?> handleNotFoundException(HttpServletRequest request, NotFoundException ex) {
        String errorMsg = "Handle NotFoundException, uri: " + request.getRequestURI();
        log.info(errorMsg, ex);
        return Response.buildCommonFailResp(ex.getErrorCode());
    }

    @ExceptionHandler(AlreadyExistsException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    Response<?> handleAlreadyExistsException(HttpServletRequest request, AlreadyExistsException ex) {
        String errorMsg = "Handle AlreadyExistsException, uri: " + request.getRequestURI();
        log.info(errorMsg, ex);
        return Response.buildCommonFailResp(ex.getErrorCode());
    }

    @ExceptionHandler(UnauthenticatedException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    Response<?> handleUnauthenticatedException(HttpServletRequest request, UnauthenticatedException ex) {
        String errorMsg = "Handle UnauthenticatedException, uri: " + request.getRequestURI();
        log.error(errorMsg, ex);
        return Response.buildCommonFailResp(ex.getErrorCode());
    }

    @SuppressWarnings("all")
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        BindingResult bindingResult = ex.getBindingResult();
        ErrorDetailDTO errorDetail = buildErrorDetail(ex);
        log.warn("HandleMethodArgumentNotValid - errorDetail: {}", errorDetail);
        Response<?> resp = Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM, errorDetail);
        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    ResponseEntity<?> handleConstraintViolationException(HttpServletRequest request,
                                                         ConstraintViolationException ex) {
        ErrorDetailDTO errorDetail = buildErrorDetail(ex);
        log.warn("handleConstraintViolationException - errorDetail: {}", errorDetail);
        Response<?> resp = Response.buildCommonFailResp(ErrorCode.ILLEGAL_PARAM, errorDetail);
        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }
}
