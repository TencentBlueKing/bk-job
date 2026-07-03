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

package com.tencent.bk.job.execute.engine.executor;

import com.tencent.bk.job.execute.engine.EngineDependentServiceHolder;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.TaskInstanceDTO;
import com.tencent.bk.job.execute.service.ExecuteObjectTaskService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

/**
 * {@link AbstractGseTaskCommand} 批次对齐回归测试。
 * <p>
 * 覆盖 Issue #4368 并行错峰模式（executionMode=2）日志缺失缺陷：
 * GSE 任务命令写初始日志（saveInitialFileTaskLogs）依据 {@code stepInstance.getBatch()}，
 * 而并行模式下 step_instance.batch 恒为初始批次(1)，导致 2/3/4 批初始日志被写到 batch=1、按各自批次查不到。
 * 修复：构造命令时按其绑定的 GSE 任务批次校正 stepInstance。
 */
class GseTaskCommandBatchAlignTest {

    private static final long JOB_INSTANCE_ID = 100L;
    private static final long STEP_INSTANCE_ID = 200L;
    private static final int EXECUTE_COUNT = 0;

    @Test
    @DisplayName("并行错峰：stepInstance.batch 恒为1时，命令按GSE任务批次校正 stepInstance")
    void parallelBatch_alignStepInstanceBatchToGseTaskBatch() {
        StepInstanceDTO stepInstance = buildStepInstance(1);
        GseTaskDTO gseTask = buildGseTask(4);

        new TestGseTaskCommand(
            mock(EngineDependentServiceHolder.class, RETURNS_DEEP_STUBS),
            mock(ExecuteObjectTaskService.class),
            buildTaskInstance(),
            stepInstance,
            gseTask);

        assertThat(stepInstance.getBatch()).isEqualTo(4);
    }

    @Test
    @DisplayName("串行/一致场景：批次已一致时对齐为无副作用")
    void consistentBatch_noSideEffect() {
        StepInstanceDTO stepInstance = buildStepInstance(2);
        GseTaskDTO gseTask = buildGseTask(2);

        new TestGseTaskCommand(
            mock(EngineDependentServiceHolder.class, RETURNS_DEEP_STUBS),
            mock(ExecuteObjectTaskService.class),
            buildTaskInstance(),
            stepInstance,
            gseTask);

        assertThat(stepInstance.getBatch()).isEqualTo(2);
    }

    private TaskInstanceDTO buildTaskInstance() {
        TaskInstanceDTO taskInstance = new TaskInstanceDTO();
        taskInstance.setId(JOB_INSTANCE_ID);
        taskInstance.setAppId(1L);
        return taskInstance;
    }

    private StepInstanceDTO buildStepInstance(int batch) {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setId(STEP_INSTANCE_ID);
        stepInstance.setTaskInstanceId(JOB_INSTANCE_ID);
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
     * 最小化具体子类，仅用于验证 {@link AbstractGseTaskCommand} 构造期的批次对齐逻辑。
     */
    private static class TestGseTaskCommand extends AbstractGseTaskCommand {

        TestGseTaskCommand(EngineDependentServiceHolder engineDependentServiceHolder,
                           ExecuteObjectTaskService executeObjectTaskService,
                           TaskInstanceDTO taskInstance,
                           StepInstanceDTO stepInstance,
                           GseTaskDTO gseTask) {
            super(engineDependentServiceHolder, executeObjectTaskService, taskInstance, stepInstance, gseTask);
        }

        @Override
        public void execute() {
            // no-op
        }
    }
}
