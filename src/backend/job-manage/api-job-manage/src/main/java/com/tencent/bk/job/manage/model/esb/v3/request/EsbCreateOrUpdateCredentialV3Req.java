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

package com.tencent.bk.job.manage.model.esb.v3.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.common.esb.model.EsbAppScopeReq;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 新建凭证请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class EsbCreateOrUpdateCredentialV3Req extends EsbAppScopeReq {

    /**
     * 凭证ID
     */
    private String id;

    /**
     * 凭证名称
     */
    private String name;

    /**
     * 凭证类型
     */
    private String type;

    /**
     * 描述
     */
    private String description;

    /**
     * AccessKey
     */
    @JsonProperty("credential_access_key")
    private String credentialAccessKey;

    /**
     * SecretKey
     */
    @JsonProperty("credential_secret_key")
    private String credentialSecretKey;

    /**
     * Username
     */
    @JsonProperty("credential_username")
    private String credentialUsername;

    /**
     * Password
     */
    @JsonProperty("credential_password")
    private String credentialPassword;
}
