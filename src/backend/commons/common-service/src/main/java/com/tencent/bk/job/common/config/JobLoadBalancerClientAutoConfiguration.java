package com.tencent.bk.job.common.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.AsyncLoadBalancerAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties;
import org.springframework.cloud.client.loadbalancer.LoadBalancerUriTools;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients;
import org.springframework.cloud.loadbalancer.blocking.client.BlockingLoadBalancerClient;
import org.springframework.cloud.loadbalancer.config.BlockingLoadBalancerClientAutoConfiguration;
import org.springframework.cloud.loadbalancer.config.LoadBalancerAutoConfiguration;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Map;

/**
 * Job 自定义请求负载均衡客户端，用于解决Open-Feign调用ipv6的报错
 */
@Configuration(proxyBeanMethods = false)
@LoadBalancerClients
@AutoConfigureAfter(LoadBalancerAutoConfiguration.class)
@AutoConfigureBefore({org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration.class,
    AsyncLoadBalancerAutoConfiguration.class, BlockingLoadBalancerClientAutoConfiguration.class})
@ConditionalOnClass(RestTemplate.class)
@Slf4j
public class JobLoadBalancerClientAutoConfiguration {

    @Bean
    @ConditionalOnBean(LoadBalancerClientFactory.class)
    @ConditionalOnMissingBean
    @Primary
    public LoadBalancerClient blockingLoadBalancerClient(LoadBalancerClientFactory loadBalancerClientFactory,
                                                         LoadBalancerProperties properties) {
        log.info("Init JobLoadBalancerClient.");
        return new JobLoadBalancerClient(loadBalancerClientFactory, properties);
    }

    private static class JobLoadBalancerClient extends BlockingLoadBalancerClient {
        public JobLoadBalancerClient(LoadBalancerClientFactory loadBalancerClientFactory,
                                     LoadBalancerProperties properties) {
            super(loadBalancerClientFactory, properties);
        }

        @Override
        public URI reconstructURI(ServiceInstance serviceInstance, URI original) {
            return LoadBalancerUriTools.reconstructURI(
                new Ipv6CapableDelegatingServiceInstance(serviceInstance),
                original
            );
        }
    }

    private static class Ipv6CapableDelegatingServiceInstance implements ServiceInstance {

        final ServiceInstance delegate;

        public Ipv6CapableDelegatingServiceInstance(ServiceInstance delegate) {
            this.delegate = delegate;
        }

        @Override
        public String getServiceId() {
            return delegate.getServiceId();
        }

        @Override
        public String getHost() {
            return getAvailableIpv6Host(delegate.getHost());
        }

        private String getAvailableIpv6Host(String host) {
            if (StringUtils.isNotBlank(host) && host.contains(":") && !host.startsWith("[")) {
                return "[" + host + "]";
            }
            return host;
        }

        @Override
        public int getPort() {
            return delegate.getPort();
        }

        @Override
        public boolean isSecure() {
            return delegate.isSecure();
        }

        @Override
        public URI getUri() {
            return delegate.getUri();
        }

        @Override
        public Map<String, String> getMetadata() {
            return delegate.getMetadata();
        }

        @Override
        public String getScheme() {
            return delegate.getScheme();
        }
    }
}
