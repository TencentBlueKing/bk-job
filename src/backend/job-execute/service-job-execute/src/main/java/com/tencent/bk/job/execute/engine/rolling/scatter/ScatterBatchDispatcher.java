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
import com.tencent.bk.job.execute.engine.listener.event.GseTaskEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.model.GseTaskDTO;
import com.tencent.bk.job.execute.model.RollingConfigDTO;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceRollingTaskDTO;
import com.tencent.bk.job.execute.service.FileExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.GseTaskService;
import com.tencent.bk.job.execute.service.ScriptExecuteObjectTaskService;
import com.tencent.bk.job.execute.service.StepInstanceRollingTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import com.tencent.bk.job.execute.service.rolling.RollingConfigService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 并行错峰模式批次下发器。
 * <p>
 * 到点后仅做“轻量准备 + 发 MQ 事件”：创建批次 GSE 任务、绑定该批次的执行对象任务、标记已下发，
 * 随后发送 {@link GseTaskEvent#startGseTask} 交由 {@code GseTaskListener} 竞争消费，从而把各批
 * GSE 下发与结果轮询均分到多实例。严禁在此直接执行 GSE 下发。
 */
@Component
@Slf4j
public class ScatterBatchDispatcher {

    private final StepInstanceService stepInstanceService;
    private final GseTaskService gseTaskService;
    private final ScriptExecuteObjectTaskService scriptExecuteObjectTaskService;
    private final FileExecuteObjectTaskService fileExecuteObjectTaskService;
    private final StepInstanceRollingTaskService stepInstanceRollingTaskService;
    private final TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    private final RollingConfigService rollingConfigService;
    private final ScatterStepConverger scatterStepConverger;

    @Autowired
    public ScatterBatchDispatcher(StepInstanceService stepInstanceService,
                                  GseTaskService gseTaskService,
                                  ScriptExecuteObjectTaskService scriptExecuteObjectTaskService,
                                  FileExecuteObjectTaskService fileExecuteObjectTaskService,
                                  StepInstanceRollingTaskService stepInstanceRollingTaskService,
                                  TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                                  RollingConfigService rollingConfigService,
                                  ScatterStepConverger scatterStepConverger) {
        this.stepInstanceService = stepInstanceService;
        this.gseTaskService = gseTaskService;
        this.scriptExecuteObjectTaskService = scriptExecuteObjectTaskService;
        this.fileExecuteObjectTaskService = fileExecuteObjectTaskService;
        this.stepInstanceRollingTaskService = stepInstanceRollingTaskService;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
        this.rollingConfigService = rollingConfigService;
        this.scatterStepConverger = scatterStepConverger;
    }

    /**
     * 下发指定批次。
     *
     * @param taskInstanceId       作业实例ID
     * @param stepInstanceId       步骤实例ID
     * @param executeCount         执行次数
     * @param batch                滚动批次
     * @param preassignedGseTaskId 预分配的 GSE 任务ID；非 null 则直接发事件、不再创建/绑定
     */
    public void dispatchBatch(Long taskInstanceId,
                              long stepInstanceId,
                              int executeCount,
                              int batch,
                              Long preassignedGseTaskId) {
        StepInstanceDTO stepInstance = stepInstanceService.getStepInstanceDetail(taskInstanceId, stepInstanceId);
        if (stepInstance == null) {
            log.warn("Scatter dispatch skip, step instance not found, stepInstanceId={}", stepInstanceId);
            return;
        }
        RunStatusEnum stepStatus = stepInstance.getStatus();
        boolean executeCountMatched = executeCount == stepInstance.getExecuteCount();
        if (stepStatus != RunStatusEnum.RUNNING) {
            // 到点兜底收敛：步骤已进入终止/终态语义（如整步终止落到非调度副本时，本副本队列中的未下发批次
            // 未被取消，到点仍会走到这里），此时不能只“跳过下发”，否则该批 rolling_task 永不置终态，
            // finishBatchAndCheckAllDone 的终态计数永远达不到 totalBatch，步骤永久卡在 STOPPING。
            // 因此把该未下发批次经收敛器置为终止成功并参与完成判定（幂等闸门保证重复回调/多副本安全）。
            if (executeCountMatched
                && (stepStatus == RunStatusEnum.STOPPING || RunStatusEnum.isFinishedStatus(stepStatus))) {
                convergeUnDispatchedBatchOnStop(stepInstance, executeCount, batch);
            } else {
                log.info("Scatter dispatch skip, step is not running. stepInstanceId={}, batch={}, status={}",
                    stepInstanceId, batch, stepStatus);
            }
            return;
        }
        if (!executeCountMatched) {
            log.info("Scatter dispatch skip, execute count changed. stepInstanceId={}, batch={}, "
                    + "eventExecuteCount={}, curExecuteCount={}", stepInstanceId, batch, executeCount,
                stepInstance.getExecuteCount());
            return;
        }

        StepInstanceRollingTaskDTO rollingTask = stepInstanceRollingTaskService.queryRollingTask(
            taskInstanceId, stepInstanceId, executeCount, batch);
        if (rollingTask == null) {
            log.warn("Scatter dispatch skip, rolling task not found. stepInstanceId={}, batch={}",
                stepInstanceId, batch);
            return;
        }
        if (Boolean.TRUE.equals(rollingTask.getDispatched())) {
            log.info("Scatter dispatch skip, batch already dispatched. stepInstanceId={}, batch={}",
                stepInstanceId, batch);
            return;
        }

        stepInstance.setBatch(batch);
        Long gseTaskId;
        if (preassignedGseTaskId != null) {
            // 并行重试等场景，批次的 GSE 任务与执行对象绑定已在准备阶段完成，直接复用
            gseTaskId = preassignedGseTaskId;
        } else {
            // 创建该批次的 GSE 任务并绑定执行对象任务
            gseTaskId = saveInitialGseTask(stepInstance);
            if (stepInstance.isScriptStep()) {
                scriptExecuteObjectTaskService.updateTaskFields(stepInstance, executeCount, batch, executeCount,
                    gseTaskId);
            } else if (stepInstance.isFileStep()) {
                fileExecuteObjectTaskService.updateTaskFields(stepInstance, executeCount, batch, executeCount,
                    gseTaskId);
            }
        }

        long now = System.currentTimeMillis();
        // 标记批次开始运行 + 已下发
        stepInstanceRollingTaskService.updateRollingTask(taskInstanceId, stepInstanceId, executeCount, batch,
            RunStatusEnum.RUNNING, now, null, null);
        stepInstanceRollingTaskService.updateDispatchInfo(taskInstanceId, stepInstanceId, executeCount, batch,
            null, Boolean.TRUE);

        // 仅发送 MQ 事件，由 GseTaskListener 竞争消费，实现多实例均摊
        taskExecuteMQEventDispatcher.dispatchGseTaskEvent(
            GseTaskEvent.startGseTask(taskInstanceId, stepInstanceId, executeCount, batch, gseTaskId, null));
        log.info("Scatter dispatch batch done. stepInstanceId={}, executeCount={}, batch={}, gseTaskId={}",
            stepInstanceId, executeCount, batch, gseTaskId);
    }

    /**
     * 到点兜底：步骤已终止/终态时，把本未下发批次置为终止成功并参与步骤完成判定，避免步骤永久卡在 STOPPING。
     * <p>
     * 仅在并行错峰模式下执行；非并行模式无该收敛语义，直接跳过。
     *
     * @param stepInstance 步骤实例
     * @param executeCount 执行次数
     * @param batch        未下发批次
     */
    private void convergeUnDispatchedBatchOnStop(StepInstanceDTO stepInstance, int executeCount, int batch) {
        RollingConfigDTO rollingConfig = rollingConfigService.getRollingConfig(
            stepInstance.getTaskInstanceId(), stepInstance.getRollingConfigId());
        if (rollingConfig == null || !rollingConfig.isParallelExecution()) {
            log.info("Scatter dispatch skip converge, not parallel scatter. stepInstanceId={}, batch={}",
                stepInstance.getId(), batch);
            return;
        }
        int totalBatch = rollingConfig.getExecuteObjectRollingConfig().getTotalBatch();
        long now = System.currentTimeMillis();
        scatterStepConverger.finishBatchAndConverge(
            stepInstance, executeCount, batch, RunStatusEnum.STOP_SUCCESS, now, totalBatch);
        log.info("Scatter dispatch converge un-dispatched batch on stop, stepInstanceId={}, executeCount={}, "
                + "batch={}, stepStatus={}", stepInstance.getId(), executeCount, batch, stepInstance.getStatus());
    }

    private Long saveInitialGseTask(StepInstanceDTO stepInstance) {
        GseTaskDTO gseTask = new GseTaskDTO(stepInstance.getTaskInstanceId(), stepInstance.getId(),
            stepInstance.getExecuteCount(), stepInstance.getBatch());
        gseTask.setStatus(RunStatusEnum.BLANK.getValue());
        return gseTaskService.saveGseTask(gseTask);
    }
}
