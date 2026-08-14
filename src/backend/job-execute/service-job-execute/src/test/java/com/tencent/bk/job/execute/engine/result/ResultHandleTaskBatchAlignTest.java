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

package com.tencent.bk.job.execute.engine.result;

import com.tencent.bk.job.common.gse.v2.model.ExecuteObjectGseKey;
import com.tencent.bk.job.execute.engine.EngineDependentServiceHolder;
import com.tencent.bk.job.execute.engine.model.GseLogBatchPullResult;
import com.tencent.bk.job.execute.engine.model.GseTaskExecuteResult;
import com.tencent.bk.job.execute.engine.model.GseTaskResult;
import com.tencent.bk.job.execute.engine.model.TaskVariablesAnalyzeResult;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.ExecuteObjectTaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * {@link AbstractResultHandleTask} 批次对齐回归测试。
 * <p>
 * 覆盖 Issue #4368 并行错峰模式（executionMode=2）日志缺失缺陷：
 * 结果处理任务写日志依据 {@code stepInstance.getBatch()}，而并行模式下 step_instance.batch
 * 恒为初始批次(1)、不随各并发批次推进，导致 batch 2/3/4 的日志被写到 batch=1、按各自批次查不到。
 * 修复：构造结果处理任务时按其绑定的 GSE 任务批次校正 stepInstance，保证多批日志按各自 batch 写入且可查。
 */
class ResultHandleTaskBatchAlignTest {

    private static final long JOB_INSTANCE_ID = 100L;
    private static final long STEP_INSTANCE_ID = 200L;
    private static final int EXECUTE_COUNT = 0;

    @Test
    @DisplayName("并行错峰：stepInstance.batch 恒为1时，结果处理任务按GSE任务批次校正 stepInstance")
    void parallelBatch_alignStepInstanceBatchToGseTaskBatch() {
        // 并行模式下 step_instance.batch 恒为初始批次 1
        StepInstanceDTO stepInstance = buildStepInstance(1);
        // 第 3 批的 GSE 任务
        GseTaskDTO gseTask = buildGseTask(3);

        newResultHandleTask(stepInstance, gseTask);

        // 结果日志（addFileTaskLog 依据 stepInstance.getBatch()）应写到批次 3，而非恒为 1
        assertThat(stepInstance.getBatch()).isEqualTo(3);
    }

    @Test
    @DisplayName("串行/一致场景：批次已一致时对齐为无副作用")
    void consistentBatch_noSideEffect() {
        StepInstanceDTO stepInstance = buildStepInstance(2);
        GseTaskDTO gseTask = buildGseTask(2);

        newResultHandleTask(stepInstance, gseTask);

        assertThat(stepInstance.getBatch()).isEqualTo(2);
    }

    private TestResultHandleTask newResultHandleTask(StepInstanceDTO stepInstance, GseTaskDTO gseTask) {
        EngineDependentServiceHolder holder = mock(EngineDependentServiceHolder.class, RETURNS_DEEP_STUBS);
        ExecuteObjectTaskService executeObjectTaskService = mock(ExecuteObjectTaskService.class);
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setId(JOB_INSTANCE_ID);
        taskInstance.setAppId(1L);
        taskInstance.setPlanId(0L);
        Map<ExecuteObjectGseKey, ExecuteObjectTask> targetTasks = new HashMap<>();
        return new TestResultHandleTask(
            holder,
            executeObjectTaskService,
            taskInstance,
            stepInstance,
            new TaskVariablesAnalyzeResult(Collections.emptyList()),
            targetTasks,
            gseTask,
            "req-1",
            Collections.emptyList());
    }

    private StepInstanceDTO buildStepInstance(int batch) {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setId(STEP_INSTANCE_ID);
        stepInstance.setTaskInstanceId(JOB_INSTANCE_ID);
        stepInstance.setAppId(1L);
        stepInstance.setExecuteCount(EXECUTE_COUNT);
        stepInstance.setBatch(batch);
        return stepInstance;
    }

    private GseTaskDTO buildGseTask(int batch) {
        GseTaskDTO gseTask = new GseTaskDTO();
        gseTask.setStepInstanceId(STEP_INSTANCE_ID);
        gseTask.setExecuteCount(EXECUTE_COUNT);
        gseTask.setBatch(batch);
        return gseTask;
    }

    /**
     * 最小化具体子类，仅用于验证 {@link AbstractResultHandleTask} 构造期的批次对齐逻辑。
     */
    private static class TestResultHandleTask extends AbstractResultHandleTask<Object> {

        TestResultHandleTask(EngineDependentServiceHolder engineDependentServiceHolder,
                             ExecuteObjectTaskService executeObjectTaskService,
                             TaskInstanceDTO taskInstance,
                             StepInstanceDTO stepInstance,
                             TaskVariablesAnalyzeResult taskVariablesAnalyzeResult,
                             Map<ExecuteObjectGseKey, ExecuteObjectTask> targetExecuteObjectTasks,
                             GseTaskDTO gseTask,
                             String requestId,
                             java.util.List<ExecuteObjectTask> executeObjectTasks) {
            super(engineDependentServiceHolder, executeObjectTaskService, taskInstance, stepInstance,
                taskVariablesAnalyzeResult, targetExecuteObjectTasks, gseTask, requestId, executeObjectTasks);
        }

        @Override
        GseLogBatchPullResult<Object> pullGseTaskResultInBatches() {
            return null;
        }

        @Override
        GseTaskExecuteResult analyseGseTaskResult(GseTaskResult<Object> gseTaskResult) {
            return null;
        }

        @Override
        public ScheduleStrategy getScheduleStrategy() {
            return null;
        }

        @Override
        public boolean isFinished() {
            return false;
        }
    }
}
