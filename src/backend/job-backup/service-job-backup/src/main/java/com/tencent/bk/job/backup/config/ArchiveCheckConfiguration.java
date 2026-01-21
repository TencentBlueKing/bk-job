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

package com.tencent.bk.job.backup.config;

import com.tencent.bk.job.backup.archive.HistoricalDataCheckTaskLauncher;
import com.tencent.bk.job.backup.archive.dao.ArchiveTaskDAO;
import com.tencent.bk.job.backup.archive.dao.impl.JobInstanceHotRecordDAO;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedTaskInstanceDetector;
import com.tencent.bk.job.backup.archive.util.lock.CheckTaskLaunchLock;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * 定时检查热DB中是否有未归档的数据
 */
@Configuration
@ConditionalOnArchiveCheckEnabled
@Slf4j
public class ArchiveCheckConfiguration {

    @Bean
    public UnarchivedTaskInstanceDetector unarchivedTaskInstanceDetector(
        MeterRegistry meterRegistry,
        ArchiveTaskDAO archiveTaskDAO,
        JobInstanceHotRecordDAO jobInstanceHotRecordDAO,
        ArchiveProperties archiveProperties
    ) {
        log.info("Init UnarchivedTaskInstanceDetector");
        return new UnarchivedTaskInstanceDetector(
            meterRegistry,
            archiveTaskDAO,
            jobInstanceHotRecordDAO,
            archiveProperties
        );
    }

    @Bean
    public CheckTaskLaunchLock checkTaskLaunchLock(StringRedisTemplate stringRedisTemplate) {
        log.info("Init CheckTaskLaunchLock");
        return new CheckTaskLaunchLock(stringRedisTemplate);
    }

    @Bean
    public HistoricalDataCheckTaskLauncher historicalDataCheckTaskLauncher(
        CheckTaskLaunchLock checkTaskLaunchLock,
        UnarchivedTaskInstanceDetector unarchivedTaskInstanceDetector
    ) {
        log.info("Init HistoricalDataCheckTaskLauncher");
        return new HistoricalDataCheckTaskLauncher(
            checkTaskLaunchLock,
            unarchivedTaskInstanceDetector
        );
    }
}
