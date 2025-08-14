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

package com.tencent.bk.job.manage.background.ha.mq;

import com.tencent.bk.job.manage.background.event.cmdb.CmdbEventManager;
import com.tencent.bk.job.manage.background.ha.BackGroundTaskBalancer;
import com.tencent.bk.job.manage.background.ha.BackGroundTaskCode;
import com.tencent.bk.job.manage.background.ha.TaskEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

/**
 * 后台任务监听器，用于监听从MQ接收到的任务并处理
 */
@Slf4j
@Service
public class BackGroundTaskListener {
    private final CmdbEventManager cmdbEventManager;
    private final BackGroundTaskBalancer backGroundTaskBalancer;

    @Autowired
    public BackGroundTaskListener(CmdbEventManager cmdbEventManager,
                                  BackGroundTaskBalancer backGroundTaskBalancer) {
        this.cmdbEventManager = cmdbEventManager;
        this.backGroundTaskBalancer = backGroundTaskBalancer;
    }

    public void handleTask(Message<TaskEntity> taskEntityMessage) {
        TaskEntity taskEntity = taskEntityMessage.getPayload();
        log.info("Received task from queue:{}", taskEntity);
        switch (taskEntity.getTaskCode()) {
            case BackGroundTaskCode.WATCH_BIZ:
                cmdbEventManager.watchBizEvent(taskEntity.getTenantId());
                break;
            case BackGroundTaskCode.WATCH_BIZ_SET:
                cmdbEventManager.watchBizSetEvent(taskEntity.getTenantId());
                break;
            case BackGroundTaskCode.WATCH_BIZ_SET_RELATION:
                cmdbEventManager.watchBizSetRelationEvent(taskEntity.getTenantId());
                break;
            case BackGroundTaskCode.WATCH_HOST:
                cmdbEventManager.watchHostEvent(taskEntity.getTenantId());
                break;
            case BackGroundTaskCode.WATCH_HOST_RELATION:
                cmdbEventManager.watchHostRelationEvent(taskEntity.getTenantId());
                break;
            default:
                log.warn("task not supported: {}", taskEntity);
                break;
        }
        // 处理完任务后，立即执行一次负载均衡，及时关闭任务监听，避免接收到过多的任务
        backGroundTaskBalancer.balance();
    }
}
