package com.tencent.bk.job.common.consul.config;

import com.tencent.bk.job.common.consul.provider.ConsulServiceInfoProvider;
import com.tencent.bk.job.common.discovery.ServiceInfoProvider;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.consul.discovery.ConditionalOnConsulDiscoveryEnabled;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
@ConditionalOnConsulDiscoveryEnabled
public class ConsulServiceInfoServiceAutoConfig {

    @Bean
    @Primary
    public ServiceInfoProvider consulServiceInfoService(DiscoveryClient discoveryClient) {
        return new ConsulServiceInfoProvider(discoveryClient);
    }

}
