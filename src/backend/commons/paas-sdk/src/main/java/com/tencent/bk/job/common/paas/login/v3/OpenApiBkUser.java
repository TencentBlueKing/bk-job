package com.tencent.bk.job.common.paas.login.v3;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * 用户信息
 */
@Data
public class OpenApiBkUser {

    /**
     * 用户唯一标识，全局唯一
     */
    @JsonProperty("bk_username")
    private String username;

    /**
     * 用户所属租户 ID
     */
    @JsonProperty("tenant_id")
    private String tenantId;

    /**
     * 用户展示名
     */
    @JsonProperty("display_name")
    private String displayName;

    /**
     * 用户语言，枚举值：zh-cn / en
     */
    @JsonProperty("language")
    private String language;

    /**
     * 用户所在时区
     */
    @JsonProperty("time_zone")
    private String timeZone;


}
