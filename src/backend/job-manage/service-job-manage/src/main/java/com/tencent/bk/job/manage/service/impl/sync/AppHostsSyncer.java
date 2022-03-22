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

package com.tencent.bk.job.manage.service.impl.sync;

import com.tencent.bk.job.common.model.dto.ApplicationDTO;
import com.tencent.bk.job.manage.service.ApplicationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.util.Pair;

import java.util.concurrent.BlockingQueue;

/**
 * 业务主机同步线程，从队列中取出需要同步的业务同步其主机
 */
@Slf4j
public class AppHostsSyncer extends Thread {

    private final ApplicationService applicationService;
    private final HostSyncService hostSyncService;
    volatile BlockingQueue<Long> queue;

    public AppHostsSyncer(ApplicationService applicationService,
                          HostSyncService hostSyncService,
                          BlockingQueue<Long> queue) {
        this.applicationService = applicationService;
        this.hostSyncService = hostSyncService;
        this.queue = queue;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Long appId = queue.take();
                ApplicationDTO applicationDTO = applicationService.getAppByAppId(appId);
                Pair<Long, Long> timeConsumingPair = hostSyncService.syncBizHostsAtOnce(applicationDTO);
                Long cmdbInterfaceTimeConsuming = timeConsumingPair.getFirst();
                Long writeToDBTimeConsuming = timeConsumingPair.getSecond();
                log.info("Sync appHosts of {}:cmdbInterfaceTimeConsuming={}ms,writeToDBTimeConsuming={}ms," +
                        "rate={}", appId, cmdbInterfaceTimeConsuming, writeToDBTimeConsuming,
                    cmdbInterfaceTimeConsuming / (0. + writeToDBTimeConsuming));
            } catch (InterruptedException e) {
                log.warn("queue.take interrupted", e);
            } catch (Throwable t) {
                log.warn("Fail to syncAppHostsAtOnce", t);
            }
        }
    }
}
