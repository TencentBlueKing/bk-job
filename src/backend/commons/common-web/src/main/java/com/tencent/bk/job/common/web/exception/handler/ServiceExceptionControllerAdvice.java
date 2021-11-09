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

import com.tencent.bk.job.common.annotation.InternalAPI;
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
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.error.ErrorDetailDTO;
import com.tencent.bk.job.common.model.error.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.context.request.WebRequest;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;

@ControllerAdvice(annotations = {InternalAPI.class})
@Slf4j
public class ServiceExceptionControllerAdvice extends ExceptionControllerAdviceBase {

    @ExceptionHandler(Throwable.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    InternalResponse<?> handleException(HttpServletRequest request, Throwable ex) {
        String exceptionInfo = "Handle Exception, uri: " + request.getRequestURI();
        log.error(exceptionInfo, ex);
        return InternalResponse.buildCommonFailResp(ErrorType.INTERNAL, ErrorCode.INTERNAL_ERROR);
    }

    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    ResponseEntity<?> handleServiceException(HttpServletRequest request, ServiceException ex) {
        String exceptionInfo = "Handle ServiceException, uri: " + request.getRequestURI();
        log.warn(exceptionInfo, ex);
        return new ResponseEntity<>(InternalResponse.buildCommonFailResp(ex), HttpStatus.OK);
    }

    @ExceptionHandler(PermissionDeniedException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.FORBIDDEN)
    InternalResponse<?> handlePermissionDeniedException(HttpServletRequest request,
                                                        PermissionDeniedException ex) {
        log.info("Handle PermissionDeniedException, uri: {}, authResult: {}",
            request.getRequestURI(), ex.getAuthResult());
        return InternalResponse.buildAuthFailResp(AuthResult.toAuthResultDTO(ex.getAuthResult()));
    }

    @ExceptionHandler(InternalException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    InternalResponse<?> handleInternalException(HttpServletRequest request, InternalException ex) {
        String errorMsg = "Handle InternalException, uri: " + request.getRequestURI();
        log.error(errorMsg, ex);
        return InternalResponse.buildCommonFailResp(ex);
    }

    @ExceptionHandler({InvalidParamException.class})
    @ResponseBody
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    InternalResponse<?> handleInvalidParamException(HttpServletRequest request, InvalidParamException ex) {
        String errorMsg = "Handle InvalidParamException, uri: " + request.getRequestURI();
        log.warn(errorMsg, ex);
        return InternalResponse.buildCommonFailResp(ex);
    }

    @ExceptionHandler(FailedPreconditionException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    InternalResponse<?> handleBusinessException(HttpServletRequest request, FailedPreconditionException ex) {
        String errorMsg = "Handle FailedPreconditionException, uri: " + request.getRequestURI();
        log.info(errorMsg, ex);
        return InternalResponse.buildCommonFailResp(ex);
    }

    @ExceptionHandler(NotFoundException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.NOT_FOUND)
    InternalResponse<?> handleNotFoundException(HttpServletRequest request, NotFoundException ex) {
        String errorMsg = "Handle NotFoundException, uri: " + request.getRequestURI();
        log.info(errorMsg, ex);
        return InternalResponse.buildCommonFailResp(ex);
    }

    @ExceptionHandler(AlreadyExistsException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.CONFLICT)
    InternalResponse<?> handleAlreadyExistsException(HttpServletRequest request, AlreadyExistsException ex) {
        String errorMsg = "Handle AlreadyExistsException, uri: " + request.getRequestURI();
        log.info(errorMsg, ex);
        return InternalResponse.buildCommonFailResp(ex);
    }

    @ExceptionHandler(UnauthenticatedException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    InternalResponse<?> handleUnauthenticatedException(HttpServletRequest request, UnauthenticatedException ex) {
        String errorMsg = "Handle UnauthenticatedException, uri: " + request.getRequestURI();
        log.error(errorMsg, ex);
        return InternalResponse.buildCommonFailResp(ex);
    }

    @SuppressWarnings("all")
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers, HttpStatus status,
                                                                  WebRequest request) {
        ErrorDetailDTO errorDetail = buildErrorDetail(ex);
        log.warn("HandleMethodArgumentNotValid - errorDetail: {}", errorDetail);
        InternalResponse<?> resp = InternalResponse.buildCommonFailResp(ErrorType.INVALID_PARAM,
            ErrorCode.ILLEGAL_PARAM, errorDetail);
        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    @ResponseBody
    ResponseEntity<?> handleConstraintViolationException(HttpServletRequest request,
                                                         ConstraintViolationException ex) {
        ErrorDetailDTO errorDetail = buildErrorDetail(ex);
        log.warn("handleConstraintViolationException - errorDetail: {}", errorDetail);
        InternalResponse<?> resp = InternalResponse.buildCommonFailResp(ErrorType.INVALID_PARAM,
            ErrorCode.ILLEGAL_PARAM, errorDetail);
        return new ResponseEntity<>(resp, HttpStatus.BAD_REQUEST);
    }
}
