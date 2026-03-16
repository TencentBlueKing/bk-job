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

import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.models.V1Endpoints;
import io.kubernetes.client.openapi.models.V1EndpointsList;
import io.kubernetes.client.openapi.models.V1Service;
import io.kubernetes.client.openapi.models.V1ServiceList;
import io.kubernetes.client.util.Namespaces;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.cloud.kubernetes.commons.KubernetesNamespaceProvider;
import org.springframework.core.ResolvableType;

import java.time.Duration;

/**
 * 编程式 Kubernetes Informer 工厂处理器。
 * Kubernetes Java Client 19.x 移除了注解式 Informer 配置
 * ({@code @KubernetesInformers}/{@code @KubernetesInformer}/{@code @GroupVersionResource})
 * 和 {@code KubernetesInformerFactoryProcessor}，改为编程式注册。
 */
public class JobSpringCloudKubernetesInformerFactoryProcessor implements BeanDefinitionRegistryPostProcessor {

    private static final Logger log = LoggerFactory.getLogger(JobSpringCloudKubernetesInformerFactoryProcessor.class);

    private BeanDefinitionRegistry beanDefinitionRegistry;

    private final ApiClient apiClient;

    private final SharedInformerFactory sharedInformerFactory;

    private final boolean allNamespaces;

    private final KubernetesNamespaceProvider kubernetesNamespaceProvider;

    JobSpringCloudKubernetesInformerFactoryProcessor(KubernetesNamespaceProvider kubernetesNamespaceProvider,
                                                     ApiClient apiClient,
                                                     SharedInformerFactory sharedInformerFactory,
                                                     boolean allNamespaces) {
        this.apiClient = apiClient;
        this.sharedInformerFactory = sharedInformerFactory;
        this.kubernetesNamespaceProvider = kubernetesNamespaceProvider;
        this.allNamespaces = allNamespaces;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        String namespace = allNamespaces ? Namespaces.NAMESPACE_ALL
            : kubernetesNamespaceProvider.getNamespace() == null
            ? Namespaces.NAMESPACE_DEFAULT : kubernetesNamespaceProvider.getNamespace();

        this.apiClient.setHttpClient(this.apiClient.getHttpClient().newBuilder().readTimeout(Duration.ZERO).build());

        registerInformer(beanFactory, V1Service.class, V1ServiceList.class,
            "", "v1", "services", 0L, namespace);
        registerInformer(beanFactory, V1Endpoints.class, V1EndpointsList.class,
            "", "v1", "endpoints", 0L, namespace);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerInformer(
        ConfigurableListableBeanFactory beanFactory,
        Class apiTypeClass,
        Class apiListTypeClass,
        String apiGroup,
        String apiVersion,
        String resourcePlural,
        long resyncPeriodMillis,
        String namespace) {

        final GenericKubernetesApi api = new GenericKubernetesApi(
            apiTypeClass, apiListTypeClass, apiGroup, apiVersion, resourcePlural, apiClient);
        SharedIndexInformer sharedIndexInformer = sharedInformerFactory.sharedIndexInformerFor(
            api, apiTypeClass, resyncPeriodMillis, namespace);

        ResolvableType informerType = ResolvableType.forClassWithGenerics(SharedInformer.class, apiTypeClass);
        RootBeanDefinition informerBean = new RootBeanDefinition();
        informerBean.setTargetType(informerType);
        informerBean.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        informerBean.setAutowireCandidate(true);
        String informerBeanName = informerType.toString();
        this.beanDefinitionRegistry.registerBeanDefinition(informerBeanName, informerBean);
        beanFactory.registerSingleton(informerBeanName, sharedIndexInformer);

        Lister lister = new Lister(sharedIndexInformer.getIndexer());
        ResolvableType listerType = ResolvableType.forClassWithGenerics(Lister.class, apiTypeClass);
        RootBeanDefinition listerBean = new RootBeanDefinition();
        listerBean.setTargetType(listerType);
        listerBean.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
        listerBean.setAutowireCandidate(true);
        String listerBeanName = listerType.toString();
        this.beanDefinitionRegistry.registerBeanDefinition(listerBeanName, listerBean);
        beanFactory.registerSingleton(listerBeanName, lister);
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
        this.beanDefinitionRegistry = registry;
    }

}
