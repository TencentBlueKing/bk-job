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

package com.tencent.bk.job.crontab.config;

import com.tencent.bk.job.common.util.http.JobHttpClientConnectionManagerFactory;
import feign.Client;
import feign.httpclient.ApacheHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.socket.LayeredConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.ssl.SSLContexts;
import org.springframework.cloud.client.loadbalancer.LoadBalancedRetryFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.openfeign.loadbalancer.RetryableFeignBlockingLoadBalancerClient;
import org.springframework.cloud.openfeign.support.FeignHttpClientProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class FeignConfig {

    @Bean
    public HttpClientConnectionManager feignClientConnectionManager(FeignHttpClientProperties httpClientProperties) {
        LayeredConnectionSocketFactory sslSocketFactory = null;
        try {
            sslSocketFactory = new SSLConnectionSocketFactory(
                SSLContexts.custom()
                    .loadTrustMaterial(null, new TrustSelfSignedStrategy())
                    .build()
            );
        } catch (NoSuchAlgorithmException | KeyManagementException | KeyStoreException e) {
            log.error("Fail to create SSLConnectionSocketFactory", e);
        }
        return JobHttpClientConnectionManagerFactory.createWatchableConnectionManager(
            httpClientProperties.getMaxConnectionsPerRoute(),
            httpClientProperties.getMaxConnections(),
            // 服务端Keep-Alive timeout为60s，此处45s+4s(默认10s)<60s即可
            45,
            TimeUnit.SECONDS,
            sslSocketFactory
        );
    }

    public ApacheHttpClient getApacheHttpClient(FeignHttpClientProperties httpClientProperties,
                                                HttpClientConnectionManager feignClientConnectionManager) {
        RequestConfig defaultRequestConfig = RequestConfig.custom()
            .setConnectTimeout(httpClientProperties.getConnectionTimeout())
            .setRedirectsEnabled(httpClientProperties.isFollowRedirects()).build();
        HttpClientBuilder httpClientBuilder = HttpClientBuilder.create()
            .setDefaultRequestConfig(defaultRequestConfig)
            .evictExpiredConnections()
            .evictIdleConnections(4000, TimeUnit.MILLISECONDS)
            .disableAutomaticRetries()
            .disableAuthCaching()
            .disableCookieManagement();
        httpClientBuilder.setRetryHandler(new StandardHttpRequestRetryHandler());
        CloseableHttpClient httpClient;
        httpClientBuilder.setConnectionManager(feignClientConnectionManager);
        httpClient = httpClientBuilder.build();
        return new ApacheHttpClient(httpClient);
    }

    @Bean
    public Client feignClient(FeignHttpClientProperties httpClientProperties,
                              HttpClientConnectionManager feignClientConnectionManager,
                              LoadBalancerClient loadBalancerClient,
                              LoadBalancedRetryFactory loadBalancedRetryFactory,
                              LoadBalancerClientFactory loadBalancerClientFactory) {
        ApacheHttpClient delegate = getApacheHttpClient(httpClientProperties, feignClientConnectionManager);
        return new RetryableFeignBlockingLoadBalancerClient(
            delegate,
            loadBalancerClient,
            loadBalancedRetryFactory,
            loadBalancerClientFactory
        );
    }
}
