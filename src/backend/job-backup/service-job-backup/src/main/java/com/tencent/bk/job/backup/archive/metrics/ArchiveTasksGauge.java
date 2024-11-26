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

package com.tencent.bk.job.backup.archive.metrics;

import com.tencent.bk.job.backup.archive.service.ArchiveTaskService;
import com.tencent.bk.job.backup.constant.ArchiveTaskStatusEnum;
import com.tencent.bk.job.backup.constant.ArchiveTaskTypeEnum;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class ArchiveTasksGauge {

    /**
     * 监控指标 - 归档任务数量
     */
    private static final String METRIC_NAME_FAILED_ARCHIVE_TASK = "job.archive.task";

    /**
     * tag - 归档任务类型
     */
    private static final String TAG_TASK_TYPE = "task_type";

    /**
     * tag - 归档任务状态
     */
    private static final String TAG_STATUS = "status";

    /**
     * 需要监控的状态
     */
    private static final List<ArchiveTaskStatusEnum> MONITOR_STATUS_LIST = Arrays.asList(
        ArchiveTaskStatusEnum.PENDING,
        ArchiveTaskStatusEnum.SUSPENDED,
        ArchiveTaskStatusEnum.RUNNING,
        ArchiveTaskStatusEnum.FAIL
    );

    private volatile Map<ArchiveTaskStatusEnum, Integer> archiveTaskCountByStatus;

    private final ArchiveTaskService archiveTaskService;

    public ArchiveTasksGauge(MeterRegistry meterRegistry,
                             ArchiveTaskService archiveTaskService) {
        this.archiveTaskService = archiveTaskService;
        this.archiveTaskCountByStatus = archiveTaskService.countTaskByStatus(
            ArchiveTaskTypeEnum.JOB_INSTANCE, MONITOR_STATUS_LIST);
        for (ArchiveTaskStatusEnum status : MONITOR_STATUS_LIST) {
            meterRegistry.gauge(
                METRIC_NAME_FAILED_ARCHIVE_TASK,
                Tags.of(TAG_TASK_TYPE, ArchiveTaskTypeEnum.JOB_INSTANCE.name(),
                    TAG_STATUS, status.name()),
                this,
                (ArchiveTasksGauge statObject) -> statObject.countByStatus(status));
        }
    }

    private long countByStatus(ArchiveTaskStatusEnum status) {
        Integer count = archiveTaskCountByStatus.get(status);
        return count == null ? 0 : count;
    }


    /**
     * 触发指标查询
     */
    @Scheduled(cron = "0 0/2 * * * *")
    public void loadMetrics() {
        this.archiveTaskCountByStatus = archiveTaskService.countTaskByStatus(
            ArchiveTaskTypeEnum.JOB_INSTANCE, MONITOR_STATUS_LIST);
    }

}
