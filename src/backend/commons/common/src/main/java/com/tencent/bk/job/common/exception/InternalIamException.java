package com.tencent.bk.job.common.exception;

import lombok.Getter;
import lombok.ToString;

/**
 * 内部服务异常--调用IAM异常
 */
@Getter
@ToString
public class InternalIamException extends InternalException {

    public InternalIamException(Throwable cause, Integer errorCode, Object[] errorParams) {
        super(cause, errorCode, errorParams);
    }

    public InternalIamException(String message, Integer errorCode) {
        super(message, errorCode);
    }

    public InternalIamException(String message, Throwable cause, Integer errorCode) {
        super(message, cause, errorCode);
    }
}
