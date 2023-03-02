package com.tencent.bk.job.common.k8s.config;

import com.tencent.bk.job.common.discovery.ServiceInfoProvider;
import com.tencent.bk.job.common.k8s.provider.K8SServiceInfoProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingClass;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.kubernetes.commons.ConditionalOnKubernetesEnabled;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnMissingClass("org.springframework.cloud.consul.discovery.ConsulDiscoveryClient")
@ConditionalOnKubernetesEnabled
public class K8sServiceInfoServiceAutoConfig {

    @Bean
    public ServiceInfoProvider k8sServiceInfoService(DiscoveryClient discoveryClient) {
        return new K8SServiceInfoProvider(discoveryClient);
    }

}
