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

package com.tencent.bk.job.common.k8s.config;

import com.tencent.bk.job.common.discovery.ServiceInfoProvider;
import com.tencent.bk.job.common.k8s.provider.K8SServiceInfoProvider;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1Service;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.kubernetes.client.KubernetesClientAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * K8S 服务实例信息提供器自动装配。
 * <p>
 * 复用 {@link KubernetesClientAutoConfiguration} 暴露的单例 CoreV1Api（背后是单例 ApiClient/OkHttpClient），
 * 避免在高频健康检查场景下反复 new ApiClient 导致 OkHttp 线程池/连接池累积；
 * 同时复用 {@link JobK8sDiscoveryClientAutoConfiguration} 注入的 {@code servicesLister} 本地缓存，
 * 用于按 {@code app.kubernetes.io/name=bk-job} 标签精确筛选作业平台 Service。
 */
@Configuration
@ConditionalOnMissingClass("org.springframework.cloud.consul.discovery.ConsulDiscoveryClient")
@ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
@AutoConfigureAfter({KubernetesClientAutoConfiguration.class, JobK8sDiscoveryClientAutoConfiguration.class})
public class K8sServiceInfoServiceAutoConfiguration {

    /**
     * 装配 {@link K8SServiceInfoProvider}：使用 {@link ObjectProvider} 包装 {@code servicesLister}，
     * 即使该 Bean 在某些条件下不可用（informer 装配链路被关闭等），仍可降级到名称兜底路径，
     * 避免装配失败影响其它健康检查能力。
     */
    @Bean
    public ServiceInfoProvider k8sServiceInfoService(
        DiscoveryClient discoveryClient,
        CoreV1Api coreV1Api,
        @Qualifier("servicesLister") ObjectProvider<Lister<V1Service>> servicesListerProvider
    ) {
        Lister<V1Service> servicesLister = servicesListerProvider.getIfAvailable();
        return new K8SServiceInfoProvider(
            discoveryClient,
            coreV1Api,
            K8SServiceInfoProvider.DEFAULT_POD_CACHE_TTL_MS,
            K8SServiceInfoProvider.DEFAULT_POD_LABEL_SELECTOR,
            servicesLister
        );
    }

}
