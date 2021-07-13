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

package com.tencent.bk.job.upgrader.iam;

import com.tencent.bk.job.common.iam.config.EsbConfiguration;
import com.tencent.bk.job.common.iam.util.BusinessAuthHelper;
import com.tencent.bk.sdk.iam.config.IamConfiguration;
import com.tencent.bk.sdk.iam.constants.SystemId;
import com.tencent.bk.sdk.iam.helper.AuthHelper;
import com.tencent.bk.sdk.iam.service.HttpClientService;
import com.tencent.bk.sdk.iam.service.PolicyService;
import com.tencent.bk.sdk.iam.service.TokenService;
import com.tencent.bk.sdk.iam.service.impl.DefaultHttpClientServiceImpl;
import com.tencent.bk.sdk.iam.service.impl.PolicyServiceImpl;
import com.tencent.bk.sdk.iam.service.impl.TokenServiceImpl;

public class JobIamHelper {

    /**
     * ESB 分配的系统 App Code
     */
    private String appCode;

    /**
     * ESB 分配的系统 App Secret
     */
    private String appSecret;

    /**
     * 权限中心的访问地址
     */
    private String iamBaseUrl;

    /**
     * ESB API 的访问地址
     */
    private String esbUrl;

    public JobIamHelper(String appCode, String appSecret, String iamBaseUrl, String esbUrl) {
        this.appCode = appCode;
        this.appSecret = appSecret;
        this.iamBaseUrl = iamBaseUrl;
        this.esbUrl = esbUrl;
    }

    public IamConfiguration iamConfiguration() {
        return new IamConfiguration(SystemId.JOB, appCode, appSecret, iamBaseUrl);
    }

    public EsbConfiguration esbConfiguration() {
        return new EsbConfiguration(esbUrl, false);
    }

    public HttpClientService httpClientService() {
        return new DefaultHttpClientServiceImpl(iamConfiguration());
    }

    public PolicyService policyService() {
        return new PolicyServiceImpl(iamConfiguration(), httpClientService());
    }

    public TokenService tokenService() {
        return new TokenServiceImpl(iamConfiguration(), httpClientService());
    }

    public AuthHelper authHelper() {
        return new AuthHelper(tokenService(), policyService(), iamConfiguration());
    }

    public BusinessAuthHelper businessAuthHelper() {
        return new BusinessAuthHelper(tokenService(), policyService(), iamConfiguration());
    }
}
