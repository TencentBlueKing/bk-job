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

package com.tencent.bk.job.execute.config;

import com.tencent.bk.job.execute.auth.impl.CachedTopoPathServiceImpl;
import com.tencent.bk.job.execute.auth.impl.CompositeTopoPathService;
import com.tencent.bk.job.execute.auth.impl.HostTopoPathCache;
import com.tencent.bk.job.execute.auth.impl.SwitchableTopoPathService;
import com.tencent.bk.sdk.iam.service.TopoPathService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
@Configuration(value = "jobExecuteIamConfiguration")
@EnableConfigurationProperties(IamHostTopoPathProperties.class)
public class IamConfiguration {

    @Bean
    public TopoPathService topoPathService(@Qualifier("cmdbTopoPathService")
                                           TopoPathService cmdbTopoPathService,
                                           @Qualifier("localDBTopoPathService")
                                           TopoPathService localDBTopoPathService) {
        CompositeTopoPathService compositeTopoPathService = new CompositeTopoPathService();
        compositeTopoPathService.addTopoPathService(cmdbTopoPathService);
        compositeTopoPathService.addTopoPathService(localDBTopoPathService);
        return compositeTopoPathService;
    }

    @ConditionalOnIamHostTopoPathCacheEnabled
    @Configuration
    public static class HostTopoPathCacheConfiguration {

        @Bean
        public HostTopoPathCache hostTopoPathCache(@Qualifier("jsonRedisTemplate")
                                                   RedisTemplate<String, Object> redisTemplate,
                                                   IamHostTopoPathProperties iamHostTopoPathProperties,
                                                   MeterRegistry meterRegistry) {
            return new HostTopoPathCache(redisTemplate, iamHostTopoPathProperties, meterRegistry);
        }

        @Bean
        public TopoPathService cachedTopoPathService(@Qualifier("topoPathService")
                                                     TopoPathService topoPathService,
                                                     HostTopoPathCache hostTopoPathCache) {
            log.info("cachedTopoPathService init");
            return new CachedTopoPathServiceImpl(topoPathService, hostTopoPathCache);
        }
    }

    @Bean
    @Primary
    public SwitchableTopoPathService switchableTopoPathService(@Qualifier("topoPathService")
                                                               TopoPathService topoPathService,
                                                               @Autowired(required = false)
                                                               @Qualifier("cachedTopoPathService")
                                                               TopoPathService cachedTopoPathService,
                                                               IamHostTopoPathProperties iamHostTopoPathProperties) {
        if (cachedTopoPathService != null) {
            return new SwitchableTopoPathService(cachedTopoPathService, iamHostTopoPathProperties);
        }
        return new SwitchableTopoPathService(topoPathService, iamHostTopoPathProperties);
    }

}
