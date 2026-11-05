package com.tencent.bk.job.common.exception;

import lombok.Getter;

/**
 * 内部服务异常--配置异常
 */
@Getter
public class IncorrectConfigException extends InternalException {

    public IncorrectConfigException(String message, Integer errorCode, Object[] errorParams) {
        super(message, errorCode, errorParams);
    }
}
