package com.tencent.bk.job.common.esb.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.util.json.SkipLogFields;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 蓝鲸 Gateway 认证信息
 */
@Data
@NoArgsConstructor
public class BkApiAuthorization {
    @JsonProperty("bk_app_code")
    private String appCode;

    @SkipLogFields("bk_app_secret")
    @JsonProperty("bk_app_secret")
    private String appSecret;

    @JsonProperty("access_token")
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private String accessToken;

    public BkApiAuthorization(String appCode, String appSecret) {
        this.appCode = appCode;
        this.appSecret = appSecret;
    }

    public BkApiAuthorization(String appCode, String appSecret, String accessToken) {
        this.appCode = appCode;
        this.appSecret = appSecret;
        this.accessToken = accessToken;
    }
}
