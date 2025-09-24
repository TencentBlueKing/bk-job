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
import com.tencent.bk.job.backup.archive.dao.impl.FileSourceTaskLogRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseFileExecuteObjTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseScriptExecuteObjTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.GseTaskRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.JobInstanceHotRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.OperationLogRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.RollingConfigRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceConfirmRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceFileRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceScriptRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.StepInstanceVariableRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.TaskInstanceHostRecordDAO;
import com.tencent.bk.job.backup.archive.dao.impl.TaskInstanceVariableRecordDAO;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedFileSourceTaskLogDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedGseFileExecuteObjectTaskDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedGseScriptExecuteObjTaskDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedGseTaskDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedOperationLogDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedRollingConfigDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedStepInstanceConfirmDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedStepInstanceDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedStepInstanceFileDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedStepInstanceScriptDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedStepInstanceVariableDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedTaskInstanceDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedTaskInstanceHostDetector;
import com.tencent.bk.job.backup.archive.service.detector.UnarchivedTaskInstanceVariableDetector;
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
    public UnarchivedFileSourceTaskLogDetector unarchivedFileSourceTaskLogDetector(
        MeterRegistry meterRegistry,
        ArchiveTaskDAO archiveTaskDAO,
        FileSourceTaskLogRecordDAO fileSourceTaskLogRecordDAO,
        ArchiveProperties archiveProperties
    ) {
        log.info("Init UnarchivedFileSourceTaskLogDetector");
        return new UnarchivedFileSourceTaskLogDetector(
            meterRegistry,
            archiveTaskDAO,
            fileSourceTaskLogRecordDAO,
            archiveProperties);
    }

    @Bean
    public UnarchivedGseFileExecuteObjectTaskDetector unarchivedGseFileExecuteObjectTaskDetector(
        MeterRegistry meterRegistry,
        ArchiveTaskDAO archiveTaskDAO,
        GseFileExecuteObjTaskRecordDAO gseFileExecuteObjTaskRecordDAO,
        ArchiveProperties archiveProperties
    ) {
        log.info("Init UnarchivedGseFileExecuteObjectTaskDetector");
        return new UnarchivedGseFileExecuteObjectTaskDetector(
            meterRegistry,
            archiveTaskDAO,
            gseFileExecuteObjTaskRecordDAO,
            archiveProperties);
    }

    @Bean
    public UnarchivedGseScriptExecuteObjTaskDetector unarchivedGseScriptExecuteObjTaskDetector(
        MeterRegistry meterRegistry,
        ArchiveTaskDAO archiveTaskDAO,
        GseScriptExecuteObjTaskRecordDAO gseScriptExecuteObjTaskRecordDAO,
        ArchiveProperties archiveProperties
    ) {
        log.info("Init UnarchivedGseScriptExecuteObjTaskDetector");
        return new UnarchivedGseScriptExecuteObjTaskDetector(
            meterRegistry,
            archiveTaskDAO,
            gseScriptExecuteObjTaskRecordDAO,
            archiveProperties
        );
    }

    @Bean
    public UnarchivedGseTaskDetector unarchivedGseTaskDetector(
        MeterRegistry meterRegistry,
        ArchiveTaskDAO archiveTaskDAO,
        GseTaskRecordDAO gseTaskRecordDAO,
        ArchiveProperties archiveProperties
    ) {
        log.info("Init UnarchivedGseTaskDetector");
        return new UnarchivedGseTaskDetector(
            meterRegistry,
            archiveTaskDAO,
            gseTaskRecordDAO,
            archiveProperties
        );
    }

    @Bean
    public UnarchivedOperationLogDetector unarchivedOperationLogDetector(
        MeterRegistry meterRegistry,
        ArchiveTaskDAO archiveTaskDAO,
        OperationLogRecordDAO operationLogRecordDAO,
        ArchiveProperties archiveProperties
    ) {
        log.info("Init UnarchivedOperationLogDetector");
        return new UnarchivedOperationLogDetector(
            meterRegistry,
            archiveTaskDAO,
            operationLogRecordDAO,
            archiveProperties
        );
    }

    @Bean
    public UnarchivedRollingConfigDetector unarchivedRollingConfigDetector(
        MeterRegistry meterRegistry,
        ArchiveTaskDAO archiveTaskDAO,
        RollingConfigRecordDAO rollingConfigRecordDAO,
        ArchiveProperties archiveProperties
    ) {
        log.info("Init UnarchivedRollingConfigDetector");
        return new UnarchivedRollingConfigDetector(
            meterRegistry,
            archiveTaskDAO,
            rollingConfigRecordDAO,
            archiveProperties
        );
    }

    @Bean
    public UnarchivedStepInstanceConfirmDetector unarchivedStepInstanceConfirmDetector(
        MeterRegistry meterRegistry,
        ArchiveTaskDAO archiveTaskDAO,
        StepInstanceConfirmRecordDAO stepInstanceConfirmRecordDAO,
        ArchiveProperties archiveProperties
    ) {
        log.info("Init UnarchivedStepInstanceConfirmDetector");
        return new UnarchivedStepInstanceConfirmDetector(
            meterRegistry,
            archiveTaskDAO,
            stepInstanceConfirmRecordDAO,
            archiveProperties
        );
    }

    @Bean
    public UnarchivedStepInstanceDetector unarchivedStepInstanceDetector(
        MeterRegistry meterRegistry,
        ArchiveTaskDAO archiveTaskDAO,
        StepInstanceRecordDAO stepInstanceRecordDAO,
        ArchiveProperties archiveProperties
    ) {
        log.info("Init UnarchivedStepInstanceDetector");
        return new UnarchivedStepInstanceDetector(
            meterRegistry,
            archiveTaskDAO,
            stepInstanceRecordDAO,
            archiveProperties
        );
    }

    @Bean
    public UnarchivedStepInstanceFileDetector unarchivedStepInstanceFileDetector(
        MeterRegistry meterRegistry,
        ArchiveTaskDAO archiveTaskDAO,
        StepInstanceFileRecordDAO stepInstanceFileRecordDAO,
        ArchiveProperties archiveProperties
    ) {
        log.info("Init UnarchivedStepInstanceFileDetector");
        return new UnarchivedStepInstanceFileDetector(
            meterRegistry,
            archiveTaskDAO,
            stepInstanceFileRecordDAO,
            archiveProperties
        );
    }

    @Bean
    public UnarchivedStepInstanceScriptDetector unarchivedStepInstanceScriptDetector(
        MeterRegistry meterRegistry,
        ArchiveTaskDAO archiveTaskDAO,
        StepInstanceScriptRecordDAO stepInstanceScriptRecordDAO,
        ArchiveProperties archiveProperties
    ) {
        log.info("Init UnarchivedStepInstanceScriptDetector");
        return new UnarchivedStepInstanceScriptDetector(
            meterRegistry,
            archiveTaskDAO,
            stepInstanceScriptRecordDAO,
            archiveProperties
        );
    }

    @Bean
    public UnarchivedStepInstanceVariableDetector unarchivedStepInstanceVariableDetector(
        MeterRegistry meterRegistry,
        ArchiveTaskDAO archiveTaskDAO,
        StepInstanceVariableRecordDAO stepInstanceVariableRecordDAO,
        ArchiveProperties archiveProperties
    ) {
        log.info("Init UnarchivedStepInstanceVariableDetector");
        return new UnarchivedStepInstanceVariableDetector(
            meterRegistry,
            archiveTaskDAO,
            stepInstanceVariableRecordDAO,
            archiveProperties
        );
    }

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
    public UnarchivedTaskInstanceHostDetector unarchivedTaskInstanceHostDetector(
        MeterRegistry meterRegistry,
        ArchiveTaskDAO archiveTaskDAO,
        TaskInstanceHostRecordDAO taskInstanceHostRecordDAO,
        ArchiveProperties archiveProperties
    ) {
        log.info("Init UnarchivedTaskInstanceHostDetector");
        return new UnarchivedTaskInstanceHostDetector(
            meterRegistry,
            archiveTaskDAO,
            taskInstanceHostRecordDAO,
            archiveProperties
        );
    }

    @Bean
    public UnarchivedTaskInstanceVariableDetector unarchivedTaskInstanceVariableDetector(
        MeterRegistry meterRegistry,
        ArchiveTaskDAO archiveTaskDAO,
        TaskInstanceVariableRecordDAO taskInstanceVariableRecordDAO,
        ArchiveProperties archiveProperties
    ) {
        log.info("Init UnarchivedTaskInstanceVariableDetector");
        return new UnarchivedTaskInstanceVariableDetector(
            meterRegistry,
            archiveTaskDAO,
            taskInstanceVariableRecordDAO,
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
        UnarchivedFileSourceTaskLogDetector unarchivedFileSourceTaskLogDetector,
        UnarchivedGseFileExecuteObjectTaskDetector unarchivedGseFileExecuteObjectTaskDetector,
        UnarchivedGseScriptExecuteObjTaskDetector unarchivedGseScriptExecuteObjTaskDetector,
        UnarchivedGseTaskDetector unarchivedGseTaskDetector,
        UnarchivedOperationLogDetector unarchivedOperationLogDetector,
        UnarchivedRollingConfigDetector unarchivedRollingConfigDetector,
        UnarchivedStepInstanceConfirmDetector unarchivedStepInstanceConfirmDetector,
        UnarchivedStepInstanceDetector unarchivedStepInstanceDetector,
        UnarchivedStepInstanceFileDetector unarchivedStepInstanceFileDetector,
        UnarchivedStepInstanceScriptDetector unarchivedStepInstanceScriptDetector,
        UnarchivedStepInstanceVariableDetector unarchivedStepInstanceVariableDetector,
        UnarchivedTaskInstanceDetector unarchivedTaskInstanceDetector,
        UnarchivedTaskInstanceHostDetector unarchivedTaskInstanceHostDetector,
        UnarchivedTaskInstanceVariableDetector unarchivedTaskInstanceVariableDetector
    ) {
        log.info("Init HistoricalDataCheckTaskLauncher");
        return new HistoricalDataCheckTaskLauncher(
            checkTaskLaunchLock,
            unarchivedFileSourceTaskLogDetector,
            unarchivedGseFileExecuteObjectTaskDetector,
            unarchivedGseScriptExecuteObjTaskDetector,
            unarchivedGseTaskDetector,
            unarchivedOperationLogDetector,
            unarchivedRollingConfigDetector,
            unarchivedStepInstanceConfirmDetector,
            unarchivedStepInstanceDetector,
            unarchivedStepInstanceFileDetector,
            unarchivedStepInstanceScriptDetector,
            unarchivedStepInstanceVariableDetector,
            unarchivedTaskInstanceDetector,
            unarchivedTaskInstanceHostDetector,
            unarchivedTaskInstanceVariableDetector);
    }
}
