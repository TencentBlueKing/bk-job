package com.tencent.bk.job.common.k8s.config;

import com.tencent.bk.job.common.k8s.availability.JobApplicationAvailabilityBean;
import org.springframework.boot.availability.ApplicationAvailabilityBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class JobApplicationAvailabilityAutoConfiguration {

    @Bean
    public ApplicationAvailabilityBean applicationAvailability() {
        return new JobApplicationAvailabilityBean();
    }

}
