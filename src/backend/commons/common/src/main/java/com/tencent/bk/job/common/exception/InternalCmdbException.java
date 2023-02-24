package com.tencent.bk.job.common.exception;

import lombok.Getter;
import lombok.ToString;

/**
 * 内部服务异常--调用CMDB异常
 */
@Getter
@ToString
public class InternalCmdbException extends InternalException {

    public InternalCmdbException(Throwable cause, Integer errorCode, Object[] errorParams) {
        super(cause, errorCode, errorParams);
    }

    public InternalCmdbException(String message, Throwable cause, Integer errorCode) {
        super(message, cause, errorCode);
    }

    public InternalCmdbException(String message, Integer errorCode) {
        super(message, errorCode);
    }

    public InternalCmdbException(Integer errorCode, Object[] errorParams) {
        super(errorCode, errorParams);
    }

}
