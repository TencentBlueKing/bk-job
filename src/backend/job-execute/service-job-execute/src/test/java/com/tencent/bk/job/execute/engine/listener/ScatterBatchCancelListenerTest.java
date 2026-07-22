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

import com.tencent.bk.job.common.constant.RollingExecutionModeEnum;
import com.tencent.bk.job.common.constant.RollingModeEnum;
import com.tencent.bk.job.common.mq.metrics.MqConsumeDelayRecorder;
import com.tencent.bk.job.common.mq.metrics.MqConsumeDelaySimulator;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.engine.listener.event.ScatterBatchCancelEvent;
import com.tencent.bk.job.execute.engine.rolling.scatter.ScatterDispatchManager;
import com.tencent.bk.job.execute.engine.rolling.scatter.ScatterDispatchTask;
import com.tencent.bk.job.execute.engine.rolling.scatter.ScatterStepConverger;
import com.tencent.bk.job.execute.model.RollingConfigDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.db.ExecuteObjectRollingConfigDetailDO;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.rolling.RollingConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ScatterBatchCancelListener} 单元测试。
 * <p>
 * 覆盖广播即时取消：副本收到广播后对本地队列取消未下发批次并收敛为终止成功；
 * 未持有该步骤批次的副本取消 0 个而不做收敛（幂等）；非并行配置不收敛。
 */
class ScatterBatchCancelListenerTest {

    private static final long JOB_INSTANCE_ID = 100L;
    private static final long STEP_INSTANCE_ID = 200L;
    private static final long ROLLING_CONFIG_ID = 400L;
    private static final int EXECUTE_COUNT = 0;
    private static final int TOTAL_BATCH = 4;

    private StepInstanceService stepInstanceService;
    private RollingConfigService rollingConfigService;
    private ScatterDispatchManager scatterDispatchManager;
    private ScatterStepConverger scatterStepConverger;

    private ScatterBatchCancelListener listener;

    @BeforeEach
    void setUp() {
        stepInstanceService = mock(StepInstanceService.class);
        rollingConfigService = mock(RollingConfigService.class);
        scatterDispatchManager = mock(ScatterDispatchManager.class);
        scatterStepConverger = mock(ScatterStepConverger.class);
        MqConsumeDelayRecorder mqConsumeDelayRecorder = mock(MqConsumeDelayRecorder.class);
        MqConsumeDelaySimulator mqConsumeDelaySimulator = mock(MqConsumeDelaySimulator.class);

        listener = new ScatterBatchCancelListener(
            stepInstanceService,
            rollingConfigService,
            scatterDispatchManager,
            scatterStepConverger,
            mqConsumeDelayRecorder,
            mqConsumeDelaySimulator);
    }

    @Test
    @DisplayName("广播取消：持有队列副本取消到的未下发批次逐个被收敛为终止成功")
    void handleEvent_cancelAndConvergeHeldBatches() {
        StepInstanceDTO stepInstance = buildStepInstance();
        when(stepInstanceService.getStepInstanceDetail(JOB_INSTANCE_ID, STEP_INSTANCE_ID)).thenReturn(stepInstance);
        mockParallelRollingConfig();
        when(scatterDispatchManager.cancelStepTasks(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT))
            .thenReturn(Arrays.asList(
                new ScatterDispatchTask(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT, 3, 1000L),
                new ScatterDispatchTask(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT, 4, 2000L)));

        listener.handleEvent(buildMessage(
            ScatterBatchCancelEvent.cancel(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT)));

        verify(scatterDispatchManager).cancelStepTasks(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT);
        verify(scatterStepConverger).finishBatchAndConverge(
            eq(stepInstance), eq(EXECUTE_COUNT), eq(3), eq(RunStatusEnum.STOP_SUCCESS),
            anyLong(), eq(TOTAL_BATCH));
        verify(scatterStepConverger).finishBatchAndConverge(
            eq(stepInstance), eq(EXECUTE_COUNT), eq(4), eq(RunStatusEnum.STOP_SUCCESS),
            anyLong(), eq(TOTAL_BATCH));
        verify(scatterStepConverger, times(2)).finishBatchAndConverge(
            any(), anyInt(), anyInt(), any(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("广播取消：非持有副本取消 0 个则不查询步骤、不收敛（幂等）")
    void handleEvent_noConvergeWhenNothingCanceled() {
        when(scatterDispatchManager.cancelStepTasks(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT))
            .thenReturn(Collections.emptyList());

        listener.handleEvent(buildMessage(
            ScatterBatchCancelEvent.cancel(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT)));

        verify(scatterDispatchManager).cancelStepTasks(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT);
        verify(stepInstanceService, never()).getStepInstanceDetail(anyLong(), anyLong());
        verify(scatterStepConverger, never()).finishBatchAndConverge(
            any(), anyInt(), anyInt(), any(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("广播取消：非并行错峰配置时不收敛")
    void handleEvent_noConvergeWhenNotParallel() {
        StepInstanceDTO stepInstance = buildStepInstance();
        when(stepInstanceService.getStepInstanceDetail(JOB_INSTANCE_ID, STEP_INSTANCE_ID)).thenReturn(stepInstance);
        mockSerialRollingConfig();
        when(scatterDispatchManager.cancelStepTasks(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT))
            .thenReturn(Collections.singletonList(
                new ScatterDispatchTask(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT, 3, 1000L)));

        listener.handleEvent(buildMessage(
            ScatterBatchCancelEvent.cancel(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT)));

        verify(scatterStepConverger, never()).finishBatchAndConverge(
            any(), anyInt(), anyInt(), any(), anyLong(), anyInt());
    }

    private Message<ScatterBatchCancelEvent> buildMessage(ScatterBatchCancelEvent event) {
        return new GenericMessage<>(event);
    }

    private void mockParallelRollingConfig() {
        RollingConfigDTO rollingConfig = new RollingConfigDTO();
        ExecuteObjectRollingConfigDetailDO detail = new ExecuteObjectRollingConfigDetailDO();
        detail.setExecutionMode(RollingExecutionModeEnum.PARALLEL.getValue());
        detail.setMode(RollingModeEnum.PAUSE_IF_FAIL.getValue());
        detail.setTotalBatch(TOTAL_BATCH);
        rollingConfig.setExecuteObjectRollingConfig(detail);
        when(rollingConfigService.getRollingConfig(JOB_INSTANCE_ID, ROLLING_CONFIG_ID)).thenReturn(rollingConfig);
    }

    private void mockSerialRollingConfig() {
        RollingConfigDTO rollingConfig = new RollingConfigDTO();
        ExecuteObjectRollingConfigDetailDO detail = new ExecuteObjectRollingConfigDetailDO();
        detail.setExecutionMode(RollingExecutionModeEnum.SERIAL.getValue());
        detail.setMode(RollingModeEnum.PAUSE_IF_FAIL.getValue());
        detail.setTotalBatch(TOTAL_BATCH);
        rollingConfig.setExecuteObjectRollingConfig(detail);
        when(rollingConfigService.getRollingConfig(JOB_INSTANCE_ID, ROLLING_CONFIG_ID)).thenReturn(rollingConfig);
    }

    private StepInstanceDTO buildStepInstance() {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setId(STEP_INSTANCE_ID);
        stepInstance.setTaskInstanceId(JOB_INSTANCE_ID);
        stepInstance.setExecuteCount(EXECUTE_COUNT);
        stepInstance.setBatch(1);
        stepInstance.setRollingConfigId(ROLLING_CONFIG_ID);
        stepInstance.setExecuteType(StepExecuteTypeEnum.SEND_FILE);
        stepInstance.setStatus(RunStatusEnum.STOPPING);
        stepInstance.setStartTime(1000L);
        return stepInstance;
    }
}
