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

package com.tencent.bk.job.common.iam.config;

import com.tencent.bk.job.common.iam.http.IamHttpClientServiceImpl;
import com.tencent.bk.job.common.iam.util.BusinessAuthHelper;
import com.tencent.bk.sdk.iam.config.IamConfiguration;
import com.tencent.bk.sdk.iam.helper.AuthHelper;
import com.tencent.bk.sdk.iam.service.HttpClientService;
import com.tencent.bk.sdk.iam.service.PolicyService;
import com.tencent.bk.sdk.iam.service.TokenService;
import com.tencent.bk.sdk.iam.service.impl.PolicyServiceImpl;
import com.tencent.bk.sdk.iam.service.impl.TokenServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * @since 28/5/2020 17:42
 */
@Configuration
public class JobIamAutoConfiguration {

    /**
     * 权限中心对应的系统 ID
     */
    @Value("${iam.system-id:}")
    private String systemId;

    /**
     * ESB 分配的系统 App Code
     */
    @Value("${app.code:}")
    private String appCode;

    /**
     * ESB 分配的系统 App Secret
     */
    @Value("${app.secret:}")
    private String appSecret;

    /**
     * 权限中心的访问地址
     */
    @Value("${iam.base-url:}")
    private String iamBaseUrl;

    /**
     * ESB API 的访问地址
     */
    @Value("${esb.service.url:}")
    private String esbUrl;

    @Value("${esb.use.test.env:false}")
    private boolean useEsbTestEnv;

    @Bean
    public IamConfiguration iamConfiguration() {
        return new IamConfiguration(systemId, appCode, appSecret, iamBaseUrl);
    }

    @Bean
    public EsbConfiguration esbConfiguration() {
        return new EsbConfiguration(esbUrl, useEsbTestEnv);
    }

    @Bean
    @DependsOn("httpConfigSetter")
    public HttpClientService httpClientService() {
        return new IamHttpClientServiceImpl(iamConfiguration());
    }

    @Bean
    public PolicyService policyService() {
        return new PolicyServiceImpl(iamConfiguration(), httpClientService());
    }

    @Bean
    public TokenService tokenService() {
        return new TokenServiceImpl(iamConfiguration(), httpClientService());
    }

    @Bean
    public AuthHelper authHelper() {
        return new AuthHelper(tokenService(), policyService(), iamConfiguration());
    }

    @Bean
    public BusinessAuthHelper businessAuthHelper() {
        return new BusinessAuthHelper(tokenService(), policyService(), iamConfiguration());
    }
}
