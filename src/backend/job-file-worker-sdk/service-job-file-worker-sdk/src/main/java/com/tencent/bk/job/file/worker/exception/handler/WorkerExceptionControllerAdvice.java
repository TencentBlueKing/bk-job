package com.tencent.bk.job.file.worker.exception.handler;

import com.tencent.bk.job.common.annotation.WorkerAPI;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.error.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import javax.servlet.http.HttpServletRequest;

/**
 * 处理Worker接口异常
 */
@ControllerAdvice(annotations = {WorkerAPI.class})
@Slf4j
public class WorkerExceptionControllerAdvice extends ResponseEntityExceptionHandler {

    @ExceptionHandler(Throwable.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    InternalResponse<?> handleException(HttpServletRequest request, Throwable ex) {
        String exceptionInfo = "Handle Exception, uri: " + request.getRequestURI();
        log.error(exceptionInfo, ex);
        return InternalResponse.buildCommonFailResp(ErrorType.INTERNAL, ErrorCode.INTERNAL_ERROR);
    }

}
