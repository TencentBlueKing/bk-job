package com.tencent.bk.job.common.constant;

/**
 * 租户 ID 常量
 */
public interface TenantIdConstants {

    /**
     * 非多租户环境默认租户 ID
     */
    String NON_TENANT_ENV_DEFAULT_TENANT_ID = "default";

    /**
     * 多租户环境运营租户 ID
     */
    String TENANT_ENV_SYSTEM_TENANT_ID = "system";
}
