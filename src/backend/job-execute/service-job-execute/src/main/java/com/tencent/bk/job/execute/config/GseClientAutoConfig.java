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

import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClientImpl;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GseClientAutoConfig {

    @Bean
    @Autowired
    public QueryAgentStatusClient gseCacheClient(GseConfig gseConfigForExecute,
                                                 MeterRegistry meterRegistry) {
        com.tencent.bk.job.common.gse.config.GseConfig gseConfig = new com.tencent.bk.job.common.gse.config.GseConfig();
        gseConfig.setEnableSsl(gseConfigForExecute.isGseSSLEnable());
        String[] cacheApiServerHosts = gseConfigForExecute.getGseCacheApiServerHost().split(",");
        gseConfig.setGseCacheApiServerHost(cacheApiServerHosts);
        gseConfig.setGseCacheApiServerPort(gseConfigForExecute.getGseCacheApiServerPort());
        gseConfig.setKeyStore(gseConfigForExecute.getGseSSLKeystore());
        gseConfig.setKeyStorePass(gseConfigForExecute.getGseSSLKeystorePassword());
        gseConfig.setTrustStore(gseConfigForExecute.getGseSSLTruststore());
        gseConfig.setTrustStorePass(gseConfigForExecute.getGseSSLTruststorePassword());
        gseConfig.setTrustStoreType(gseConfigForExecute.getGseSSLTruststoreStoreType());
        gseConfig.setTrustManagerType(gseConfigForExecute.getGseSSLTruststoreManagerType());
        gseConfig.setQueryBatchSize(gseConfigForExecute.getGseQueryBatchSize());
        gseConfig.setQueryThreadsNum(gseConfigForExecute.getGseQueryThreadsNum());
        return new QueryAgentStatusClientImpl(gseConfig, meterRegistry);
    }
}
