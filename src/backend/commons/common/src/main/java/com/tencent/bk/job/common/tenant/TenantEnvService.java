package com.tencent.bk.job.common.tenant;

/**
 * 租户环境信息 Service
 */
public interface TenantEnvService {
    /**
     * 该环境是否支持多租户
     */
    boolean isTenantEnabled();

    /**
     * 获取Job机器所归属的租户ID
     *
     * @return 租户ID
     */
    String getJobMachineTenantId();
}
