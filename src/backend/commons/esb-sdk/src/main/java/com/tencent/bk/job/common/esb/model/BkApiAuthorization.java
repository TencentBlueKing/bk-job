/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-JOB蓝鲸智云作业平台 is licensed under the MIT License.
 *
 * License for BK-JOB蓝鲸智云作业平台:
 * --------------------------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF
 * CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

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
