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
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.engine.listener.event.JobMessage;
import com.tencent.bk.job.execute.engine.listener.event.ScatterBatchCancelEvent;
import com.tencent.bk.job.execute.engine.rolling.scatter.ScatterDispatchManager;
import com.tencent.bk.job.execute.engine.rolling.scatter.ScatterDispatchTask;
import com.tencent.bk.job.execute.engine.rolling.scatter.ScatterStepConverger;
import com.tencent.bk.job.execute.model.RollingConfigDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.rolling.RollingConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 执行引擎事件处理-并行错峰批次取消广播（广播即时取消）。
 * <p>
 * 并行错峰整步终止时，终止副本广播 {@link ScatterBatchCancelEvent} 到所有副本（fanout，含自身）。
 * 每个副本收到后对本地 {@link ScatterDispatchManager} 延迟队列执行取消，并把取消到的未下发批次经
 * {@link ScatterStepConverger} 收敛为终止成功（{@link RunStatusEnum#STOP_SUCCESS}）。
 * <p>
 * 幂等/正确性：
 * <ul>
 *     <li>某步骤的未下发批次仅存在于唯一持有副本的本地队列，故仅该副本会真正取消并收敛，其余副本取消 0 个；</li>
 *     <li>收敛复用 DAO 终态幂等闸门（status NOT IN 终态集），与到点兜底/重复回调并发安全，不会重复收敛；</li>
 *     <li>仅取消匹配 executeCount 的批次，不影响重试后新一轮（executeCount 递增）的批次；</li>
 *     <li>广播仅作即时性优化，即使丢失，未取消批次仍会由 ScatterBatchDispatcher 到点兜底收敛，正确性不依赖广播。</li>
 * </ul>
 */
@Component
@Slf4j
public class ScatterBatchCancelListener extends BaseJobMqListener {

    private final StepInstanceService stepInstanceService;
    private final RollingConfigService rollingConfigService;
    private final ScatterDispatchManager scatterDispatchManager;
    private final ScatterStepConverger scatterStepConverger;

    @Autowired
    public ScatterBatchCancelListener(StepInstanceService stepInstanceService,
                                      RollingConfigService rollingConfigService,
                                      ScatterDispatchManager scatterDispatchManager,
                                      ScatterStepConverger scatterStepConverger,
                                      MqConsumeDelayRecorder mqConsumeDelayRecorder,
                                      MqConsumeDelaySimulator mqConsumeDelaySimulator) {
        super(mqConsumeDelayRecorder, mqConsumeDelaySimulator);
        this.stepInstanceService = stepInstanceService;
        this.rollingConfigService = rollingConfigService;
        this.scatterDispatchManager = scatterDispatchManager;
        this.scatterStepConverger = scatterStepConverger;
    }

    @Override
    protected String getBindingName() {
        return MqBindingNames.HANDLE_SCATTER_BATCH_CANCEL_FANOUT_EVENT;
    }

    @Override
    public void handleEvent(Message<? extends JobMessage> message) {
        ScatterBatchCancelEvent event = (ScatterBatchCancelEvent) message.getPayload();
        log.info("Receive scatter batch cancel event: {}, duration: {}ms", event, event.duration());
        try {
            cancelAndConvergeLocalBatches(event);
        } catch (Exception e) {
            log.error("Handle scatter batch cancel event error, event=" + event, e);
        }
    }

    /**
     * 取消本副本延迟队列中该步骤的未下发批次，并将取消到的批次收敛为终止成功。
     */
    private void cancelAndConvergeLocalBatches(ScatterBatchCancelEvent event) {
        Long taskInstanceId = event.getTaskInstanceId();
        long stepInstanceId = event.getStepInstanceId();
        int executeCount = event.getExecuteCount();

        List<ScatterDispatchTask> canceled = scatterDispatchManager.cancelStepTasks(
            taskInstanceId, stepInstanceId, executeCount);
        if (canceled.isEmpty()) {
            log.info("No un-dispatched scatter batch canceled on this replica by broadcast, "
                + "stepInstanceId={}, executeCount={}", stepInstanceId, executeCount);
            return;
        }

        StepInstanceDTO stepInstance = stepInstanceService.getStepInstanceDetail(taskInstanceId, stepInstanceId);
        if (stepInstance == null) {
            log.warn("Scatter batch cancel skip converge, step instance not found, stepInstanceId={}", stepInstanceId);
            return;
        }
        RollingConfigDTO rollingConfig = rollingConfigService.getRollingConfig(
            taskInstanceId, stepInstance.getRollingConfigId());
        if (rollingConfig == null || !rollingConfig.isParallelExecution()) {
            log.info("Scatter batch cancel skip converge, not parallel scatter. stepInstanceId={}", stepInstanceId);
            return;
        }
        int totalBatch = rollingConfig.getExecuteObjectRollingConfig().getTotalBatch();
        long now = System.currentTimeMillis();
        for (ScatterDispatchTask task : canceled) {
            scatterStepConverger.finishBatchAndConverge(stepInstance, executeCount, task.getBatch(),
                RunStatusEnum.STOP_SUCCESS, now, totalBatch);
        }
        log.info("Converge canceled scatter batches by broadcast, stepInstanceId={}, executeCount={}, canceled={}",
            stepInstanceId, executeCount, canceled.size());
    }
}
