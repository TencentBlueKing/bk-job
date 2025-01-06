package com.tencent.bk.job.common.paas.model;

/**
 * 租户状态
 */
public enum TenantStatusEnum {

    /**
     * 启用
     */
    ENABLED(Constants.ENABLED),
    /**
     * 禁用
     */
    DISABLED(Constants.DISABLED);

    TenantStatusEnum(String status) {
        this.status = status;
    }

    public interface Constants {
        String ENABLED = "enabled";
        String DISABLED = "disabled";
    }

    private final String status;

    public static TenantStatusEnum valOf(String status) {
        for (TenantStatusEnum value : values()) {
            if (value.status.equalsIgnoreCase(status)) {
                return value;
            }
        }
        throw new IllegalArgumentException("No TenantStatusEnum constant: " + status);
    }

    public String getStatus() {
        return status;
    }
}
