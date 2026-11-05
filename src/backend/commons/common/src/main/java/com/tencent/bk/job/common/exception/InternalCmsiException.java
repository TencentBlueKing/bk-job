package com.tencent.bk.job.common.exception;

import lombok.Getter;

/**
 * 内部服务异常--调用cmsi接口异常
 */
@Getter
public class InternalCmsiException extends InternalException {

    public InternalCmsiException(String message, Throwable cause, Integer errorCode) {
        super(message, cause, errorCode);
    }
}
