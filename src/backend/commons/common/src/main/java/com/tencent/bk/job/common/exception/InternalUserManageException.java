package com.tencent.bk.job.common.exception;

import lombok.Getter;
import lombok.ToString;

/**
 * 内部服务异常--用户管理异常
 */
@Getter
@ToString
public class InternalUserManageException extends InternalException {

    public InternalUserManageException(String message, Throwable cause, Integer errorCode) {
        super(message, cause, errorCode);
    }
}
