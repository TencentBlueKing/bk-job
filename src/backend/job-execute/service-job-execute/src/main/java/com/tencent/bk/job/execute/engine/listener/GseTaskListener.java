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

package com.tencent.bk.job.execute.engine.listener;

import com.tencent.bk.job.execute.common.exception.MessageHandleException;
import com.tencent.bk.job.execute.common.exception.MessageHandlerUnavailableException;
import com.tencent.bk.job.execute.engine.GseTaskManager;
import com.tencent.bk.job.execute.engine.consts.GseTaskActionEnum;
import com.tencent.bk.job.execute.engine.exception.ExceptionStatusManager;
import com.tencent.bk.job.execute.engine.listener.event.GseTaskEvent;
import com.tencent.bk.job.execute.engine.message.GseTaskProcessor;
import com.tencent.bk.job.execute.monitor.metrics.GseTasksExceptionCounter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * 执行引擎事件处理-GSE任务
 */
@Component
@EnableBinding({GseTaskProcessor.class})
@Slf4j
public class GseTaskListener {
    private final GseTaskManager gseTaskManager;
    private final ExceptionStatusManager exceptionStatusManager;
    /**
     * 任务执行异常Counter
     */
    private final GseTasksExceptionCounter gseTasksExceptionCounter;

    @Autowired
    public GseTaskListener(GseTaskManager gseTaskManager,
                           ExceptionStatusManager exceptionStatusManager,
                           GseTasksExceptionCounter gseTasksExceptionCounter) {
        this.gseTaskManager = gseTaskManager;
        this.exceptionStatusManager = exceptionStatusManager;
        this.gseTasksExceptionCounter = gseTasksExceptionCounter;
    }

    /**
     * 处理GSE任务相关的事件
     *
     * @param gseTaskEvent GSE任务事件
     */
    @StreamListener(GseTaskProcessor.INPUT)
    public void handleEvent(@Payload GseTaskEvent gseTaskEvent) {
        log.info("Handel gse task event: {}", gseTaskEvent);
        long stepInstanceId = gseTaskEvent.getStepInstanceId();
        String requestId = gseTaskEvent.getRequestId();
        try {
            int action = gseTaskEvent.getAction();
            if (GseTaskActionEnum.START.getValue() == action) {
                gseTaskManager.startTask(stepInstanceId, gseTaskEvent.getRequestId());
            } else if (GseTaskActionEnum.STOP.getValue() == action) {
                gseTaskManager.stopTask(stepInstanceId, requestId);
            } else {
                log.error("Error gse task action:{}", action);
            }
        } catch (Throwable e) {
            String errorMsg = "Handling gse task event error,stepInstanceId:" + stepInstanceId;
            log.error(errorMsg, e);
            handleException(stepInstanceId, e);
        }
    }

    private void handleException(long stepInstanceId, Throwable e) throws MessageHandleException {
        // 服务关闭，消息被拒绝，重新入队列
        if (e instanceof MessageHandlerUnavailableException) {
            throw (MessageHandlerUnavailableException) e;
        }
        gseTasksExceptionCounter.increment();
        // 任务状态应当置为异常状态
        exceptionStatusManager.setAbnormalStatusForStep(stepInstanceId);
    }
}
