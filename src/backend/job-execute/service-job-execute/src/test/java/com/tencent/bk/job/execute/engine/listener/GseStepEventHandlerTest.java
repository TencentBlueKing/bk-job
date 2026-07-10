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
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.execute.engine.listener.event.EventSource;
import com.tencent.bk.job.execute.engine.listener.event.StepEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.execute.engine.prepare.FilePrepareService;
import com.tencent.bk.job.execute.engine.rolling.scatter.ScatterBatchFinishResult;
import com.tencent.bk.job.execute.engine.rolling.scatter.ScatterDispatchManager;
import com.tencent.bk.job.execute.model.ExecuteObjectTask;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.RollingConfigDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceRollingTaskDTO;
import com.tencent.bk.job.execute.model.db.ExecuteObjectRollingConfigDetailDO;
import com.tencent.bk.job.execute.service.FileExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceRollingTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.TaskInstanceService;
import com.tencent.bk.job.execute.service.rolling.RollingConfigService;
import com.tencent.bk.job.execute.service.rolling.StepInstanceFileBatchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
 * {@link GseStepEventHandler} 并行错峰模式（executionMode=2）刷新步骤状态的回归测试。
 * <p>
 * 覆盖 Issue #4368 的 S1 缺陷：REFRESH 路径必须按 GSE 任务携带的“真正结束批次”做完成判定，
 * 而不是恒为 1 的 stepInstance.getBatch()；否则多批时步骤永不收敛。
 */
class GseStepEventHandlerTest {

    private static final long JOB_INSTANCE_ID = 100L;
    private static final long STEP_INSTANCE_ID = 200L;
    private static final long GSE_TASK_ID = 300L;
    private static final long ROLLING_CONFIG_ID = 400L;
    private static final int EXECUTE_COUNT = 0;

    private StepInstanceService stepInstanceService;
    private TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    private FilePrepareService filePrepareService;
    private GseTaskService gseTaskService;
    private RollingConfigService rollingConfigService;
    private StepInstanceRollingTaskService stepInstanceRollingTaskService;
    private FileExecuteObjectTaskService fileExecuteObjectTaskService;
    private ScatterDispatchManager scatterDispatchManager;

    private GseStepEventHandler handler;

    @BeforeEach
    void setUp() {
        TaskInstanceService taskInstanceService = mock(TaskInstanceService.class);
        stepInstanceService = mock(StepInstanceService.class);
        taskExecuteMQEventDispatcher = mock(TaskExecuteMQEventDispatcher.class);
        filePrepareService = mock(FilePrepareService.class);
        gseTaskService = mock(GseTaskService.class);
        rollingConfigService = mock(RollingConfigService.class);
        stepInstanceRollingTaskService = mock(StepInstanceRollingTaskService.class);
        ScriptExecuteObjectTaskService scriptExecuteObjectTaskService = mock(ScriptExecuteObjectTaskService.class);
        fileExecuteObjectTaskService = mock(FileExecuteObjectTaskService.class);
        StepInstanceFileBatchService stepInstanceFileBatchService = mock(StepInstanceFileBatchService.class);
        scatterDispatchManager = mock(ScatterDispatchManager.class);

        handler = new GseStepEventHandler(
            taskInstanceService,
            stepInstanceService,
            taskExecuteMQEventDispatcher,
            filePrepareService,
            gseTaskService,
            rollingConfigService,
            stepInstanceRollingTaskService,
            scriptExecuteObjectTaskService,
            fileExecuteObjectTaskService,
            stepInstanceFileBatchService,
            scatterDispatchManager);
    }

    @Test
    @DisplayName("并行错峰-成功回调：按GSE任务结束批次(而非stepInstance.getBatch())做完成判定")
    void parallelSuccess_finishByGseTaskBatch() {
        int totalBatch = 4;
        int finishedBatch = 3;
        mockParallelRollingConfig(totalBatch);
        // stepInstance.getBatch() 恒为 1，GSE 任务携带真正结束的批次 3
        StepInstanceDTO stepInstance = buildRollingStepInstance(1);
        mockGseTask(finishedBatch, RunStatusEnum.SUCCESS);
        when(stepInstanceRollingTaskService.finishBatchAndCheckAllDone(
            any(), anyLong(), anyInt(), anyInt(), any(), any(), any(), any(), anyInt()))
            .thenReturn(ScatterBatchFinishResult.NOT_LAST_BATCH);

        handler.handleEvent(buildRefreshEvent(), stepInstance);

        ArgumentCaptor<Integer> batchCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<RunStatusEnum> statusCaptor = ArgumentCaptor.forClass(RunStatusEnum.class);
        verify(stepInstanceRollingTaskService).finishBatchAndCheckAllDone(
            eq(JOB_INSTANCE_ID), eq(STEP_INSTANCE_ID), eq(EXECUTE_COUNT),
            batchCaptor.capture(), statusCaptor.capture(),
            any(), any(), any(), eq(totalBatch));
        assertThat(batchCaptor.getValue()).isEqualTo(finishedBatch);
        assertThat(statusCaptor.getValue()).isEqualTo(RunStatusEnum.SUCCESS);
        // 非最后批次：步骤保持 RUNNING，不更新步骤终态
        verify(stepInstanceService, never()).updateStepExecutionInfo(
            anyLong(), anyLong(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("并行错峰-失败回调：按GSE任务结束批次做完成判定，批次终态为失败")
    void parallelFail_finishByGseTaskBatch() {
        int totalBatch = 4;
        int finishedBatch = 2;
        mockParallelRollingConfig(totalBatch);
        StepInstanceDTO stepInstance = buildRollingStepInstance(1);
        stepInstance.setIgnoreError(false);
        mockGseTask(finishedBatch, RunStatusEnum.FAIL);
        when(stepInstanceRollingTaskService.finishBatchAndCheckAllDone(
            any(), anyLong(), anyInt(), anyInt(), any(), any(), any(), any(), anyInt()))
            .thenReturn(ScatterBatchFinishResult.NOT_LAST_BATCH);

        handler.handleEvent(buildRefreshEvent(), stepInstance);

        ArgumentCaptor<Integer> batchCaptor = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<RunStatusEnum> statusCaptor = ArgumentCaptor.forClass(RunStatusEnum.class);
        verify(stepInstanceRollingTaskService).finishBatchAndCheckAllDone(
            eq(JOB_INSTANCE_ID), eq(STEP_INSTANCE_ID), eq(EXECUTE_COUNT),
            batchCaptor.capture(), statusCaptor.capture(),
            any(), any(), any(), eq(totalBatch));
        assertThat(batchCaptor.getValue()).isEqualTo(finishedBatch);
        assertThat(statusCaptor.getValue()).isEqualTo(RunStatusEnum.FAIL);
    }

    @Test
    @DisplayName("并行错峰-最后批次收敛：存在失败批时步骤聚合为失败并收敛")
    void parallelLastBatch_aggregateToFail() {
        int totalBatch = 3;
        mockParallelRollingConfig(totalBatch);
        StepInstanceDTO stepInstance = buildRollingStepInstance(1);
        stepInstance.setStartTime(1000L);
        mockGseTask(3, RunStatusEnum.SUCCESS);
        when(stepInstanceRollingTaskService.finishBatchAndCheckAllDone(
            any(), anyLong(), anyInt(), anyInt(), any(), any(), any(), any(), anyInt()))
            .thenReturn(ScatterBatchFinishResult.LAST_BATCH);
        // 三批终态：批2失败 → 步骤聚合为失败
        when(stepInstanceRollingTaskService.listRollingTasksByStep(JOB_INSTANCE_ID, STEP_INSTANCE_ID))
            .thenReturn(Arrays.asList(
                buildRollingTask(1, RunStatusEnum.SUCCESS),
                buildRollingTask(2, RunStatusEnum.FAIL),
                buildRollingTask(3, RunStatusEnum.SUCCESS)));

        handler.handleEvent(buildRefreshEvent(), stepInstance);

        ArgumentCaptor<RunStatusEnum> statusCaptor = ArgumentCaptor.forClass(RunStatusEnum.class);
        verify(stepInstanceService).updateStepExecutionInfo(
            eq(JOB_INSTANCE_ID), eq(STEP_INSTANCE_ID), statusCaptor.capture(), any(), any(), any());
        assertThat(statusCaptor.getValue()).isEqualTo(RunStatusEnum.FAIL);
    }

    @Test
    @DisplayName("并行错峰-最后批次收敛：全部成功时步骤聚合为成功并清理")
    void parallelLastBatch_aggregateToSuccess() {
        int totalBatch = 3;
        mockParallelRollingConfig(totalBatch);
        StepInstanceDTO stepInstance = buildRollingStepInstance(1);
        stepInstance.setStartTime(1000L);
        mockGseTask(3, RunStatusEnum.SUCCESS);
        when(stepInstanceRollingTaskService.finishBatchAndCheckAllDone(
            any(), anyLong(), anyInt(), anyInt(), any(), any(), any(), any(), anyInt()))
            .thenReturn(ScatterBatchFinishResult.LAST_BATCH);
        when(stepInstanceRollingTaskService.listRollingTasksByStep(JOB_INSTANCE_ID, STEP_INSTANCE_ID))
            .thenReturn(Arrays.asList(
                buildRollingTask(1, RunStatusEnum.SUCCESS),
                buildRollingTask(2, RunStatusEnum.SUCCESS),
                buildRollingTask(3, RunStatusEnum.SUCCESS)));

        handler.handleEvent(buildRefreshEvent(), stepInstance);

        ArgumentCaptor<RunStatusEnum> statusCaptor = ArgumentCaptor.forClass(RunStatusEnum.class);
        verify(stepInstanceService).updateStepExecutionInfo(
            eq(JOB_INSTANCE_ID), eq(STEP_INSTANCE_ID), statusCaptor.capture(), any(), any(), any());
        assertThat(statusCaptor.getValue()).isEqualTo(RunStatusEnum.SUCCESS);
        // 成功收敛后清理文件分发中间文件
        verify(filePrepareService).clearPreparedTmpFile(JOB_INSTANCE_ID, STEP_INSTANCE_ID);
    }

    @Test
    @DisplayName("并行错峰-重复回调幂等：批次已终态返回ALREADY_FINAL时不改步骤状态")
    void parallelDuplicateCallback_idempotent() {
        mockParallelRollingConfig(4);
        StepInstanceDTO stepInstance = buildRollingStepInstance(1);
        mockGseTask(2, RunStatusEnum.SUCCESS);
        when(stepInstanceRollingTaskService.finishBatchAndCheckAllDone(
            any(), anyLong(), anyInt(), anyInt(), any(), any(), any(), any(), anyInt()))
            .thenReturn(ScatterBatchFinishResult.ALREADY_FINAL);

        handler.handleEvent(buildRefreshEvent(), stepInstance);

        verify(stepInstanceService, never()).updateStepExecutionInfo(
            anyLong(), anyLong(), any(), any(), any(), any());
        verify(filePrepareService, never()).clearPreparedTmpFile(anyLong(), anyLong());
    }

    @Test
    @DisplayName("串行模式-成功回调：不走并行完成判定，不回填结束批次")
    void serialSuccess_notUseParallelFinish() {
        int currentBatch = 2;
        mockSerialRollingConfig();
        StepInstanceDTO stepInstance = buildRollingStepInstance(currentBatch);
        mockGseTask(currentBatch, RunStatusEnum.SUCCESS);
        // 非最后批次，走 ROLLING_WAITING，避免清理逻辑
        when(rollingConfigService.getTotalBatch(JOB_INSTANCE_ID, STEP_INSTANCE_ID, ROLLING_CONFIG_ID))
            .thenReturn(3);

        handler.handleEvent(buildRefreshEvent(), stepInstance);

        // 串行模式绝不调用并行完成判定
        verify(stepInstanceRollingTaskService, never()).finishBatchAndCheckAllDone(
            any(), anyLong(), anyInt(), anyInt(), any(), any(), any(), any(), anyInt());
        // 串行分支按 stepInstance 自身批次收敛，batch 未被篡改
        assertThat(stepInstance.getBatch()).isEqualTo(currentBatch);
    }

    @Test
    @DisplayName("并行错峰-终止后重试全部：整步所有批次重新错峰下发，且各批执行对象任务仅插入一次（不触发唯一键冲突）")
    void parallelRetryAll_reschedulesAllBatchesWithoutDuplicateInsert() {
        int totalBatch = 4;
        int retryExecuteCount = 1;
        mockParallelScatterRollingConfig(totalBatch);
        StepInstanceDTO stepInstance = buildRollingStepInstance(1);
        stepInstance.setExecuteCount(retryExecuteCount);
        stepInstance.setStatus(RunStatusEnum.STOP_SUCCESS);
        when(gseTaskService.saveGseTask(any())).thenReturn(GSE_TASK_ID);
        when(filePrepareService.needToPrepareSourceFilesForGseTask(any())).thenReturn(false);
        // 模拟真实 DAO 的按批次查询：携带具体 batch 时仅返回该批次的执行对象任务；
        // batch 为 null 时才返回全批次（与 FileExecuteObjectTaskDAOImpl.listTasks 的 batch 过滤语义一致）
        when(fileExecuteObjectTaskService.listTasks(any(), eq(retryExecuteCount - 1), any(), any()))
            .thenAnswer(invocation -> buildBatchExecuteObjectTasks(
                totalBatch, retryExecuteCount - 1, invocation.getArgument(2)));

        handler.handleEvent(StepEvent.retryStepAll(JOB_INSTANCE_ID, STEP_INSTANCE_ID), stepInstance);

        // 关键优化断言：并行重试在查询层就按批次过滤，每批仅查询「当前 batch」的执行对象任务，
        // 而非查询全量后在内存中过滤，故 listTasks 被逐批调用且批次参数恰为 1..totalBatch。
        ArgumentCaptor<Integer> queryBatchCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(fileExecuteObjectTaskService, org.mockito.Mockito.times(totalBatch))
            .listTasks(any(), eq(retryExecuteCount - 1), queryBatchCaptor.capture(), any());
        assertThat(queryBatchCaptor.getAllValues()).containsExactlyInAnyOrder(1, 2, 3, 4);

        // 批1 立即下发（MQ 事件），批2..N 进入错峰延迟队列
        verify(taskExecuteMQEventDispatcher, org.mockito.Mockito.times(1)).dispatchGseTaskEvent(any());
        ArgumentCaptor<com.tencent.bk.job.execute.engine.rolling.scatter.ScatterDispatchTask> scatterCaptor =
            ArgumentCaptor.forClass(com.tencent.bk.job.execute.engine.rolling.scatter.ScatterDispatchTask.class);
        verify(scatterDispatchManager, org.mockito.Mockito.times(totalBatch - 1)).addTask(scatterCaptor.capture());
        // 入队批次为 2..N，且 executeCount 使用递增后的新执行次数
        List<Integer> scatterBatches = scatterCaptor.getAllValues().stream()
            .peek(task -> assertThat(task.getExecuteCount()).isEqualTo(retryExecuteCount))
            .map(com.tencent.bk.job.execute.engine.rolling.scatter.ScatterDispatchTask::getBatch)
            .sorted()
            .collect(Collectors.toList());
        assertThat(scatterBatches).containsExactly(2, 3, 4);

        // 关键回归断言：各批执行对象任务只按「本批次」插入一次，(batch, executeObjectId) 不重复，
        // 避免逐批循环重复插入全批次而违反唯一键 uk_step_id_execute_count_batch_mode_execute_obj_id。
        ArgumentCaptor<Collection<ExecuteObjectTask>> saveCaptor =
            ArgumentCaptor.forClass(Collection.class);
        verify(fileExecuteObjectTaskService, org.mockito.Mockito.atLeastOnce()).batchSaveTasks(saveCaptor.capture());
        List<String> savedKeys = new ArrayList<>();
        for (Collection<ExecuteObjectTask> saved : saveCaptor.getAllValues()) {
            for (ExecuteObjectTask task : saved) {
                // 所有落库执行对象任务都必须使用递增后的新执行次数
                assertThat(task.getExecuteCount()).isEqualTo(retryExecuteCount);
                savedKeys.add(task.getBatch() + ":" + task.getExecuteObjectId());
            }
        }
        // 每批一个执行对象，共 4 条，且互不重复（若重复插入会出现 16 条并含重复键）
        assertThat(savedKeys).hasSize(totalBatch);
        assertThat(savedKeys).doesNotHaveDuplicates();
    }

    private void mockParallelRollingConfig(int totalBatch) {
        RollingConfigDTO rollingConfig = new RollingConfigDTO();
        ExecuteObjectRollingConfigDetailDO detail = new ExecuteObjectRollingConfigDetailDO();
        detail.setExecutionMode(RollingExecutionModeEnum.PARALLEL.getValue());
        detail.setMode(RollingModeEnum.PAUSE_IF_FAIL.getValue());
        detail.setTotalBatch(totalBatch);
        rollingConfig.setExecuteObjectRollingConfig(detail);
        when(rollingConfigService.getRollingConfig(JOB_INSTANCE_ID, ROLLING_CONFIG_ID)).thenReturn(rollingConfig);
    }

    private void mockParallelScatterRollingConfig(int totalBatch) {
        RollingConfigDTO rollingConfig = new RollingConfigDTO();
        ExecuteObjectRollingConfigDetailDO detail = new ExecuteObjectRollingConfigDetailDO();
        detail.setExecutionMode(RollingExecutionModeEnum.PARALLEL.getValue());
        detail.setMode(RollingModeEnum.PAUSE_IF_FAIL.getValue());
        detail.setTotalBatch(totalBatch);
        detail.setBatchStartWaitFixedMs(10000L);
        detail.setBatchStartWaitRandomMinMs(1000L);
        detail.setBatchStartWaitRandomMaxMs(3000L);
        rollingConfig.setExecuteObjectRollingConfig(detail);
        when(rollingConfigService.getRollingConfig(JOB_INSTANCE_ID, ROLLING_CONFIG_ID)).thenReturn(rollingConfig);
    }

    private void mockSerialRollingConfig() {
        RollingConfigDTO rollingConfig = new RollingConfigDTO();
        ExecuteObjectRollingConfigDetailDO detail = new ExecuteObjectRollingConfigDetailDO();
        detail.setExecutionMode(RollingExecutionModeEnum.SERIAL.getValue());
        detail.setMode(RollingModeEnum.PAUSE_IF_FAIL.getValue());
        detail.setIncludeStepInstanceIdList(Collections.singletonList(STEP_INSTANCE_ID));
        rollingConfig.setExecuteObjectRollingConfig(detail);
        when(rollingConfigService.getRollingConfig(JOB_INSTANCE_ID, ROLLING_CONFIG_ID)).thenReturn(rollingConfig);
    }

    private void mockGseTask(int batch, RunStatusEnum status) {
        GseTaskDTO gseTask = new GseTaskDTO();
        gseTask.setBatch(batch);
        gseTask.setStatus(status.getValue());
        when(gseTaskService.getGseTask(JOB_INSTANCE_ID, GSE_TASK_ID)).thenReturn(gseTask);
    }

    private StepInstanceDTO buildRollingStepInstance(int currentBatch) {
        StepInstanceDTO stepInstance = new StepInstanceDTO();
        stepInstance.setId(STEP_INSTANCE_ID);
        stepInstance.setTaskInstanceId(JOB_INSTANCE_ID);
        stepInstance.setExecuteCount(EXECUTE_COUNT);
        stepInstance.setBatch(currentBatch);
        stepInstance.setRollingConfigId(ROLLING_CONFIG_ID);
        stepInstance.setExecuteType(StepExecuteTypeEnum.SEND_FILE);
        stepInstance.setStatus(RunStatusEnum.RUNNING);
        stepInstance.setStartTime(1000L);
        return stepInstance;
    }

    /**
     * 构造模拟 DAO 返回的执行对象任务：
     * <ul>
     *     <li>{@code queryBatch} 为 null：返回全批次（每批一个执行对象），模拟不带 batch 条件的查询；</li>
     *     <li>{@code queryBatch} 非 null：仅返回该批次的执行对象，模拟带 batch=? 条件的查询。</li>
     * </ul>
     */
    private List<ExecuteObjectTask> buildBatchExecuteObjectTasks(int totalBatch,
                                                                 int executeCount,
                                                                 Integer queryBatch) {
        List<ExecuteObjectTask> tasks = new ArrayList<>();
        for (int batch = 1; batch <= totalBatch; batch++) {
            if (queryBatch != null && batch != queryBatch) {
                continue;
            }
            ExecuteObject executeObject = mock(ExecuteObject.class);
            when(executeObject.isExecutable()).thenReturn(true);
            when(executeObject.getId()).thenReturn("eo-" + batch);
            ExecuteObjectTask task = new ExecuteObjectTask(
                JOB_INSTANCE_ID, STEP_INSTANCE_ID, executeCount, batch,
                com.tencent.bk.job.logsvr.consts.FileTaskModeEnum.DOWNLOAD, executeObject);
            task.setStatus(com.tencent.bk.job.execute.engine.consts.ExecuteObjectTaskStatusEnum.WAITING);
            tasks.add(task);
        }
        return tasks;
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

    private StepEvent buildRefreshEvent() {
        EventSource eventSource = EventSource.buildGseTaskEventSource(
            JOB_INSTANCE_ID, STEP_INSTANCE_ID, EXECUTE_COUNT, null, GSE_TASK_ID);
        return StepEvent.refreshStep(JOB_INSTANCE_ID, STEP_INSTANCE_ID, eventSource);
    }
}
