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

package com.tencent.bk.job.common.consul.config;

import com.tencent.bk.job.common.constant.JobDiscoveryConsts;
import org.springframework.boot.info.BuildProperties;
import org.springframework.cloud.consul.ConditionalOnConsulEnabled;
import org.springframework.cloud.consul.discovery.ConditionalOnConsulDiscoveryEnabled;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistrationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration(proxyBeanMethods = false)
@Import({ConsulServiceInfoServiceConfiguration.class})
@ConditionalOnConsulEnabled
@ConditionalOnConsulDiscoveryEnabled
public class JobConsulAutoConfiguration {
    @Bean
    public ConsulRegistrationCustomizer jobConsulRegistrationCustomizer(BuildProperties buildProperties) {
        return registration -> {
            // 将版本号写入Tag中
            registration.getService().getTags().add(
                JobDiscoveryConsts.TAG_KEY_VERSION + "=" + buildProperties.getVersion());
            // 区分Job后台服务与组件（Redis、MQ等）
            registration.getService().getTags()
                .add(JobDiscoveryConsts.TAG_KEY_TYPE + "=" + JobDiscoveryConsts.TAG_VALUE_TYPE_JOB_BACKEND_SERVICE);
        };
    }
}
