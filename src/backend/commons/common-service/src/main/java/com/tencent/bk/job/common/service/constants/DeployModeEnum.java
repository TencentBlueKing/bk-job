package com.tencent.bk.job.common.service.constants;

/**
 * Job 部署模式
 */
public enum DeployModeEnum {

    /**
     * 标准模式（微服务）
     */
    STANDARD(Constants.STANDARD),
    /**
     * 轻量化模式(微服务合并）
     */
    LITE(Constants.LITE);

    public static class Constants {
        public static final String STANDARD = "standard";
        public static final String LITE = "lite";
    }


    DeployModeEnum(String mode) {
        this.mode = mode;
    }

    private final String mode;

    public String getValue() {
        return mode;
    }
}
