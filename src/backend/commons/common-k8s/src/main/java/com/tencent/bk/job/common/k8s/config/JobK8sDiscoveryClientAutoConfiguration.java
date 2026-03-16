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


import io.kubernetes.client.informer.SharedInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1Endpoints;
import io.kubernetes.client.openapi.models.V1Service;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.CommonsClientAutoConfiguration;
import org.springframework.cloud.client.ConditionalOnDiscoveryEnabled;
import org.springframework.cloud.client.ConditionalOnDiscoveryHealthIndicatorEnabled;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration;
import org.springframework.cloud.kubernetes.client.KubernetesClientAutoConfiguration;
import org.springframework.cloud.kubernetes.client.discovery.KubernetesInformerDiscoveryClient;
import org.springframework.cloud.kubernetes.commons.KubernetesNamespaceProvider;
import org.springframework.cloud.kubernetes.commons.PodUtils;
import org.springframework.cloud.kubernetes.commons.discovery.KubernetesDiscoveryClientHealthIndicatorInitializer;
import org.springframework.cloud.kubernetes.commons.discovery.KubernetesDiscoveryProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 负载均衡相关的自定义Bean配置
 * 注意：该类用于使用部分自定义Bean实例覆盖KubernetesDiscoveryClientAutoConfiguration类的配置内容，框架升级时需要同步更新！！！
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnCloudPlatform(CloudPlatform.KUBERNETES)
@ConditionalOnDiscoveryEnabled
@AutoConfigureBefore({SimpleDiscoveryClientAutoConfiguration.class, CommonsClientAutoConfiguration.class})
@AutoConfigureAfter({KubernetesClientAutoConfiguration.class})
@EnableConfigurationProperties(KubernetesDiscoveryProperties.class)
public class JobK8sDiscoveryClientAutoConfiguration {

    @ConditionalOnClass({HealthIndicator.class})
    @ConditionalOnDiscoveryEnabled
    @ConditionalOnDiscoveryHealthIndicatorEnabled
    @Configuration
    public static class KubernetesDiscoveryClientHealthIndicatorConfiguration {

        @Bean
        public KubernetesDiscoveryClientHealthIndicatorInitializer indicatorInitializer(
            ApplicationEventPublisher applicationEventPublisher, PodUtils podUtils) {
            return new KubernetesDiscoveryClientHealthIndicatorInitializer(podUtils, applicationEventPublisher);
        }

    }

    @Bean
    @ConditionalOnMissingBean
    public JobSpringCloudKubernetesInformerFactoryProcessor discoveryInformerConfigurer(
        KubernetesNamespaceProvider kubernetesNamespaceProvider,
        ApiClient apiClient,
        JobCatalogSharedInformerFactory sharedInformerFactory,
        Environment environment) {
        // Injecting KubernetesDiscoveryProperties here would cause it to be
        // initialize too early
        // Instead get the all-namespaces property value from the Environment directly
        boolean allNamespaces = environment.getProperty("spring.cloud.kubernetes.discovery.all-namespaces",
            Boolean.class, false);
        return new JobSpringCloudKubernetesInformerFactoryProcessor(
            kubernetesNamespaceProvider,
            apiClient,
            sharedInformerFactory,
            allNamespaces
        );
    }

    @Bean
    public JobCatalogSharedInformerFactory jobCatalogSharedInformerFactory(ApiClient apiClient) {
        return new JobCatalogSharedInformerFactory();
    }

    @Bean
    public KubernetesInformerDiscoveryClient kubernetesInformerDiscoveryClient(
        KubernetesNamespaceProvider kubernetesNamespaceProvider,
        JobCatalogSharedInformerFactory jobCatalogSharedInformerFactory,
        Lister<V1Service> serviceLister,
        Lister<V1Endpoints> endpointsLister,
        SharedInformer<V1Service> serviceInformer,
        SharedInformer<V1Endpoints> endpointsInformer,
        KubernetesDiscoveryProperties properties
    ) {
        return new JobKubernetesInformerDiscoveryClient(
            kubernetesNamespaceProvider.getNamespace(),
            jobCatalogSharedInformerFactory,
            serviceLister,
            endpointsLister,
            serviceInformer,
            endpointsInformer,
            properties
        );
    }

    class JobCatalogSharedInformerFactory extends SharedInformerFactory {
    }
}

