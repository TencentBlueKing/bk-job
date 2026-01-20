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

package com.tencent.bk.job.gateway.web.server;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.autoconfigure.web.embedded.NettyWebServerFactoryCustomizer;
import org.springframework.boot.web.embedded.netty.NettyReactiveWebServerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 扩展主业务的NettyWebServerCustomizer
 */
@Component
@Slf4j
public class GatewayWebServerFactoryCustomizer extends NettyWebServerFactoryCustomizer {

    private final List<NettyFactoryCustomizer> customizers;

    public GatewayWebServerFactoryCustomizer(Environment environment,
                                             ServerProperties serverProperties,
                                             List<NettyFactoryCustomizer> customizers) {
        super(environment, serverProperties);
        this.customizers = customizers;
    }

    @Override
    public void customize(NettyReactiveWebServerFactory factory) {
        super.customize(factory);
        if (!customizers.isEmpty()) {
            for (NettyFactoryCustomizer customizer : customizers) {
                try {
                    log.debug("Gateway applying additional Netty customizer: {}",
                        customizer.getClass().getSimpleName());
                    customizer.customize(factory);
                } catch (Exception e) {
                    log.warn("Gateway applying additional Netty customizer {} failed.",
                        customizer.getClass().getSimpleName(), e);
                }
            }
        }
    }
}
