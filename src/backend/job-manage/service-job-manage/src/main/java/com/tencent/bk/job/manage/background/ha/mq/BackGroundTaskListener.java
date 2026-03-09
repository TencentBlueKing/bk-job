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
    /**
     * CMDB事件管理器
     */
    private final CmdbEventManager cmdbEventManager;
    /**
     * 后台任务负载均衡器
     */
    private final BackGroundTaskBalancer backGroundTaskBalancer;

    @Autowired
    public BackGroundTaskListener(CmdbEventManager cmdbEventManager,
                                  BackGroundTaskBalancer backGroundTaskBalancer) {
        this.cmdbEventManager = cmdbEventManager;
        this.backGroundTaskBalancer = backGroundTaskBalancer;
    }

    /**
     * 尝试对从MQ接收到的任务消息进行处理，异常则打印错误日志
     *
     * @param taskEntityMessage 任务实体MQ信息
     */
    public void handleTask(Message<TaskEntity> taskEntityMessage) {
        try {
            doHandleTask(taskEntityMessage);
        } catch (Throwable t) {
            log.error("Fail to handleTask", t);
        }
    }

    /**
     * 从MQ接收到任务消息后对其进行处理（启动事件监听）
     *
     * @param taskEntityMessage 任务实体MQ信息
     */
    private void doHandleTask(Message<TaskEntity> taskEntityMessage) {
        TaskEntity taskEntity = taskEntityMessage.getPayload();
        log.info("Received task from queue: {}", taskEntity);
        String tenantId = taskEntity.getTenantId();
        Boolean watchResult = null;
        switch (taskEntity.getTaskType()) {
            case WATCH_BIZ:
                watchResult = cmdbEventManager.startWatchBizEvent(tenantId);
                break;
            case WATCH_BIZ_SET:
                watchResult = cmdbEventManager.startWatchBizSetEvent(tenantId);
                break;
            case WATCH_BIZ_SET_RELATION:
                watchResult = cmdbEventManager.startWatchBizSetRelationEvent(tenantId);
                break;
            case WATCH_HOST:
                watchResult = cmdbEventManager.startWatchHostEvent(tenantId);
                break;
            case WATCH_HOST_RELATION:
                watchResult = cmdbEventManager.startWatchHostRelationEvent(tenantId);
                break;
            default:
                log.warn("task not supported: {}", taskEntity);
                break;
        }
        log.info("task={}, watchResult={}", taskEntity.getUniqueCode(), watchResult);
        // 处理完任务后，立即执行一次负载均衡，及时关闭任务监听，避免接收到过多的任务
        backGroundTaskBalancer.balance();
    }
}
