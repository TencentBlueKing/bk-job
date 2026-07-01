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

import com.tencent.bk.job.common.mq.metrics.MqConsumeDelayRecorder;
import com.tencent.bk.job.common.mq.metrics.MqConsumeDelaySimulator;
import com.tencent.bk.job.execute.engine.listener.event.JobMessage;
import com.tencent.bk.job.execute.engine.listener.event.RollingBatchDispatchResumeEvent;
import com.tencent.bk.job.execute.engine.rolling.scatter.ScatterDispatchManager;
import com.tencent.bk.job.execute.engine.rolling.scatter.ScatterDispatchTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

/**
 * 执行引擎事件处理-滚动批次并行错峰下发恢复。
 * <p>
 * 停机实例把未下发批次通过 MQ 转移，本实例竞争消费后按剩余延时 {@code max(0, dispatch_time-now)} 重新入队。
 */
@Component
@Slf4j
public class RollingBatchDispatchResumeListener extends BaseJobMqListener {

    private final ScatterDispatchManager scatterDispatchManager;

    @Autowired
    public RollingBatchDispatchResumeListener(ScatterDispatchManager scatterDispatchManager,
                                              MqConsumeDelayRecorder mqConsumeDelayRecorder,
                                              MqConsumeDelaySimulator mqConsumeDelaySimulator) {
        super(mqConsumeDelayRecorder, mqConsumeDelaySimulator);
        this.scatterDispatchManager = scatterDispatchManager;
    }

    @Override
    protected String getBindingName() {
        return MqBindingNames.HANDLE_ROLLING_BATCH_DISPATCH_RESUME_EVENT;
    }

    @Override
    public void handleEvent(Message<? extends JobMessage> message) {
        RollingBatchDispatchResumeEvent event = (RollingBatchDispatchResumeEvent) message.getPayload();
        log.info("Receive rolling batch dispatch resume event: {}, duration: {}ms", event, event.duration());
        try {
            // 转移后按原计划下发时刻重新入队，剩余延时由 DelayQueue 依据 dispatchTime 自动计算
            ScatterDispatchTask task = new ScatterDispatchTask(
                event.getJobInstanceId(),
                event.getStepInstanceId(),
                event.getExecuteCount(),
                event.getBatch(),
                event.getDispatchTime() == null ? System.currentTimeMillis() : event.getDispatchTime(),
                event.getGseTaskId()
            );
            scatterDispatchManager.addTask(task);
        } catch (Exception e) {
            log.error("Handle rolling batch dispatch resume event error, event=" + event, e);
        }
    }
}
