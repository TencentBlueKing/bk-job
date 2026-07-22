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

package com.tencent.bk.job.execute.engine.rolling.scatter;

import com.tencent.bk.job.common.constant.RollingExecutionModeEnum;
import com.tencent.bk.job.common.constant.RollingModeEnum;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.model.RollingConfigDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceRollingTaskDTO;
import com.tencent.bk.job.execute.model.db.ExecuteObjectRollingConfigDetailDO;
import com.tencent.bk.job.execute.service.FileExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceRollingTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.rolling.RollingConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ScatterBatchDispatcher} 单元测试。
 * <p>
 * 覆盖 Issue #4368 #3(B) 到点兜底收敛缺陷：整步终止落到非调度副本时，未下发批次到点仍会走到
 * {@code dispatchBatch}；此时不能只“跳过下发”，必须把该批置终态并参与完成判定，否则步骤永久卡 STOPPING。
 */
class ScatterBatchDispatcherTest {

    private static final long JOB_INSTANCE_ID = 100L;
    private static final long STEP_INSTANCE_ID = 200L;
    private static final long ROLLING_CONFIG_ID = 400L;
    private static final long GSE_TASK_ID = 300L;
    private static final int EXECUTE_COUNT = 0;
    private static final int TOTAL_BATCH = 4;

    private StepInstanceService stepInstanceService;
    private GseTaskService gseTaskService;
    private FileExecuteObjectTaskService fileExecuteObjectTaskService;
    private StepInstanceRollingTaskService stepInstanceRollingTaskService;
    private TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    private RollingConfigService rollingConfigService;
    private ScatterStepConverger scatterStepConverger;

    private ScatterBatchDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        stepInstanceService = mock(StepInstanceService.class);
        gseTaskService = mock(GseTaskService.class);
        ScriptExecuteObjectTaskService scriptExecuteObjectTaskService = mock(ScriptExecuteObjectTaskService.class);
        fileExecuteObjectTaskService = mock(FileExecuteObjectTaskService.class);
        stepInstanceRollingTaskService = mock(StepInstanceRollingTaskService.class);
        taskExecuteMQEventDispatcher = mock(TaskExecuteMQEventDispatcher.class);
        rollingConfigService = mock(RollingConfigService.class);
        scatterStepConverger = mock(ScatterStepConverger.class);

        dispatcher = new ScatterBatchDispatcher(
            stepInstanceService,
            gseTaskService,
            scriptExecuteObjectTaskService,
            fileExecuteObjectTaskService,
            stepInstanceRollingTaskService,
            taskExecuteMQEventDispatcher,
            rollingConfigService,
            scatterStepConverger);
    }

    @Test
    @DisplayName("到点兜底：步骤STOPPING时未下发批次被收敛为终止成功，且不发GSE下发事件")
    void dispatchBatch_convergeWhenStepStopping() {
        StepInstanceDTO stepInstance = buildStepInstance(RunStatusEnum.STOPPING);
        when(stepInstanceService.getStepInstanceDetail(JOB_INSTANCE_ID, STEP_INSTANCE_ID)).thenReturn(stepInstance);
        mockParallelRollingConfig();

        dispatcher.dispatchBatch(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT, 3, null);

        // 到点兜底收敛：以 STOP_SUCCESS 置终态并参与完成判定
        verify(scatterStepConverger).finishBatchAndConverge(
            eq(stepInstance), eq(EXECUTE_COUNT), eq(3), eq(RunStatusEnum.STOP_SUCCESS),
            anyLong(), eq(TOTAL_BATCH));
        // 终止语义下绝不再发 GSE 下发事件
        verify(taskExecuteMQEventDispatcher, never()).dispatchGseTaskEvent(any());
    }

    @Test
    @DisplayName("到点兜底：步骤已终态(STOP_SUCCESS)时未下发批次同样被收敛(幂等由DAO闸门保证)")
    void dispatchBatch_convergeWhenStepFinished() {
        StepInstanceDTO stepInstance = buildStepInstance(RunStatusEnum.STOP_SUCCESS);
        when(stepInstanceService.getStepInstanceDetail(JOB_INSTANCE_ID, STEP_INSTANCE_ID)).thenReturn(stepInstance);
        mockParallelRollingConfig();

        dispatcher.dispatchBatch(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT, 4, null);

        verify(scatterStepConverger).finishBatchAndConverge(
            eq(stepInstance), eq(EXECUTE_COUNT), eq(4), eq(RunStatusEnum.STOP_SUCCESS),
            anyLong(), eq(TOTAL_BATCH));
        verify(taskExecuteMQEventDispatcher, never()).dispatchGseTaskEvent(any());
    }

    @Test
    @DisplayName("到点兜底：执行次数已变更(重试)时不收敛旧批次，避免误操作旧执行次数数据")
    void dispatchBatch_notConvergeWhenExecuteCountChanged() {
        StepInstanceDTO stepInstance = buildStepInstance(RunStatusEnum.STOPPING);
        stepInstance.setExecuteCount(EXECUTE_COUNT + 1);
        when(stepInstanceService.getStepInstanceDetail(JOB_INSTANCE_ID, STEP_INSTANCE_ID)).thenReturn(stepInstance);

        dispatcher.dispatchBatch(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT, 3, null);

        verify(scatterStepConverger, never()).finishBatchAndConverge(
            any(), anyInt(), anyInt(), any(), anyLong(), anyInt());
        verify(taskExecuteMQEventDispatcher, never()).dispatchGseTaskEvent(any());
    }

    @Test
    @DisplayName("到点兜底：非并行错峰配置时不收敛(无该语义)")
    void dispatchBatch_notConvergeWhenNotParallel() {
        StepInstanceDTO stepInstance = buildStepInstance(RunStatusEnum.STOPPING);
        when(stepInstanceService.getStepInstanceDetail(JOB_INSTANCE_ID, STEP_INSTANCE_ID)).thenReturn(stepInstance);
        mockSerialRollingConfig();

        dispatcher.dispatchBatch(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT, 3, null);

        verify(scatterStepConverger, never()).finishBatchAndConverge(
            any(), anyInt(), anyInt(), any(), anyLong(), anyInt());
        verify(taskExecuteMQEventDispatcher, never()).dispatchGseTaskEvent(any());
    }

    @Test
    @DisplayName("正常下发：步骤RUNNING且未下发时创建GSE任务并发下发事件，不走收敛")
    void dispatchBatch_normalDispatchWhenRunning() {
        StepInstanceDTO stepInstance = buildStepInstance(RunStatusEnum.RUNNING);
        when(stepInstanceService.getStepInstanceDetail(JOB_INSTANCE_ID, STEP_INSTANCE_ID)).thenReturn(stepInstance);
        StepInstanceRollingTaskDTO rollingTask = new StepInstanceRollingTaskDTO();
        rollingTask.setBatch(2);
        rollingTask.setDispatched(Boolean.FALSE);
        when(stepInstanceRollingTaskService.queryRollingTask(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT, 2))
            .thenReturn(rollingTask);
        when(gseTaskService.saveGseTask(any())).thenReturn(GSE_TASK_ID);

        dispatcher.dispatchBatch(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT, 2, null);

        verify(taskExecuteMQEventDispatcher).dispatchGseTaskEvent(any());
        verify(scatterStepConverger, never()).finishBatchAndConverge(
            any(), anyInt(), anyInt(), any(), anyLong(), anyInt());
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

    private StepInstanceDTO buildStepInstance(RunStatusEnum status) {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setId(STEP_INSTANCE_ID);
        stepInstance.setTaskInstanceId(JOB_INSTANCE_ID);
        stepInstance.setExecuteCount(EXECUTE_COUNT);
        stepInstance.setBatch(1);
        stepInstance.setRollingConfigId(ROLLING_CONFIG_ID);
        stepInstance.setExecuteType(StepExecuteTypeEnum.SEND_FILE);
        stepInstance.setStatus(status);
        stepInstance.setStartTime(1000L);
        return stepInstance;
    }
}
