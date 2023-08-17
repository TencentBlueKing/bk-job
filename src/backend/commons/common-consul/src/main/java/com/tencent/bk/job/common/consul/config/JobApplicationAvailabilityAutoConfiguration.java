package com.tencent.bk.job.common.consul.config;

import org.springframework.boot.availability.ApplicationAvailabilityBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JobApplicationAvailabilityAutoConfiguration {

    @Bean
    public ApplicationAvailabilityBean applicationAvailability() {
        return new ApplicationAvailabilityBean();
    }

}
