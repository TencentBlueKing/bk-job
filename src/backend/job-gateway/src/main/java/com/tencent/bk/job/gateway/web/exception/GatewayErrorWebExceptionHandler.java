package com.tencent.bk.job.gateway.web.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.DefaultErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * gateway全局异常处理器,
 * 针对非业务逻辑导致的边界异常（如PrematureCloseException），进行日志降级处理。
 * 边界异常：通常是客户端、网络、K8s环境问题，导致连接在响应生成之前就被关闭的异常。
 */
@Component
@Order(-1)
@Slf4j
public class GatewayErrorWebExceptionHandler extends DefaultErrorWebExceptionHandler {

    /**
     * 边界异常消息列表，匹配到这些消息的异常会降级日志
     */
    private static final Set<String> BOUNDARY_EXCEPTION_MESSAGES = Collections.unmodifiableSet(
        new HashSet<>(Arrays.asList(
            "Connection prematurely closed BEFORE response",
            "Connection has been closed BEFORE response"
        ))
    );

    public GatewayErrorWebExceptionHandler(ErrorAttributes errorAttributes,
                                           WebProperties webProperties,
                                           ServerProperties serverProperties,
                                           ApplicationContext applicationContext,
                                           ServerCodecConfigurer serverCodecConfigurer
    ) {
        super(errorAttributes,
            webProperties.getResources(),
            serverProperties.getError(),
            applicationContext
        );
        super.setMessageWriters(serverCodecConfigurer.getWriters());
        super.setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    protected void logError(ServerRequest request,
                            ServerResponse response,
                            Throwable throwable
    ) {
        // 边界异常日志降级warn
        if (isBoundaryException(throwable)) {
            log.warn(
                "Boundary exception detected. path={}, method={}, exceptionType={}, throwableMessage={}",
                request.path(),
                request.methodName(),
                throwable.getClass().getSimpleName(),
                throwable.getMessage()
            );
            return;
        }
        super.logError(request, response, throwable);
    }

    /**
     * 判断是否属于可接受的边界异常
     */
    private boolean isBoundaryException(Throwable throwable) {
        if (throwable == null || throwable.getMessage() == null) {
            return false;
        }
        return BOUNDARY_EXCEPTION_MESSAGES.stream().anyMatch(throwable.getMessage()::contains);
    }
}
