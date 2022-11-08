package com.tencent.bk.job.common.util.json;

/**
 * Json 解析异常
 */
public class JsonParseException extends RuntimeException {

    public JsonParseException(Throwable cause) {
        super(cause);
    }

    public JsonParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
