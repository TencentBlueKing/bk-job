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

package com.tencent.bk.job.manage.common;

import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.ServiceException;
import com.tencent.bk.job.common.i18n.MessageI18nService;
import com.tencent.bk.job.common.iam.exception.InSufficientPermissionException;
import com.tencent.bk.job.common.iam.model.AuthResult;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.model.ServiceResponse;
import com.tencent.bk.job.common.model.permission.AuthResultVO;
import com.tencent.bk.job.common.web.exception.HttpStatusServiceException;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

@ControllerAdvice({"com.tencent.bk.job.manage.api.web", "com.tencent.bk.job.manage.api.inner"})
@Slf4j
public class ExceptionControllerAdvice extends ResponseEntityExceptionHandler {
    private final MessageI18nService i18nService;
    private final WebAuthService webAuthService;

    @Autowired
    public ExceptionControllerAdvice(MessageI18nService i18nService, WebAuthService webAuthService) {
        this.i18nService = i18nService;
        this.webAuthService = webAuthService;
    }

    @ExceptionHandler(ServiceException.class)
    @ResponseBody
    ResponseEntity<?> handleControllerServiceException(HttpServletRequest request, ServiceException ex) {
        log.error("Handle service exception", ex);
        if (ex instanceof HttpStatusServiceException) {
            HttpStatusServiceException httpStatusServiceException = (HttpStatusServiceException) ex;
            return new ResponseEntity<>(ServiceResponse.buildCommonFailResp(httpStatusServiceException.getErrorCode()
                , i18nService), httpStatusServiceException.getHttpStatus());
        } else if (ex instanceof InSufficientPermissionException) {
            InSufficientPermissionException inSufficientPermissionException = (InSufficientPermissionException) ex;
            AuthResultVO authResultVO = null;
            if (inSufficientPermissionException.getAuthResult() != null) {
                AuthResult authResult = inSufficientPermissionException.getAuthResult();
                log.debug("Insufficient permission, authResult: {}", authResult);
                if (StringUtils.isEmpty(authResult.getApplyUrl())) {
                    authResult.setApplyUrl(webAuthService.getApplyUrl(authResult.getRequiredActionResources()));
                }
                authResultVO = webAuthService.toAuthResultVO(authResult);
            } else if (ex.getErrorParams()[0] != null && ex.getErrorParams()[0] instanceof AuthResultVO) {
                authResultVO = (AuthResultVO) ex.getErrorParams()[0];
            }
            return new ResponseEntity<>(ServiceResponse.buildAuthFailResp(authResultVO), HttpStatus.OK);
        } else {
            if (ex.getErrorCode() > 0) {
                return new ResponseEntity<>(ServiceResponse.buildCommonFailResp(ex.getErrorCode(),
                    ex.getErrorParams(), i18nService), HttpStatus.OK);
            } else {
                return new ResponseEntity<>(ServiceResponse.buildCommonFailResp(ex.getErrorMsg()), HttpStatus.OK);
            }
        }
    }

    @ExceptionHandler(Throwable.class)
    @ResponseBody
    ResponseEntity<?> handleControllerException(HttpServletRequest request, Throwable ex) {
        logRequest(request);
        log.error("Handle exception", ex);
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
            logRequest(request);
            return new ResponseEntity<>(ServiceResponse.buildCommonFailResp(ErrorCode.SERVICE_INTERNAL_ERROR,
                i18nService), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseBody
    ResponseEntity<?> handleControllerException(HttpServletRequest request, MaxUploadSizeExceededException ex) {
        logRequest(request);
        log.error("Handle MaxUploadSizeExceededException, maxFileSize:{}", ex.getMaxUploadSize());
        long maxFileSize = ex.getMaxUploadSize();
        String fileSizeDesc = (maxFileSize << 20) + "G";
        return new ResponseEntity<>(ServiceResponse.buildCommonFailResp(ErrorCode.UPLOAD_FILE_MAX_SIZE_EXCEEDED,
            new String[]{fileSizeDesc}, i18nService),
            HttpStatus.OK);
    }

    private HttpStatus getStatus(HttpServletRequest request) {
        Integer statusCode = (Integer) request.getAttribute("javax.servlet.error.status_code");
        if (statusCode == null) {
            return HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return HttpStatus.valueOf(statusCode);
    }

    private void logRequest(HttpServletRequest request) {
        try {
            if (log.isDebugEnabled()) {
                Map<String, String> headerMap = new HashMap<>();
                Enumeration<String> headerNames = request.getHeaderNames();
                while (headerNames.hasMoreElements()) {
                    String headName = headerNames.nextElement();
                    headerMap.put(headName, request.getHeader(headName));
                }
                log.debug("request=({},{},{},{},{},{})", request.getRemoteHost(), request.getMethod(),
                    request.getRequestURL().append(request.getQueryString()).toString(), request.getParameterMap(),
                    headerMap, request.getContentLength());
            } else {
                log.info("request=({},{},{},{})", request.getRemoteHost(), request.getMethod(),
                    request.getRequestURL().append(request.getQueryString()).toString(), request.getContentLength());
            }
        } catch (Throwable t) {
            log.warn("Fail to log", t);
        }
    }
}
