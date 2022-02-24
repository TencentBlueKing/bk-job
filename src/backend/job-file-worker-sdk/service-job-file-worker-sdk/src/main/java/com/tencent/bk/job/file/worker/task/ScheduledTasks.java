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

package com.tencent.bk.job.file.worker.task;

import com.tencent.bk.job.file.worker.task.clear.ClearFileTask;
import com.tencent.bk.job.file.worker.task.heartbeat.HeartBeatTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class ScheduledTasks {

    private static final Logger logger = LoggerFactory.getLogger(ScheduledTasks.class);

    private final HeartBeatTask heartBeatTask;
    private final ClearFileTask clearFileTask;

    @Autowired
    public ScheduledTasks(HeartBeatTask heartBeatTask, ClearFileTask clearFileTask) {
        this.heartBeatTask = heartBeatTask;
        this.clearFileTask = clearFileTask;
    }

    /**
     * 清理：1天/次，每天早上8点清理过期文件
     */
    @Scheduled(cron = "0 0 8 * * *")
    public void clearExpiredFile() {
        logger.info(Thread.currentThread().getId() + ":clearExpiredFile start");
        try {
            clearFileTask.clearExpiredFile();
        } catch (Exception e) {
            logger.error("clearExpiredFileTask fail", e);
        }
    }

    /**
     * 每分钟检查磁盘容量并清理
     */
    @Scheduled(cron = "0 * * * * *")
    public void checkVolumeAndClear() {
        logger.debug(Thread.currentThread().getId() + ":checkVolumeAndClear start");
        try {
            clearFileTask.checkVolumeAndClear();
        } catch (Exception e) {
            logger.error("clearExpiredFileTask fail", e);
        }
    }

    /**
     * Worker心跳：1min/次
     */
    @Scheduled(fixedRate = 60 * 1000L)
    public void heartBeat() {
        logger.info(Thread.currentThread().getId() + ":heartBeat start");
        try {
            heartBeatTask.run();
        } catch (Exception e) {
            logger.error("heartBeatTask fail", e);
        }
    }
}
