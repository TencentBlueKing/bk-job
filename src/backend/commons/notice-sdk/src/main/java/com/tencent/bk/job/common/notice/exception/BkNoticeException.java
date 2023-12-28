package com.tencent.bk.job.common.notice.exception;

import com.tencent.bk.job.common.exception.InternalException;
import lombok.Getter;
import lombok.ToString;

/**
 * 调用蓝鲸消息通知中心异常
 */
@Getter
@ToString
public class BkNoticeException extends InternalException {

    public BkNoticeException(Throwable cause, Integer errorCode, Object[] errorParams) {
        super(cause, errorCode, errorParams);
    }

    public BkNoticeException(String message, Integer errorCode) {
        super(message, errorCode);
    }

    public BkNoticeException(String message, Throwable cause, Integer errorCode) {
        super(message, cause, errorCode);
    }
}
