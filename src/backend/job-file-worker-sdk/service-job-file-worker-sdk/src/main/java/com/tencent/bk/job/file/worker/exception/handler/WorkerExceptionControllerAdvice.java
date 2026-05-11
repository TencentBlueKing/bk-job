package com.tencent.bk.job.file.worker.exception.handler;

import com.tencent.bk.job.common.annotation.WorkerAPI;
import com.tencent.bk.job.common.constant.ErrorCode;
import com.tencent.bk.job.common.exception.UnauthenticatedException;
import com.tencent.bk.job.common.model.InternalResponse;
import com.tencent.bk.job.common.model.error.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 处理Worker接口异常
 */
@ControllerAdvice(annotations = {WorkerAPI.class})
@Slf4j
public class WorkerExceptionControllerAdvice extends ResponseEntityExceptionHandler {

    /**
     * 认证失败异常（如三方文件源凭证错误）：保留原始 errorCode/errorType 以 HTTP 200 返回结构化响应，
     * 避免被上游 RestTemplate 默认错误处理器吞掉响应体，便于 file-gateway 解析后还原成
     * {@link UnauthenticatedException} 继续向用户透传，最终在 web 层映射为 HTTP 401。
     * <p>
     * 仅针对此类异常做特殊处理；其它异常仍走下面的兜底 handler 返回 HTTP 500，保持原有行为不变。
     */
    @ExceptionHandler(UnauthenticatedException.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.OK)
    InternalResponse<?> handleUnauthenticatedException(HttpServletRequest request, UnauthenticatedException ex) {
        log.error("Handle UnauthenticatedException, uri: {}", request.getRequestURI(), ex);
        return InternalResponse.buildCommonFailResp(ex);
    }

    @ExceptionHandler(Throwable.class)
    @ResponseBody
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    InternalResponse<?> handleException(HttpServletRequest request, Throwable ex) {
        String exceptionInfo = "Handle Exception, uri: " + request.getRequestURI();
        log.error(exceptionInfo, ex);
        return InternalResponse.buildCommonFailResp(ErrorType.INTERNAL, ErrorCode.INTERNAL_ERROR);
    }

}
