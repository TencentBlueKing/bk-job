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

import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.prepare.FilePrepareService;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceRollingTaskDTO;
import com.tencent.bk.job.execute.service.StepInstanceRollingTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ScatterStepConverger} 单元测试。
 * <p>
 * 覆盖 Issue #4368 的两点：
 * <ul>
 *     <li>#2 状态聚合：异常(ABNORMAL_STATE/ABANDONED) 优先于失败(FAIL)，与串行 finishStepWithAbnormalState 对齐；</li>
 *     <li>#3 收敛：最后一批到达终态时唯一收敛步骤，非最后一批不改步骤状态，refreshJob 按开关发送。</li>
 * </ul>
 */
class ScatterStepConvergerTest {

    private static final long JOB_INSTANCE_ID = 100L;
    private static final long STEP_INSTANCE_ID = 200L;
    private static final int EXECUTE_COUNT = 0;

    private StepInstanceService stepInstanceService;
    private StepInstanceRollingTaskService stepInstanceRollingTaskService;
    private TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    private FilePrepareService filePrepareService;
    private ScatterStepConverger converger;

    @BeforeEach
    void setUp() {
        stepInstanceService = mock(StepInstanceService.class);
        stepInstanceRollingTaskService = mock(StepInstanceRollingTaskService.class);
        taskExecuteMQEventDispatcher = mock(TaskExecuteMQEventDispatcher.class);
        filePrepareService = mock(FilePrepareService.class);
        converger = new ScatterStepConverger(stepInstanceService, stepInstanceRollingTaskService,
            taskExecuteMQEventDispatcher, filePrepareService);
    }

    @Test
    @DisplayName("聚合优先级：有异常有失败→步骤异常(ABNORMAL_STATE)")
    void aggregate_abnormalTakesPriorityOverFail() {
        mockRollingTasks(
            buildRollingTask(1, RunStatusEnum.FAIL),
            buildRollingTask(2, RunStatusEnum.ABNORMAL_STATE),
            buildRollingTask(3, RunStatusEnum.SUCCESS));
        assertThat(converger.aggregateParallelStepStatus(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT))
            .isEqualTo(RunStatusEnum.ABNORMAL_STATE);
    }

    @Test
    @DisplayName("聚合优先级：ABANDONED 与异常同级，优先于失败→步骤异常")
    void aggregate_abandonedTakesPriorityOverFail() {
        mockRollingTasks(
            buildRollingTask(1, RunStatusEnum.FAIL),
            buildRollingTask(2, RunStatusEnum.ABANDONED));
        assertThat(converger.aggregateParallelStepStatus(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT))
            .isEqualTo(RunStatusEnum.ABNORMAL_STATE);
    }

    @Test
    @DisplayName("聚合优先级：失败优先于终止成功→失败")
    void aggregate_failTakesPriorityOverStopSuccess() {
        mockRollingTasks(
            buildRollingTask(1, RunStatusEnum.STOP_SUCCESS),
            buildRollingTask(2, RunStatusEnum.FAIL));
        assertThat(converger.aggregateParallelStepStatus(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT))
            .isEqualTo(RunStatusEnum.FAIL);
    }

    @Test
    @DisplayName("聚合优先级：终止成功优先于忽略错误→终止成功")
    void aggregate_stopSuccessTakesPriorityOverIgnoreError() {
        mockRollingTasks(
            buildRollingTask(1, RunStatusEnum.IGNORE_ERROR),
            buildRollingTask(2, RunStatusEnum.STOP_SUCCESS));
        assertThat(converger.aggregateParallelStepStatus(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT))
            .isEqualTo(RunStatusEnum.STOP_SUCCESS);
    }

    @Test
    @DisplayName("聚合优先级：仅忽略错误→忽略错误")
    void aggregate_ignoreErrorOnly() {
        mockRollingTasks(
            buildRollingTask(1, RunStatusEnum.SUCCESS),
            buildRollingTask(2, RunStatusEnum.IGNORE_ERROR));
        assertThat(converger.aggregateParallelStepStatus(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT))
            .isEqualTo(RunStatusEnum.IGNORE_ERROR);
    }

    @Test
    @DisplayName("聚合优先级：全部成功→成功")
    void aggregate_allSuccess() {
        mockRollingTasks(
            buildRollingTask(1, RunStatusEnum.SUCCESS),
            buildRollingTask(2, RunStatusEnum.SUCCESS));
        assertThat(converger.aggregateParallelStepStatus(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT))
            .isEqualTo(RunStatusEnum.SUCCESS);
    }

    @Test
    @DisplayName("聚合：仅统计目标执行次数的批次，历史执行次数被忽略")
    void aggregate_onlyCountCurrentExecuteCount() {
        StepInstanceRollingTaskDTO staleFail = buildRollingTask(1, RunStatusEnum.FAIL);
        staleFail.setExecuteCount(EXECUTE_COUNT + 1);
        mockRollingTasks(
            staleFail,
            buildRollingTask(2, RunStatusEnum.SUCCESS));
        assertThat(converger.aggregateParallelStepStatus(JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT))
            .isEqualTo(RunStatusEnum.SUCCESS);
    }

    @Test
    @DisplayName("收敛：最后一批终态时，收敛步骤并发送refreshJob")
    void converge_lastBatchWithRefreshJob() {
        StepInstanceDTO stepInstance = buildStepInstance();
        when(stepInstanceRollingTaskService.finishBatchAndCheckAllDone(
            any(), anyLong(), anyInt(), anyInt(), any(), any(), any(), any(), anyInt()))
            .thenReturn(ScatterBatchFinishResult.LAST_BATCH);
        mockRollingTasks(
            buildRollingTask(1, RunStatusEnum.STOP_SUCCESS),
            buildRollingTask(2, RunStatusEnum.STOP_SUCCESS));

        ScatterBatchFinishResult result = converger.finishBatchAndConverge(
            stepInstance, EXECUTE_COUNT, 2, RunStatusEnum.STOP_SUCCESS, 1000L, 2);

        assertThat(result).isEqualTo(ScatterBatchFinishResult.LAST_BATCH);
        verify(stepInstanceService).updateStepExecutionInfo(
            eq(JOB_INSTANCE_ID), eq(STEP_INSTANCE_ID), eq(RunStatusEnum.STOP_SUCCESS), any(), any(), any());
        verify(taskExecuteMQEventDispatcher).dispatchJobEvent(any());
    }

    @Test
    @DisplayName("收敛：非最后一批不改步骤状态、不发refreshJob")
    void converge_notLastBatch() {
        StepInstanceDTO stepInstance = buildStepInstance();
        when(stepInstanceRollingTaskService.finishBatchAndCheckAllDone(
            any(), anyLong(), anyInt(), anyInt(), any(), any(), any(), any(), anyInt()))
            .thenReturn(ScatterBatchFinishResult.NOT_LAST_BATCH);

        ScatterBatchFinishResult result = converger.finishBatchAndConverge(
            stepInstance, EXECUTE_COUNT, 1, RunStatusEnum.STOP_SUCCESS, 1000L, 3);

        assertThat(result).isEqualTo(ScatterBatchFinishResult.NOT_LAST_BATCH);
        verify(stepInstanceService, never()).updateStepExecutionInfo(
            anyLong(), anyLong(), any(), any(), any(), any());
        verify(taskExecuteMQEventDispatcher, never()).dispatchJobEvent(any());
    }

    @Test
    @DisplayName("收敛：最后一批成功时，收敛步骤、清理文件并发送refreshJob")
    void converge_lastBatchSuccessClearsFileAndRefreshJob() {
        StepInstanceDTO stepInstance = buildStepInstance();
        when(stepInstanceRollingTaskService.finishBatchAndCheckAllDone(
            any(), anyLong(), anyInt(), anyInt(), any(), any(), any(), any(), anyInt()))
            .thenReturn(ScatterBatchFinishResult.LAST_BATCH);
        mockRollingTasks(
            buildRollingTask(1, RunStatusEnum.SUCCESS),
            buildRollingTask(2, RunStatusEnum.SUCCESS));

        converger.finishBatchAndConverge(
            stepInstance, EXECUTE_COUNT, 2, RunStatusEnum.SUCCESS, 1000L, 2);

        verify(stepInstanceService).updateStepExecutionInfo(
            eq(JOB_INSTANCE_ID), eq(STEP_INSTANCE_ID), eq(RunStatusEnum.SUCCESS), any(), any(), any());
        verify(filePrepareService).clearPreparedTmpFile(JOB_INSTANCE_ID, STEP_INSTANCE_ID);
        verify(taskExecuteMQEventDispatcher).dispatchJobEvent(any());
    }

    private void mockRollingTasks(StepInstanceRollingTaskDTO... tasks) {
        when(stepInstanceRollingTaskService.listRollingTasksByStep(JOB_INSTANCE_ID, STEP_INSTANCE_ID))
            .thenReturn(Arrays.asList(tasks));
    }

    private StepInstanceRollingTaskDTO buildRollingTask(int batch, RunStatusEnum status) {
        StepInstanceRollingTaskDTO task = new StepInstanceRollingTaskDTO();
        task.setTaskInstanceId(JOB_INSTANCE_ID);
        task.setStepInstanceId(STEP_INSTANCE_ID);
        task.setExecuteCount(EXECUTE_COUNT);
        task.setBatch(batch);
        task.setStatus(status);
        return task;
    }

    private StepInstanceDTO buildStepInstance() {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setId(STEP_INSTANCE_ID);
        stepInstance.setTaskInstanceId(JOB_INSTANCE_ID);
        stepInstance.setExecuteCount(EXECUTE_COUNT);
        stepInstance.setBatch(1);
        stepInstance.setExecuteType(StepExecuteTypeEnum.SEND_FILE);
        stepInstance.setStatus(RunStatusEnum.RUNNING);
        stepInstance.setStartTime(1000L);
        return stepInstance;
    }
}
