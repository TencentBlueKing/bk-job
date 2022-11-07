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

package com.tencent.bk.job.common.cc.config;

import com.tencent.bk.job.common.cc.sdk.BizCmdbClient;
import com.tencent.bk.job.common.esb.config.BkApiConfig;
import com.tencent.bk.job.common.esb.constants.EsbLang;
import com.tencent.bk.job.common.util.FlowController;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class BizCmdbClientAutoConfig {

    @Bean("cmdbThreadPoolExecutor")
    public ThreadPoolExecutor cmdbThreadPoolExecutor(CmdbConfig cmdbConfig) {
        int cmdbQueryThreadsNum = cmdbConfig.getCmdbQueryThreadsNum();
        return new ThreadPoolExecutor(
            cmdbQueryThreadsNum,
            cmdbQueryThreadsNum,
            180L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(cmdbQueryThreadsNum * 4), (r, executor) -> {
            //使用请求的线程直接拉取数据
            log.error("cmdb request runnable rejected, use current thread({}), plz add more threads",
                Thread.currentThread().getName());
            r.run();
        });
    }

    @Bean("cmdbLongTermThreadPoolExecutor")
    public ThreadPoolExecutor cmdbLongTermThreadPoolExecutor(CmdbConfig cmdbConfig) {
        int longTermCmdbQueryThreadsNum = cmdbConfig.getFindHostRelationLongTermConcurrency();
        return new ThreadPoolExecutor(
            longTermCmdbQueryThreadsNum,
            longTermCmdbQueryThreadsNum,
            180L,
            TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(longTermCmdbQueryThreadsNum * 4), (r, executor) -> {
            //使用请求的线程直接拉取数据
            log.warn("cmdb long term request runnable rejected, use current thread({}), plz add more threads",
                Thread.currentThread().getName());
            r.run();
        });
    }

    @Bean
    @Primary
    public BizCmdbClient bizCmdbClient(BkApiConfig bkApiConfig,
                                       CmdbConfig cmdbConfig,
                                       ThreadPoolExecutor cmdbThreadPoolExecutor,
                                       ThreadPoolExecutor cmdbLongTermThreadPoolExecutor,
                                       MeterRegistry meterRegistry,
                                       @Autowired(required = false) FlowController flowController) {
        return new BizCmdbClient(
            bkApiConfig,
            cmdbConfig,
            EsbLang.EN,
            cmdbThreadPoolExecutor,
            cmdbLongTermThreadPoolExecutor,
            flowController,
            meterRegistry
        );
    }

    @Bean("cnBizCmdbClient")
    public BizCmdbClient cnBizCmdbClient(BkApiConfig bkApiConfig,
                                         CmdbConfig cmdbConfig,
                                         ThreadPoolExecutor cmdbThreadPoolExecutor,
                                         ThreadPoolExecutor cmdbLongTermThreadPoolExecutor,
                                         MeterRegistry meterRegistry,
                                         @Autowired(required = false) FlowController flowController) {
        return new BizCmdbClient(
            bkApiConfig,
            cmdbConfig,
            EsbLang.CN,
            cmdbThreadPoolExecutor,
            cmdbLongTermThreadPoolExecutor,
            flowController,
            meterRegistry
        );
    }
}
