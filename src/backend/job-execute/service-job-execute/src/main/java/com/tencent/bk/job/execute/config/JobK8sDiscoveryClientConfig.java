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

package com.tencent.bk.job.execute.config;


import io.kubernetes.client.informer.SharedInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1Endpoints;
import io.kubernetes.client.openapi.models.V1EndpointsList;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.spring.extended.controller.annotation.GroupVersionResource;
import io.kubernetes.client.spring.extended.controller.annotation.KubernetesInformer;
import io.kubernetes.client.spring.extended.controller.annotation.KubernetesInformers;
import org.springframework.cloud.kubernetes.client.discovery.KubernetesInformerDiscoveryClient;
import org.springframework.cloud.kubernetes.commons.KubernetesNamespaceProvider;
import org.springframework.cloud.kubernetes.commons.discovery.KubernetesDiscoveryProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JobK8sDiscoveryClientConfig {
    @Bean
    public JobCatalogSharedInformerFactory catalogSharedInformerFactory(
        ApiClient apiClient) {
        return new JobCatalogSharedInformerFactory();
    }

    @Bean
    public KubernetesInformerDiscoveryClient kubernetesInformerDiscoveryClient(
        KubernetesNamespaceProvider kubernetesNamespaceProvider,
        JobCatalogSharedInformerFactory sharedInformerFactory, Lister<V1Service> serviceLister,
        Lister<V1Endpoints> endpointsLister, SharedInformer<V1Service> serviceInformer,
        SharedInformer<V1Endpoints> endpointsInformer, KubernetesDiscoveryProperties properties) {
        return new JobKubernetesInformerDiscoveryClient(kubernetesNamespaceProvider.getNamespace(),
            sharedInformerFactory, serviceLister, endpointsLister, serviceInformer, endpointsInformer,
            properties);
    }

    @KubernetesInformers({
        @KubernetesInformer(apiTypeClass = V1Service.class, apiListTypeClass = V1ServiceList.class,
            groupVersionResource = @GroupVersionResource(apiGroup = "", apiVersion = "v1",
                resourcePlural = "services")),
        @KubernetesInformer(apiTypeClass = V1Endpoints.class, apiListTypeClass = V1EndpointsList.class,
            groupVersionResource = @GroupVersionResource(apiGroup = "", apiVersion = "v1",
                resourcePlural = "endpoints"))})
    class JobCatalogSharedInformerFactory extends SharedInformerFactory {

        // TODO: optimization to ease memory pressure from continuous list&watch.

    }
}

