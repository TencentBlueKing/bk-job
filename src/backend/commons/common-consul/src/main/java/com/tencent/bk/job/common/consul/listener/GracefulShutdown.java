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

package com.tencent.bk.job.common.consul.listener;

import com.tencent.bk.job.common.consul.config.JobConsulProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.consul.serviceregistry.ConsulRegistration;
import org.springframework.cloud.consul.serviceregistry.ConsulServiceRegistry;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

import static org.springframework.boot.actuate.health.Status.OUT_OF_SERVICE;

@Slf4j
public class GracefulShutdown implements ApplicationListener<ContextClosedEvent> {

    private final ConsulRegistration consulRegistration;
    private final ConsulServiceRegistry consulServiceRegistry;
    private final JobConsulProperties jobConsulProperties;

    public GracefulShutdown(ConsulRegistration consulRegistration,
                            ConsulServiceRegistry consulServiceRegistry,
                            JobConsulProperties jobConsulProperties) {
        log.info("Init consul service graceful shutdown component");
        this.consulRegistration = consulRegistration;
        this.consulServiceRegistry = consulServiceRegistry;
        this.jobConsulProperties = jobConsulProperties;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("begin to offline...");
        // 1.下线服务
        try {
            consulServiceRegistry.setStatus(consulRegistration, OUT_OF_SERVICE.getCode());
        } catch (Exception ignore) {
            log.info("Fail to set consul status");
        }
        // 2.等待网关刷新consul服务状态
        try {
            Thread.sleep(jobConsulProperties.getRefreshSeconds() * 1000L);
        } catch (InterruptedException e) {
            log.error("Error when offline", e);
        }
        log.info("offline done, begin to deregister");
        try {
            consulServiceRegistry.deregister(consulRegistration);
        } catch (Exception ignore) {
            log.info("Fail to deregister consul");
        }
    }
}
