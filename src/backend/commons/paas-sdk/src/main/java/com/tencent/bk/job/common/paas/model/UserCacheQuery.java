package com.tencent.bk.job.common.paas.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserCacheQuery {

    /**
     * 租户ID
     */
    private String tenantId;

    /**
     * 用户UUID
     */
    private String username;

}
