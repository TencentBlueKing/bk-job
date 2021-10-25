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
import com.tencent.bk.job.execute.engine.consts.GseStepActionEnum;
import com.tencent.bk.job.execute.engine.exception.ExceptionStatusManager;
import com.tencent.bk.job.execute.engine.message.GseTaskProcessor;
import com.tencent.bk.job.execute.engine.model.StepControlMessage;
import com.tencent.bk.job.execute.monitor.metrics.GseTasksExceptionCounter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * 执行引擎流程处理-GSE任务
 */
@Component
@EnableBinding({GseTaskProcessor.class})
@Slf4j
public class GseStepListener {
    private final GseTaskManager gseTaskManager;
    private final ExceptionStatusManager exceptionStatusManager;
    /**
     * 任务执行异常Counter
     */
    private final GseTasksExceptionCounter gseTasksExceptionCounter;

    @Autowired
    public GseStepListener(
        GseTaskManager gseTaskManager,
        ExceptionStatusManager exceptionStatusManager,
        GseTasksExceptionCounter gseTasksExceptionCounter
    ) {
        this.gseTaskManager = gseTaskManager;
        this.exceptionStatusManager = exceptionStatusManager;
        this.gseTasksExceptionCounter = gseTasksExceptionCounter;
    }

    @StreamListener(GseTaskProcessor.INPUT)
    public void handleMessage(@Payload StepControlMessage gseStepControlMessage) {
        log.info("Receive gse step control message, stepInstanceId={}, action={}, requestId={}, msgSendTime={}",
            gseStepControlMessage.getStepInstanceId(),
            gseStepControlMessage.getAction(), gseStepControlMessage.getRequestId(), gseStepControlMessage.getTime());
        long stepInstanceId = gseStepControlMessage.getStepInstanceId();
        String requestId = gseStepControlMessage.getRequestId();
        try {
            int action = gseStepControlMessage.getAction();
            if (GseStepActionEnum.START.getValue() == action) {
                gseTaskManager.startStep(stepInstanceId, gseStepControlMessage.getRequestId());
            } else if (GseStepActionEnum.STOP.getValue() == action) {
                gseTaskManager.stopStep(stepInstanceId, requestId);
            } else if (GseStepActionEnum.RETRY_FAIL.getValue() == action) {
                gseTaskManager.retryFail(stepInstanceId, requestId);
            } else if (GseStepActionEnum.RETRY_ALL.getValue() == action) {
                gseTaskManager.retryAll(stepInstanceId, requestId);
            } else {
                log.error("Error gse step control action:{}", action);
            }
        } catch (Throwable e) {
            String errorMsg = "Handling gse step control message error,stepInstanceId:" + stepInstanceId;
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
