/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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

import com.tencent.bk.job.common.esb.config.AppProperties;
import com.tencent.bk.job.common.esb.config.BkApiGatewayProperties;
import com.tencent.bk.job.common.i18n.service.MessageI18nService;
import com.tencent.bk.job.common.iam.client.ApiGwIamClient;
import com.tencent.bk.job.common.iam.client.IIamClient;
import com.tencent.bk.job.common.iam.http.IamHttpClientServiceImpl;
import com.tencent.bk.job.common.iam.mock.MockBusinessAuthHelper;
import com.tencent.bk.job.common.iam.mock.MockIamClient;
import com.tencent.bk.job.common.iam.mock.MockPolicyServiceImpl;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.service.BusinessAuthService;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.iam.service.impl.AppAuthServiceImpl;
import com.tencent.bk.job.common.iam.service.impl.AuthServiceImpl;
import com.tencent.bk.job.common.iam.service.impl.BusinessAuthServiceImpl;
import com.tencent.bk.job.common.iam.service.impl.WebAuthServiceImpl;
import com.tencent.bk.job.common.iam.util.BusinessAuthHelper;
import com.tencent.bk.job.common.paas.user.IVirtualAdminAccountProvider;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.sdk.iam.config.IamConfiguration;
import com.tencent.bk.sdk.iam.helper.AuthHelper;
import com.tencent.bk.sdk.iam.service.HttpClientService;
import com.tencent.bk.sdk.iam.service.PolicyService;
import com.tencent.bk.sdk.iam.service.TokenService;
import com.tencent.bk.sdk.iam.service.TopoPathService;
import com.tencent.bk.sdk.iam.service.impl.PolicyServiceImpl;
import com.tencent.bk.sdk.iam.service.impl.TokenServiceImpl;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import({IamAspectConfiguration.class, IamInterceptorConfiguration.class})
@EnableConfigurationProperties(JobIamProperties.class)
public class IamAutoConfiguration {

    @Bean
    public IamConfiguration iamConfiguration(AppProperties appProperties,
                                             JobIamProperties jobIamProperties,
                                             BkApiGatewayProperties bkApiGatewayProperties) {
        return new IamConfiguration(
            jobIamProperties.getSystemId(),
            appProperties.getCode(),
            appProperties.getSecret(),
            bkApiGatewayProperties.getBkIam().getUrl()
        );
    }


    @Bean
    public HttpClientService httpClientService(IamConfiguration iamConfiguration,
                                               IVirtualAdminAccountProvider virtualAdminAccountProvider) {
        return new IamHttpClientServiceImpl(iamConfiguration, virtualAdminAccountProvider);
    }

    @Bean
    @ConditionalOnMockIamApiDisabled
    public PolicyService policyService(IamConfiguration iamConfiguration,
                                       HttpClientService httpClientService) {
        return new PolicyServiceImpl(iamConfiguration, httpClientService);
    }

    @Bean
    public TokenService tokenService(IamConfiguration iamConfiguration,
                                     HttpClientService httpClientService) {
        return new TokenServiceImpl(iamConfiguration, httpClientService);
    }

    @Bean
    @ConditionalOnMockIamApiDisabled
    public BusinessAuthHelper businessAuthHelper(IamConfiguration iamConfiguration,
                                                 TokenService tokenService,
                                                 PolicyService policyService,
                                                 @Autowired(required = false) TopoPathService topoPathService) {
        return new BusinessAuthHelper(tokenService, policyService, topoPathService, iamConfiguration);
    }

    @Bean
    public WebAuthService webAuthService(MessageI18nService i18nService, AuthService authService) {
        return new WebAuthServiceImpl(i18nService, authService);
    }

    @Bean
    public BusinessAuthService businessAuthService(AppAuthService appAuthService) {
        return new BusinessAuthServiceImpl(appAuthService);
    }

    @Bean
    public AuthService authService(AuthHelper authHelper,
                                   MessageI18nService i18nService,
                                   IIamClient iamClient) {
        return new AuthServiceImpl(
            authHelper,
            i18nService,
            iamClient
        );
    }

    @Bean
    public AppAuthService appAuthService(AuthHelper authHelper,
                                         BusinessAuthHelper businessAuthHelper,
                                         PolicyService policyService,
                                         JobIamProperties jobIamProperties,
                                         IIamClient iamClient) {
        return new AppAuthServiceImpl(
            authHelper,
            businessAuthHelper,
            policyService,
            jobIamProperties,
            iamClient
        );
    }


    @Bean
    @ConditionalOnMockIamApiDisabled
    public IIamClient apiGwIamClient(MeterRegistry meterRegistry,
                                     IamConfiguration iamConfiguration,
                                     BkApiGatewayProperties bkApiGatewayProperties,
                                     TenantEnvService tenantEnvService,
                                     IVirtualAdminAccountProvider virtualAdminAccountProvider) {
        return new ApiGwIamClient(
            meterRegistry,
            new AppProperties(iamConfiguration.getAppCode(), iamConfiguration.getAppSecret()),
            bkApiGatewayProperties,
            tenantEnvService,
            virtualAdminAccountProvider
        );
    }

    @Bean
    @ConditionalOnMockIamApiEnabled
    public IIamClient mockedIamClient() {
        return new MockIamClient();
    }

    @Bean
    @ConditionalOnMockIamApiEnabled
    public BusinessAuthHelper mockedBusinessAuthHelper(IamConfiguration iamConfiguration,
                                                       TokenService tokenService,
                                                       PolicyService policyService,
                                                       @Autowired(required = false) TopoPathService topoPathService) {
        return new MockBusinessAuthHelper(tokenService, policyService, topoPathService, iamConfiguration);
    }

    @Bean
    @ConditionalOnMockIamApiEnabled
    public PolicyService mockedPolicyService() {
        return new MockPolicyServiceImpl();
    }
}
