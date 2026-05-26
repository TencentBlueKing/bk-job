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

package com.tencent.bk.job.manage.passwordrotation;

import com.tencent.bk.job.common.crypto.CryptoConfigService;
import com.tencent.bk.job.common.crypto.SymmetricCryptoService;
import com.tencent.bk.job.common.crypto.passwordrotation.FieldRewriter;
import com.tencent.bk.job.common.crypto.passwordrotation.PasswordRotationConfig;
import com.tencent.bk.job.common.crypto.passwordrotation.PasswordRotationLockExecutor;
import com.tencent.bk.job.common.crypto.passwordrotation.PasswordRotationOrchestrator;
import com.tencent.bk.job.common.crypto.passwordrotation.PasswordRotationProgressDAO;
import com.tencent.bk.job.common.crypto.passwordrotation.PasswordRotationStartupTrigger;
import com.tencent.bk.job.common.redis.util.DistributedUniqueTask;
import com.tencent.bk.job.common.util.ip.IpUtils;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

/**
 * 密码轮换自动装配：注册 {@link PasswordRotationConfig} 配置 Bean、编排器 Bean
 * 与启动触发器 Bean（applicationName=job-manage）。
 * <p>
 * 所有 Bean 名均带 service 前缀，避免轻量化部署模式（job-assemble）下
 * 与其他 service 的同名配置/Bean 注册冲突；通过 @Qualifier 显式绑定本 service 的
 * 进度 DAO 与 FieldRewriter 分组，确保只编排属于本 service 的迁移任务。
 */
@Configuration("jobManagePasswordRotationAutoConfiguration")
@EnableConfigurationProperties(PasswordRotationConfig.class)
public class PasswordRotationAutoConfiguration {

    private static final String APPLICATION_NAME = "job-manage";

    @Bean("jobManagePasswordRotationOrchestrator")
    public PasswordRotationOrchestrator passwordRotationOrchestrator(
        SymmetricCryptoService symmetricCryptoService,
        CryptoConfigService cryptoConfigService,
        @Qualifier("jobManagePasswordRotationProgressDAOImpl") PasswordRotationProgressDAO progressDAO,
        @Qualifier("jobManagePasswordRotationRewriter") List<FieldRewriter> rewriters,
        PasswordRotationConfig config,
        MeterRegistry meterRegistry
    ) {
        return new PasswordRotationOrchestrator(
            symmetricCryptoService, cryptoConfigService, progressDAO, rewriters, config, meterRegistry);
    }

    @Bean("jobManagePasswordRotationStartupTrigger")
    public PasswordRotationStartupTrigger passwordRotationStartupTrigger(
        RedisTemplate<String, String> redisTemplate,
        @Qualifier("jobManagePasswordRotationOrchestrator") PasswordRotationOrchestrator orchestrator,
        PasswordRotationConfig config,
        CryptoConfigService cryptoConfigService
    ) {
        PasswordRotationLockExecutor lockExecutor = (lockKey, task) ->
            new DistributedUniqueTask<Void>(
                redisTemplate,
                PasswordRotationStartupTrigger.buildTaskName(APPLICATION_NAME),
                lockKey,
                IpUtils.getFirstMachineIP(),
                () -> {
                    task.run();
                    return null;
                }
            ).execute();
        return new PasswordRotationStartupTrigger(
            APPLICATION_NAME, orchestrator, config, lockExecutor, cryptoConfigService);
    }
}
