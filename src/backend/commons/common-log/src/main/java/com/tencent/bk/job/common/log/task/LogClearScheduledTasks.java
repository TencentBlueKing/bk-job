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

package com.tencent.bk.job.common.log.task;

import com.tencent.bk.job.common.log.config.LogClearByVolumeUsageProperties;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

@Slf4j
public class LogClearScheduledTasks {

    private static final Logger logger = LoggerFactory.getLogger(LogClearScheduledTasks.class);

    private final ClearLogFileByVolumeUsageTask clearLogFileByVolumeUsageTask;

    public LogClearScheduledTasks(LogClearByVolumeUsageProperties logClearByVolumeUsageProperties) {
        if (logClearByVolumeUsageProperties.isEnabled()) {
            this.clearLogFileByVolumeUsageTask = new ClearLogFileByVolumeUsageTask(
                logClearByVolumeUsageProperties.getMaxVolume()
            );
        } else {
            this.clearLogFileByVolumeUsageTask = null;
        }
    }

    /**
     * 清理：每20分钟根据磁盘使用量清理一次日志文件
     */
    @Scheduled(cron = "50 0/20 * * * ?")
    public void clearLogFileByVolumeUsage() {
        if (clearLogFileByVolumeUsageTask == null) {
            log.debug("clearLogFileByVolumeUsage not enabled, ignore clearLogFile");
            return;
        }
        logger.info("clearLogFileByVolumeUsage start");
        try {
            clearLogFileByVolumeUsageTask.checkVolumeAndClear();
        } catch (Exception e) {
            logger.error("clearLogFileByVolumeUsage fail", e);
        }
    }

}
