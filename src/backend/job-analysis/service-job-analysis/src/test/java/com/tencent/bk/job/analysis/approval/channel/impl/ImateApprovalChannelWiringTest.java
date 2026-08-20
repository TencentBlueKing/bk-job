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

package com.tencent.bk.job.analysis.approval.channel.impl;

import com.tencent.bk.job.analysis.config.ApprovalProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 单元测试 - IMate 渠道的 Bean 装配。
 * <p>
 * 其余单测都直接 new 出渠道实例，绕过了容器，因此装配失败只会在部署时才暴露：
 * {@link ImateApprovalChannel} 另有一个供单测注入 HttpHelper 的构造器，多构造器且都不带
 * {@code @Autowired} 时 Spring 会放弃构造器注入、回退去找无参构造器，启动即失败。
 * 这里让容器真的去装配一次，把该性质锁住。
 * <p>
 * 同时覆盖真实渠道与 Mock 渠道的互斥：同一渠道注册两个实现会让回查目标不确定。
 */
class ImateApprovalChannelWiringTest {

    private static final String PROP_URL = "job.analysis.approval.channels.imate.url=http://imate.example.com";
    private static final String PROP_MOCK_ON = "job.analysis.approval.channels.imate.mock.enabled=true";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(ChannelTestConfiguration.class));

    @Test
    @DisplayName("配置了渠道地址且未开启 Mock 时，真实渠道能被容器装配出来")
    void givenRealChannelConfigThenChannelWired() {
        contextRunner.withPropertyValues(PROP_URL)
            .run(context -> assertThat(context)
                .hasSingleBean(ImateApprovalChannel.class)
                .doesNotHaveBean(MockImateApprovalChannel.class));
    }

    @Test
    @DisplayName("开启 Mock 时只装配 Mock 渠道，与真实渠道严格互斥")
    void givenMockEnabledThenOnlyMockWired() {
        contextRunner.withPropertyValues(PROP_URL, PROP_MOCK_ON)
            .run(context -> assertThat(context)
                .hasSingleBean(MockImateApprovalChannel.class)
                .doesNotHaveBean(ImateApprovalChannel.class));
    }

    @Test
    @DisplayName("未配置渠道地址即视为渠道未就绪，两个实现都不装配")
    void givenNoUrlThenNoChannelWired() {
        contextRunner.run(context -> assertThat(context)
            .hasNotFailed()
            .doesNotHaveBean(ImateApprovalChannel.class)
            .doesNotHaveBean(MockImateApprovalChannel.class));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(ApprovalProperties.class)
    @Import({ImateApprovalChannel.class, MockImateApprovalChannel.class})
    static class ChannelTestConfiguration {
    }
}
