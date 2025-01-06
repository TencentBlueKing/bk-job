package com.tencent.bk.job.common.tenant;

import com.tencent.bk.job.common.constant.TenantIdConstants;

/**
 * 租户环境信息 Service
 */
public class TenantEnvService {

    private final TenantProperties tenantProperties;

    public TenantEnvService(TenantProperties tenantProperties) {
        this.tenantProperties = tenantProperties;
    }


    /**
     * 该环境是否支持多租户
     */
    public boolean isTenantEnabled() {
        return tenantProperties.isEnabled();
    }

    /**
     * 获取默认的租户 ID
     */
    public String getDefaultTenantId() {
        return isTenantEnabled() ? TenantIdConstants.MULTI_TENANT_ENV_DEFAULT_TENANT_ID :
            TenantIdConstants.SINGLE_TENANT_ENV_DEFAULT_TENANT_ID;
    }
}
