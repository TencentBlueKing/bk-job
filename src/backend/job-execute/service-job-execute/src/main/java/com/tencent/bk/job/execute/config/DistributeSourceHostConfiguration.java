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

package com.tencent.bk.job.execute.config;

import com.tencent.bk.job.common.gse.service.AgentStateClient;
import com.tencent.bk.job.common.tenant.TenantEnvService;
import com.tencent.bk.job.execute.engine.prepare.third.FileWorkerHostService;
import com.tencent.bk.job.execute.service.AgentService;
import com.tencent.bk.job.execute.service.ExternalAgentService;
import com.tencent.bk.job.execute.service.HostService;
import com.tencent.bk.job.execute.service.LocalFileDistributeSourceHostProvisioner;
import com.tencent.bk.job.execute.service.ThirdFileDistributeSourceHostProvisioner;
import com.tencent.bk.job.execute.service.impl.ExternalAgentServiceImpl;
import com.tencent.bk.job.execute.service.impl.FileWorkerHostProvisioner;
import com.tencent.bk.job.execute.service.impl.LocalFileExternalAgentHostProvisioner;
import com.tencent.bk.job.execute.service.impl.LocalAgentHostProvisioner;
import com.tencent.bk.job.execute.service.impl.ThirdFileExternalAgentHostProvisioner;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.tencent.bk.job.execute.config.GseConfig.EXECUTE_BEAN_AGENT_STATE_CLIENT;

/**
 * 通过开关控制使用job机器分发文件 还是使用 配置的集群外机器分发文件
 */
@Configuration
@Slf4j
public class DistributeSourceHostConfiguration {

    /**
     * 采用集群外机器作为分发源
     */
    @ConditionalOnExternalAgentEnabled
    @Bean
    ExternalAgentService externalAgentService(NFSExternalAgentHostConfig nfsExternalAgentHostConfig,
                                              HostService hostService,
                                              @Qualifier(EXECUTE_BEAN_AGENT_STATE_CLIENT)
                                              AgentStateClient agentStateClient,
                                              TenantEnvService tenantEnvService) {
        return new ExternalAgentServiceImpl(
            nfsExternalAgentHostConfig,
            hostService,
            agentStateClient,
            tenantEnvService
        );
    }

    /**
     * 文件源文件分发，采用集群外机器作为分发源
     *
     */
    @ConditionalOnExternalAgentEnabled
    @Bean
    ThirdFileDistributeSourceHostProvisioner thirdFileExternalAgentHostProvisioner(
        ExternalAgentService externalAgentService
    ) {
        log.info("init ThirdFileExternalAgentHostProvisioner");
        return new ThirdFileExternalAgentHostProvisioner(externalAgentService);
    }

    /**
     * 文件源文件分发，采用 job-file-worker 所在node进行分发
     *
     */
    @ConditionalOnExternalAgentDisabled
    @Bean
    ThirdFileDistributeSourceHostProvisioner fileWorkerHostProvisioner(FileWorkerHostService fileWorkerHostService) {
        log.info("init FileWorkerHostProvisioner");
        return new FileWorkerHostProvisioner(fileWorkerHostService);
    }

    /**
     * 本地文件分发，采用集群外的机器作为分发源
     *
     */
    @ConditionalOnExternalAgentEnabled
    @Bean
    LocalFileDistributeSourceHostProvisioner localFileExternalAgentHostProvisioner(
        ExternalAgentService externalAgentService
    ) {
        log.info("init LocalFileExternalAgentHostProvisioner");
        return new LocalFileExternalAgentHostProvisioner(externalAgentService);
    }

    /**
     * 本地文件分发，采用job-execute所在node进行分发
     *
     */
    @ConditionalOnExternalAgentDisabled
    @Bean
    LocalFileDistributeSourceHostProvisioner localAgentHostProvisioner(AgentService agentService) {
        log.info("init LocalAgentHostProvisioner");
        return new LocalAgentHostProvisioner(agentService);
    }
}
