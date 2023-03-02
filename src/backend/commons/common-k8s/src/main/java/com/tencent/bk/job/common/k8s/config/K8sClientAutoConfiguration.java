package com.tencent.bk.job.common.k8s.config;

import io.kubernetes.client.openapi.ApiClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Protocol;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.kubernetes.client.KubernetesClientAutoConfiguration;
import org.springframework.cloud.kubernetes.commons.ConditionalOnKubernetesEnabled;
import org.springframework.cloud.kubernetes.commons.KubernetesClientProperties;
import org.springframework.cloud.kubernetes.commons.KubernetesCommonsAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Collections;

import static org.springframework.cloud.kubernetes.client.KubernetesClientUtils.kubernetesApiClient;

@Configuration
@ConditionalOnKubernetesEnabled
@EnableConfigurationProperties(KubernetesClientProperties.class)
@AutoConfigureAfter(KubernetesCommonsAutoConfiguration.class)
@AutoConfigureBefore(KubernetesClientAutoConfiguration.class)
@Slf4j
public class K8sClientAutoConfiguration {
    @Bean
    @Primary
    public ApiClient apiClient(KubernetesClientProperties properties) {
        log.info("Init k8s api client");
        ApiClient apiClient = kubernetesApiClient();
        apiClient.setUserAgent(properties.getUserAgent());
        apiClient.setHttpClient(
            apiClient.getHttpClient().newBuilder().protocols(Collections.singletonList(Protocol.HTTP_1_1)).build());
        return apiClient;
    }
}
