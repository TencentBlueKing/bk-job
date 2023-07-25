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

package com.tencent.bk.job.common.gse.v1.config;

import com.tencent.bk.job.common.gse.config.GseV1Properties;
import com.tencent.bk.job.common.gse.v1.CuratorFrameworkFactoryBean;
import com.tencent.bk.job.common.gse.v1.GseCacheClientFactory;
import com.tencent.bk.job.common.gse.v1.GseServer;
import com.tencent.bk.job.common.gse.v1.GseV1ApiClient;
import com.tencent.bk.job.common.gse.v1.GseZkServer;
import com.tencent.bk.job.common.gse.v1.IntervalIncrementForeverRetry;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Objects;


@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "gse.enabled", havingValue = "true")
@EnableConfigurationProperties(GseV1Properties.class)
public class GseV1AutoConfiguration {

    private static final String ZOOKEEPER_SERVER_TYPE = "zookeeper";

    @Bean("gseServer")
    @ConditionalOnMissingBean(name = "gseServer")
    public GseServer gseServer(GseV1Properties gseV1Properties) {
        return new GseServer(gseV1Properties);
    }

    /**
     * 启用 ZooKeeper 方式，初始化 GseZkServer
     *
     * @param curatorFramework ZooKeeper 连接组件
     */
    @Bean("gseServer")
    @ConditionalOnProperty(name = "gse.server.discovery.type", havingValue = ZOOKEEPER_SERVER_TYPE)
    public GseServer gseServer(GseV1Properties gseV1Properties, CuratorFramework curatorFramework) {
        return new GseZkServer(gseV1Properties, curatorFramework);
    }

    /**
     * 启用 ZooKeeper 方式，初始化 ZooKeeper 连接组件
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(name = "gse.server.discovery.type", havingValue = ZOOKEEPER_SERVER_TYPE)
    public CuratorFrameworkFactoryBean curatorFrameworkFactoryBean(GseV1Properties gseV1Properties) {
        Objects.requireNonNull(gseV1Properties.getServer().getZooKeeper());
        return new CuratorFrameworkFactoryBean(gseV1Properties.getServer().getZooKeeper(),
            new IntervalIncrementForeverRetry(60000));
    }

    @Bean("gseCacheClientFactory")
    public GseCacheClientFactory gseCacheClientFactory(GseV1Properties gseV1Properties) {
        return new GseCacheClientFactory(gseV1Properties);
    }

    @Bean("gseV1ApiClient")
    public GseV1ApiClient gseV1ApiClient(MeterRegistry meterRegistry,
                                         GseCacheClientFactory gseCacheClientFactory) {
        return new GseV1ApiClient(meterRegistry, gseCacheClientFactory);
    }

}
