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

package com.tencent.bk.job.common.audit.config;

import com.tencent.bk.audit.AuditRequestProvider;
import com.tencent.bk.audit.config.AuditAutoConfiguration;
import com.tencent.bk.audit.config.AuditProperties;
import com.tencent.bk.job.common.audit.AddResourceScopeAuditPostFilter;
import com.tencent.bk.job.common.audit.JobAuditExceptionResolver;
import com.tencent.bk.job.common.audit.JobAuditRequestProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuditProperties.class)
@ConditionalOnProperty(name = "audit.enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureBefore(AuditAutoConfiguration.class)
@Slf4j
public class JobAuditAutoConfiguration {

    @Bean
    public AuditRequestProvider auditRequestProvider() {
        log.info("Init JobAuditRequestProvider");
        return new JobAuditRequestProvider();
    }


    @Bean
    public JobAuditExceptionResolver auditExceptionResolver() {
        log.info("Init JobAuditExceptionResolver");
        return new JobAuditExceptionResolver();
    }

    @Bean
    public AddResourceScopeAuditPostFilter addResourceScopeAuditPostFilter() {
        return new AddResourceScopeAuditPostFilter();
    }
}
