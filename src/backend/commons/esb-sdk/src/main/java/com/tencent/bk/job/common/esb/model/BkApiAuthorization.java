package com.tencent.bk.job.common.esb.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.util.json.SkipLogFields;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 蓝鲸网关认证信息. 说明文档
 * <a>https://github.com/TencentBlueKing/blueking-apigateway/issues/325</a>
 */
@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class BkApiAuthorization {
    /**
     * 应用 ID
     */
    @JsonProperty("bk_app_code")
    private String appCode;

    /**
     * 安全秘钥
     */
    @SkipLogFields
    @JsonProperty("bk_app_secret")
    private String appSecret;

    /**
     * 用户态 access_token，或应用态 access_token
     */
    @SkipLogFields
    @JsonProperty("access_token")
    private String accessToken;

    /**
     * 用户登录态 token，用于认证用户；登录蓝鲸，对应 Cookies 中 bk_token 字段的值；提供 bk_token 时，不需要再提供 bk_username
     */
    @SkipLogFields
    @JsonProperty("bk_token")
    private String bkToken;

    /**
     * 当前用户用户名；仅用于应用免用户认证的场景中，用于指定当前用户
     */
    @JsonProperty("bk_username")
    private String username;

    /**
     * 应用认证
     *
     * @param appCode   应用ID
     * @param appSecret 安全秘钥
     * @return 认证信息
     */
    public static BkApiAuthorization appAuthorization(String appCode, String appSecret) {
        BkApiAuthorization authorization = new BkApiAuthorization();
        authorization.setAppCode(appCode);
        authorization.setAppSecret(appSecret);
        return authorization;
    }

    /**
     * 应用认证 - 免用户认证
     *
     * @param appCode   应用ID
     * @param appSecret 安全秘钥
     * @return 认证信息
     */
    public static BkApiAuthorization appAuthorization(String appCode, String appSecret, String username) {
        BkApiAuthorization authorization = new BkApiAuthorization();
        authorization.setAppCode(appCode);
        authorization.setAppSecret(appSecret);
        authorization.setUsername(username);
        return authorization;
    }

    /**
     * 用户认证 - 根据 token
     *
     * @param bkToken 用户登录 token
     * @return 认证信息
     */
    public static BkApiAuthorization userAuthorization(String appCode, String appSecret, String bkToken) {
        BkApiAuthorization authorization = new BkApiAuthorization();
        authorization.setAppCode(appCode);
        authorization.setAppSecret(appSecret);
        authorization.setBkToken(bkToken);
        return authorization;
    }


}
