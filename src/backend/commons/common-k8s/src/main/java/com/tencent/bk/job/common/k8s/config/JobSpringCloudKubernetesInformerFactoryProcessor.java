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

package com.tencent.bk.job.common.k8s.config;

import io.kubernetes.client.informer.SharedIndexInformer;
import io.kubernetes.client.informer.SharedInformer;
import io.kubernetes.client.informer.SharedInformerFactory;
import io.kubernetes.client.informer.cache.Lister;
import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.spring.extended.controller.KubernetesInformerFactoryProcessor;
import io.kubernetes.client.spring.extended.controller.annotation.KubernetesInformer;
import io.kubernetes.client.spring.extended.controller.annotation.KubernetesInformers;
import io.kubernetes.client.util.Namespaces;
import io.kubernetes.client.util.generic.GenericKubernetesApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.cloud.kubernetes.commons.KubernetesNamespaceProvider;
import org.springframework.core.ResolvableType;

import java.time.Duration;

/**
 * SpringCloudKubernetesInformerFactoryProcessor为非公开类，此处将其设置为公开类便于覆盖Bean时使用
 */
public class JobSpringCloudKubernetesInformerFactoryProcessor extends KubernetesInformerFactoryProcessor {

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
        super();
        this.apiClient = apiClient;
        this.sharedInformerFactory = sharedInformerFactory;
        this.kubernetesNamespaceProvider = kubernetesNamespaceProvider;
        this.allNamespaces = allNamespaces;
    }

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		String namespace = allNamespaces ? Namespaces.NAMESPACE_ALL : kubernetesNamespaceProvider.getNamespace() == null
				? Namespaces.NAMESPACE_DEFAULT : kubernetesNamespaceProvider.getNamespace();

		this.apiClient.setHttpClient(this.apiClient.getHttpClient().newBuilder().readTimeout(Duration.ZERO).build());

		KubernetesInformers kubernetesInformers = sharedInformerFactory.getClass()
				.getAnnotation(KubernetesInformers.class);
		if (kubernetesInformers == null || kubernetesInformers.value().length == 0) {
			log.info("No informers registered in the sharedInformerFactory..");
			return;
		}
		for (KubernetesInformer kubernetesInformer : kubernetesInformers.value()) {
			final GenericKubernetesApi api = new GenericKubernetesApi(kubernetesInformer.apiTypeClass(),
					kubernetesInformer.apiListTypeClass(), kubernetesInformer.groupVersionResource().apiGroup(),
					kubernetesInformer.groupVersionResource().apiVersion(),
					kubernetesInformer.groupVersionResource().resourcePlural(), apiClient);
			SharedIndexInformer sharedIndexInformer = sharedInformerFactory.sharedIndexInformerFor(api,
					kubernetesInformer.apiTypeClass(), kubernetesInformer.resyncPeriodMillis(),
					kubernetesInformer.namespace().equals(Namespaces.NAMESPACE_ALL) ? namespace
							: kubernetesInformer.namespace());
			ResolvableType informerType = ResolvableType.forClassWithGenerics(SharedInformer.class,
					kubernetesInformer.apiTypeClass());
			RootBeanDefinition informerBean = new RootBeanDefinition();
			informerBean.setTargetType(informerType);
			informerBean.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
			informerBean.setAutowireCandidate(true);
			String informerBeanName = informerType.toString();
			this.beanDefinitionRegistry.registerBeanDefinition(informerBeanName, informerBean);
			beanFactory.registerSingleton(informerBeanName, sharedIndexInformer);

			Lister lister = new Lister(sharedIndexInformer.getIndexer());
			ResolvableType listerType = ResolvableType.forClassWithGenerics(Lister.class,
					kubernetesInformer.apiTypeClass());
			RootBeanDefinition listerBean = new RootBeanDefinition();
			listerBean.setTargetType(listerType);
			listerBean.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
			listerBean.setAutowireCandidate(true);
			String listerBeanName = listerType.toString();
			this.beanDefinitionRegistry.registerBeanDefinition(listerBeanName, listerBean);
			beanFactory.registerSingleton(listerBeanName, lister);
		}
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		this.beanDefinitionRegistry = registry;
	}

}
