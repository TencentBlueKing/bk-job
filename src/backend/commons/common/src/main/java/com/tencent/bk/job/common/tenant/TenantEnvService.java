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

    /**
     * 获取调用GSE接口使用的租户ID
     *
     * @return 租户ID
     */
    String getTenantIdForGSE();

    /**
     * 获取在制品库创建Job项目时使用的租户ID
     *
     * @return 租户ID
     */
    String getTenantIdForArtifactoryBkJobProject();
}
