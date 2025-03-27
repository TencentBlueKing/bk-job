package com.tencent.bk.job.common.tenant;

import com.tencent.bk.job.common.constant.TenantIdConstants;

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

    @Override
    public String getJobMachineTenantId() {
        // 开启多租户时，Job的机器属于系统租户
        return TenantIdConstants.SYSTEM_TENANT_ID;
    }

    @Override
    public String getTenantIdForGSE() {
        // 开启多租户时，使用系统租户
        return TenantIdConstants.SYSTEM_TENANT_ID;
    }

    @Override
    public String getTenantIdForArtifactoryBkJobProject() {
        // 开启多租户时，使用系统租户
        return TenantIdConstants.SYSTEM_TENANT_ID;
    }
}
