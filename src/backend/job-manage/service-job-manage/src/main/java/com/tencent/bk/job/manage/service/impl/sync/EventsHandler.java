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

import com.tencent.bk.job.common.cc.model.result.ResourceEvent;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.BlockingQueue;

@Slf4j
public abstract class EventsHandler<T> extends Thread {

    BlockingQueue<ResourceEvent<T>> queue;
    boolean idle;
    Long appId = null;

    public EventsHandler(BlockingQueue<ResourceEvent<T>> queue) {
        this.queue = queue;
    }

    public boolean isIdle() {
        return idle;
    }

    public Long getAppId() {
        return appId;
    }

    public void commitEvent(Long appId, ResourceEvent<T> event) {
        try {
            boolean result = this.queue.add(event);
            if (!result) {
                log.warn("Fail to commitEvent:{}", event);
            } else {
                this.idle = false;
                this.appId = appId;
            }
        } catch (Exception e) {
            log.warn("Fail to commitEvent:" + event, e);
        }
    }

    abstract void handleEvent(ResourceEvent<T> event);

    @Override
    public void run() {
        while (true) {
            ResourceEvent<T> event = null;
            try {
                event = queue.take();
                handleEvent(event);
            } catch (InterruptedException e) {
                log.warn("queue.take interrupted", e);
            } catch (Throwable t) {
                log.warn("Fail to handleOneEvent:" + event, t);
            } finally {
                if (queue.size() == 0) {
                    this.idle = true;
                    this.appId = null;
                }
            }
        }
    }
}
