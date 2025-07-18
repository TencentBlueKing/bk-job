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

package com.tencent.bk.job.execute.engine.listener;

import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.engine.listener.event.JobMessage;
import com.tencent.bk.job.execute.engine.listener.event.StepEvent;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.service.StepInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * 执行引擎事件处理-步骤
 */
@Component
@Slf4j
public class StepListener extends BaseJobMqListener {
    private final StepInstanceService stepInstanceService;
    private final GseStepEventHandler gseStepEventHandler;
    private final ConfirmStepEventHandler confirmStepEventHandler;

    @Autowired
    public StepListener(StepInstanceService stepInstanceService,
                        GseStepEventHandler gseStepEventHandler,
                        ConfirmStepEventHandler confirmStepEventHandler) {
        this.stepInstanceService = stepInstanceService;
        this.gseStepEventHandler = gseStepEventHandler;
        this.confirmStepEventHandler = confirmStepEventHandler;
    }

    /**
     * 处理步骤执行相关的事件
     *
     * @param message 消息
     */
    @Override
    public void handleEvent(Message<? extends JobMessage> message) {
        StepEvent stepEvent = (StepEvent) message.getPayload();
        log.info("Handle step event: {}, duration: {}ms", stepEvent, stepEvent.duration());
        long stepInstanceId = stepEvent.getStepInstanceId();
        try {
            StepInstanceDTO stepInstance = stepInstanceService.getStepInstanceDetail(
                stepEvent.getJobInstanceId(), stepInstanceId);
            dispatchEvent(stepEvent, stepInstance);
        } catch (Throwable e) {
            String errorMsg = "Handling step event error,stepInstanceId:" + stepInstanceId;
            log.error(errorMsg, e);
        }

    }

    private void dispatchEvent(StepEvent stepEvent, StepInstanceDTO stepInstance) {
        StepExecuteTypeEnum stepType = stepInstance.getExecuteType();

        switch (stepType) {
            case EXECUTE_SCRIPT:
            case SEND_FILE:
            case EXECUTE_SQL:
                gseStepEventHandler.handleEvent(stepEvent, stepInstance);
                break;
            case MANUAL_CONFIRM:
                confirmStepEventHandler.handleEvent(stepEvent, stepInstance);
                break;
            default:
                log.error("Unhandled step event: {}", stepEvent);
        }
    }


}
