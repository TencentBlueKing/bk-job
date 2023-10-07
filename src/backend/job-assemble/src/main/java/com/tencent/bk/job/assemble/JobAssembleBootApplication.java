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

package com.tencent.bk.job.assemble;

import com.tencent.bk.job.common.service.boot.JobBootApplication;
import com.tencent.bk.job.common.service.feature.config.FeatureToggleConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.availability.ApplicationAvailabilityAutoConfiguration;
import org.springframework.boot.autoconfigure.jooq.JooqAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@JobBootApplication(
    scanBasePackages =
        {
            "com.tencent.bk.job.assemble",
            "com.tencent.bk.job.manage",
            "com.tencent.bk.job.execute",
            "com.tencent.bk.job.crontab",
            "com.tencent.bk.job.logsvr",
            "com.tencent.bk.job.analysis",
            "com.tencent.bk.job.file_gateway",
            "com.tencent.bk.job.backup",
            "com.tencent.bk.job.common.config"
        },
    exclude = {JooqAutoConfiguration.class, ApplicationAvailabilityAutoConfiguration.class}
)
@EnableCaching
@EnableScheduling
@EnableConfigurationProperties({FeatureToggleConfig.class})
@EnableFeignClients(basePackages = "com.tencent.bk.job")
public class JobAssembleBootApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobAssembleBootApplication.class, args);
    }

}
