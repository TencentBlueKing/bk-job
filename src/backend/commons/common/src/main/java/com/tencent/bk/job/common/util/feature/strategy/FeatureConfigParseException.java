package com.tencent.bk.job.common.util.feature.strategy;

/**
 * 特性开关配置解析异常
 */
public class FeatureConfigParseException extends RuntimeException {
    public FeatureConfigParseException() {
    }

    public FeatureConfigParseException(String message) {
        super(message);
    }

    public FeatureConfigParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public FeatureConfigParseException(Throwable cause) {
        super(cause);
    }
}
