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

package com.tencent.bk.job.manage.config;

import com.tencent.bk.job.common.gse.config.GseConfig;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClient;
import com.tencent.bk.job.common.gse.service.QueryAgentStatusClientImpl;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@AutoConfigureAfter(GseConfigForManage.class)
public class GseClientAutoConfig {

    @Bean
    @Autowired
    public QueryAgentStatusClient gseCacheClient(GseConfigForManage gseConfigForManage,
                                                 MeterRegistry meterRegistry) {
        GseConfig gseConfig = new GseConfig();
        gseConfig.setEnableSsl(gseConfigForManage.isEnableSsl());
        gseConfig.setGseCacheApiServerHost(gseConfigForManage.getGseCacheApiServerHosts());
        gseConfig.setGseCacheApiServerPort(gseConfigForManage.getGseCacheApiServerPort());
        gseConfig.setKeyStore(gseConfigForManage.getKeyStore());
        gseConfig.setKeyStorePass(gseConfigForManage.getKeyStorePass());
        gseConfig.setTrustStore(gseConfigForManage.getTrustStore());
        gseConfig.setTrustStorePass(gseConfigForManage.getTrustStorePass());
        gseConfig.setTrustStoreType(gseConfigForManage.getTrustStoreType());
        gseConfig.setTrustManagerType(gseConfigForManage.getTrustManagerType());
        gseConfig.setQueryBatchSize(gseConfigForManage.getGseQueryBatchSize());
        gseConfig.setQueryThreadsNum(gseConfigForManage.getGseQueryThreadsNum());
        return new QueryAgentStatusClientImpl(gseConfig, meterRegistry);
    }
}
