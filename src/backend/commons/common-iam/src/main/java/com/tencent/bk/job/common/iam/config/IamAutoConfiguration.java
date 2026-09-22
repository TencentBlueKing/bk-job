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
import com.tencent.bk.job.common.iam.http.IamV4HttpClientServiceImpl;
import com.tencent.bk.job.common.iam.http.RetryableIamV4HttpClientService;
import com.tencent.bk.job.common.retry.RetryExecutor;
import com.tencent.bk.job.common.iam.mock.MockBusinessAuthHelper;
import com.tencent.bk.job.common.iam.mock.MockIamClient;
import com.tencent.bk.job.common.iam.mock.MockPolicyServiceImpl;
import com.tencent.bk.job.common.iam.service.ActionAuthExecutor;
import com.tencent.bk.job.common.iam.service.AppAuthExecutor;
import com.tencent.bk.job.common.iam.service.AppAuthService;
import com.tencent.bk.job.common.iam.service.AuthService;
import com.tencent.bk.job.common.iam.service.BusinessAuthService;
import com.tencent.bk.job.common.iam.service.WebAuthService;
import com.tencent.bk.job.common.iam.service.impl.AppAuthServiceImpl;
import com.tencent.bk.job.common.iam.service.impl.AuthServiceImpl;
import com.tencent.bk.job.common.iam.service.impl.BusinessAuthServiceImpl;
import com.tencent.bk.job.common.iam.service.impl.V3ActionAuthExecutor;
import com.tencent.bk.job.common.iam.service.impl.V3AppAuthExecutor;
import com.tencent.bk.job.common.iam.service.impl.V4ActionAuthExecutor;
import com.tencent.bk.job.common.iam.service.impl.V4AppAuthExecutor;
import com.tencent.bk.job.common.iam.service.impl.WebAuthServiceImpl;
import com.tencent.bk.job.common.iam.util.BusinessAuthHelper;
import com.tencent.bk.job.common.paas.user.IVirtualAdminAccountProvider;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.common.util.http.ExternalSystemEnum;
import com.tencent.bk.job.common.util.http.JobHttpSslVerifyProperties;
import com.tencent.bk.sdk.iam.config.IamConfiguration;
import com.tencent.bk.sdk.iam.helper.AuthHelper;
import com.tencent.bk.sdk.iam.service.HttpClientService;
import com.tencent.bk.sdk.iam.service.PolicyService;
import com.tencent.bk.sdk.iam.service.TokenService;
import com.tencent.bk.sdk.iam.service.TopoPathService;
import com.tencent.bk.sdk.iam.service.impl.PolicyServiceImpl;
import com.tencent.bk.sdk.iam.service.impl.TokenServiceImpl;
import com.tencent.bk.sdk.iamv4.config.IamV4Configuration;
import com.tencent.bk.sdk.iamv4.service.IamV4AuthService;
import com.tencent.bk.sdk.iamv4.service.impl.IamV4AuthServiceImpl;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@Import({IamAspectConfiguration.class, IamInterceptorConfiguration.class})
@EnableConfigurationProperties({JobIamProperties.class, JobIamV4Properties.class})
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
                                               IVirtualAdminAccountProvider virtualAdminAccountProvider,
                                               JobHttpSslVerifyProperties sslVerifyProperties) {
        return new IamHttpClientServiceImpl(
            iamConfiguration,
            virtualAdminAccountProvider,
            sslVerifyProperties.isVerifyEnabled(ExternalSystemEnum.IAM)
        );
    }

    /**
     * IAM v4 鉴权服务：走独立的 APIGW 地址（bkIamV4），使用 {@link IamV4Configuration} 与
     * {@link IamV4HttpClientServiceImpl}。若 IAM 重试开启（存在 iamRetryExecutor bean），
     * 用 {@link RetryableIamV4HttpClientService} 包一层重试。
     */
    @Bean
    public IamV4AuthService iamV4AuthService(AppProperties appProperties,
                                             JobIamV4Properties jobIamV4Properties,
                                             BkApiGatewayProperties bkApiGatewayProperties,
                                             IVirtualAdminAccountProvider virtualAdminAccountProvider,
                                             ObjectProvider<RetryExecutor> iamRetryExecutorProvider) {
        String v4Url = bkApiGatewayProperties.getBkIamV4().getUrl();
        IamV4Configuration iamV4Configuration = new IamV4Configuration(
            jobIamV4Properties.getSystemId(),
            appProperties.getCode(),
            appProperties.getSecret(),
            v4Url
        );
        HttpClientService v4HttpClientService =
            new IamV4HttpClientServiceImpl(iamV4Configuration, virtualAdminAccountProvider);
        RetryExecutor iamRetryExecutor = iamRetryExecutorProvider.getIfAvailable();
        if (iamRetryExecutor != null) {
            v4HttpClientService = new RetryableIamV4HttpClientService(v4HttpClientService, iamRetryExecutor);
        }
        return new IamV4AuthServiceImpl(iamV4Configuration, v4HttpClientService);
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
                                   IIamClient iamClient,
                                   IamV4AuthService iamV4AuthService) {
        ActionAuthExecutor v3Executor = new V3ActionAuthExecutor(authHelper, iamClient);
        ActionAuthExecutor v4Executor = new V4ActionAuthExecutor(iamV4AuthService);
        return new AuthServiceImpl(v3Executor, v4Executor, i18nService);
    }

    @Bean
    public AppAuthService appAuthService(AuthHelper authHelper,
                                         BusinessAuthHelper businessAuthHelper,
                                         PolicyService policyService,
                                         JobIamProperties jobIamProperties,
                                         JobIamV4Properties jobIamV4Properties,
                                         IIamClient iamClient,
                                         IamV4AuthService iamV4AuthService) {
        AppAuthExecutor v3Executor = new V3AppAuthExecutor(authHelper, businessAuthHelper, policyService, iamClient);
        AppAuthExecutor v4Executor = new V4AppAuthExecutor(iamV4AuthService);
        return new AppAuthServiceImpl(v3Executor, v4Executor, jobIamProperties, jobIamV4Properties);
    }


    @Bean
    @ConditionalOnMockIamApiDisabled
    public IIamClient apiGwIamClient(MeterRegistry meterRegistry,
                                     IamConfiguration iamConfiguration,
                                     BkApiGatewayProperties bkApiGatewayProperties,
                                     TenantEnvService tenantEnvService,
                                     IVirtualAdminAccountProvider virtualAdminAccountProvider,
                                     JobHttpSslVerifyProperties sslVerifyProperties) {
        return new ApiGwIamClient(
            meterRegistry,
            new AppProperties(iamConfiguration.getAppCode(), iamConfiguration.getAppSecret()),
            bkApiGatewayProperties,
            tenantEnvService,
            virtualAdminAccountProvider,
            sslVerifyProperties.isVerifyEnabled(ExternalSystemEnum.IAM)
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
