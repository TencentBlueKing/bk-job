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

package com.tencent.bk.job.common.service.config;

import com.tencent.bk.job.common.VersionInfoLogApplicationRunner;
import com.tencent.bk.job.common.config.BkConfig;
import com.tencent.bk.job.common.util.ApplicationContextRegister;
import com.tencent.bk.job.common.util.http.HttpHelperFactory;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

@Slf4j
@Configuration(proxyBeanMethods = false)
@Import({JobCommonConfig.class, BkConfig.class})
public class JobCommonAutoConfiguration {
    @Bean("applicationContextRegister")
    @Lazy(false)
    public ApplicationContextRegister applicationContextRegister() {
        return new ApplicationContextRegister();
    }

    @Bean
    HttpConfigSetter httpConfigSetter(@Autowired MeterRegistry meterRegistry) {
        HttpHelperFactory.setMeterRegistry(meterRegistry);
        log.info("meterRegistry for HttpHelperFactory init");
        return new HttpConfigSetter();
    }

    static class HttpConfigSetter {
        HttpConfigSetter() {
        }
    }

    @Value("${spring.application.name:bk-job}")
    private String serviceName;

    @Bean
    public VersionInfoLogApplicationRunner versionInfoLogApplicationRunner(BuildProperties buildProperties) {
        return new VersionInfoLogApplicationRunner(serviceName, buildProperties);
    }
}
