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

package com.tencent.bk.job.file_gateway.service.dispatch.impl;

import com.tencent.bk.job.common.util.ThreadUtils;
import com.tencent.bk.job.file_gateway.model.resp.inner.TaskInfoDTO;
import com.tencent.bk.job.file_gateway.service.dispatch.ReDispatchTaskService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.helpers.MessageFormatter;

import java.util.HashSet;
import java.util.Set;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ReDispatchTask extends TimerTask {
    private static final AtomicInteger reDispatchThreadNum = new AtomicInteger(0);
    private static final Set<String> reDispatchingTaskIds = new HashSet<>();
    private final ReDispatchTaskService reDispatchTaskService;
    private final String fileSourceTaskId;
    private final Long intervalMills;

    ReDispatchTask(ReDispatchTaskService reDispatchTaskService,
                   String fileSourceTaskId,
                   Long intervalMills) {
        this.reDispatchTaskService = reDispatchTaskService;
        this.fileSourceTaskId = fileSourceTaskId;
        this.intervalMills = intervalMills;
    }

    public static Integer getReDispatchThreadsNum() {
        return reDispatchThreadNum.get();
    }

    @Override
    public void run() {
        synchronized (reDispatchingTaskIds) {
            if (reDispatchingTaskIds.contains(fileSourceTaskId)) {
                log.info("task {} already in reDispatching, ignore", fileSourceTaskId);
                return;
            }
            reDispatchingTaskIds.add(fileSourceTaskId);
            reDispatchThreadNum.incrementAndGet();
        }
        reDispatchTaskWithRetry();
        synchronized (reDispatchingTaskIds) {
            reDispatchingTaskIds.remove(fileSourceTaskId);
            reDispatchThreadNum.decrementAndGet();
        }
    }

    /**
     * 对文件源任务进行重调度，失败时进行重试
     */
    private void reDispatchTaskWithRetry() {
        boolean reDispatchSuccess = false;
        int retryCount = 0;
        int maxRetryCount = 3;
        while (!reDispatchSuccess && retryCount < maxRetryCount) {
            try {
                TaskInfoDTO taskInfoDTO = reDispatchTaskService.reDispatchFileSourceTask(fileSourceTaskId);
                reDispatchSuccess = true;
                log.debug("reDispatch result of {}:{}", fileSourceTaskId, taskInfoDTO);
            } catch (Exception e) {
                retryCount += 1;
                String message = MessageFormatter.format(
                    "Fail to redispatch task {}, wait {}ms to retry {}",
                    new Object[]{
                        fileSourceTaskId,
                        intervalMills,
                        retryCount
                    }
                ).getMessage();
                if (retryCount < maxRetryCount) {
                    log.info(message);
                    ThreadUtils.sleep(intervalMills);
                } else {
                    log.error(message, e);
                }
            }
        }
    }
}
