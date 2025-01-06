package com.tencent.bk.job.common.paas.model;

import lombok.Data;

/**
 * 租户信息
 */
@Data
public class OpenApiTenant {

    /**
     * 租户 ID
     */
    private String id;

    /**
     * 租户名
     */
    private String name;

    /**
     * 租户状态，enabled 表示启用，disabled 表示禁用
     *
     * @see TenantStatusEnum
     */
    private String status;
}
