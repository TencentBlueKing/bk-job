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

import com.tencent.bk.job.execute.engine.gse.GseServer;
import com.tencent.bk.job.execute.engine.gse.GseZkServer;
import com.tencent.bk.job.execute.engine.gse.IntervalIncrementForeverRetry;
import org.apache.curator.framework.CuratorFramework;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @since 22/12/2020 21:41
 */
@Configuration
public class GseServerAutoConfig {

    private static final String ZOOKEEPER_SERVER_TYPE = "zookeeper";

    /**
     * GSE ZooKeeper 连接信息
     */
    @Value("${gse.server.zookeeper.connect.string:}")
    private String gseServerZkConnectString;

    /**
     * 储存 GSE 接入服务器信息的 ZooKeeper 节点
     */
    @Value("${gse.server.zookeeper.path:/gse/config/server/task/accessv3}")
    private String gseServerZkPath;

    /**
     * ZooKeeper 会话超时时间
     */
    @Value("${gse.server.zookeeper.timeout.session:60000}")
    private Integer gseServerZkSessionTimeOutMills;

    /**
     * ZooKeeper 连接超时时间
     */
    @Value("${gse.server.zookeeper.timeout.connect:60000}")
    private Integer gseServerZkConnectTimeOutMills;

    /**
     * 不存在 gseServer，回落到 domain/ip list 方式
     */
    @Bean("gseServer")
    @ConditionalOnMissingBean(name = "gseServer")
    public GseServer gseServer() {
        return new GseServer();
    }

    /**
     * 启用 ZooKeeper 方式，初始化 GseZkServer
     *
     * @param curatorFramework ZooKeeper 连接组件
     */
    @Bean("gseServer")
    @ConditionalOnProperty(name = "gse.server.discovery.type", havingValue = ZOOKEEPER_SERVER_TYPE)
    public GseServer gseServer(CuratorFramework curatorFramework) {
        return new GseZkServer(curatorFramework, gseServerZkPath);
    }

    /**
     * 启用 ZooKeeper 方式，初始化 ZooKeeper 连接组件
     */
    @Bean(initMethod = "start", destroyMethod = "stop")
    @ConditionalOnProperty(name = "gse.server.discovery.type", havingValue = ZOOKEEPER_SERVER_TYPE)
    public CuratorFrameworkFactoryBean curatorFrameworkFactoryBean() {
        return new CuratorFrameworkFactoryBean(gseServerZkConnectString, gseServerZkSessionTimeOutMills,
            gseServerZkConnectTimeOutMills, new IntervalIncrementForeverRetry(60000));
    }
}
