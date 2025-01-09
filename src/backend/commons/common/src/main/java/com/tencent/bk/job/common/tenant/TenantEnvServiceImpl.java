package com.tencent.bk.job.common.tenant;

public class TenantEnvServiceImpl implements TenantEnvService {
    private final TenantProperties tenantProperties;

    public TenantEnvServiceImpl(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }


    /**
     * 该环境是否支持多租户
     */
    public boolean isTenantEnabled() {
        return tenantProperties.isEnabled();
    }
}
