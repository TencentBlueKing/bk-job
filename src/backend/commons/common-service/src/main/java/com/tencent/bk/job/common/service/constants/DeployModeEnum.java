package com.tencent.bk.job.common.service.constants;

/**
 * Job 部署模式
 */
public enum DeployModeEnum {

    /**
     * 微服务模式
     */
    MICRO_SERVICE(Constants.MICRO_SERVICE),
    /**
     * 服务合并模式
     */
    ASSEMBLE(Constants.ASSEMBLE);

    public static class Constants {
        public static final String MICRO_SERVICE = "microservice";
        public static final String ASSEMBLE = "assemble";
    }


    DeployModeEnum(String mode) {
        this.mode = mode;
    }

    private final String mode;

    public String getValue() {
        return mode;
    }
}
