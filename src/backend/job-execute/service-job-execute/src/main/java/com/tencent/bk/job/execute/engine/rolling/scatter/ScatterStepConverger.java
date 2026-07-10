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
import com.tencent.bk.job.execute.engine.listener.event.EventSource;
import com.tencent.bk.job.execute.engine.listener.event.JobEvent;
import com.tencent.bk.job.execute.engine.listener.event.TaskExecuteMQEventDispatcher;
import com.tencent.bk.job.execute.engine.prepare.FilePrepareService;
import com.tencent.bk.job.execute.model.StepInstanceDTO;
import com.tencent.bk.job.execute.model.StepInstanceRollingTaskDTO;
import com.tencent.bk.job.execute.service.StepInstanceRollingTaskService;
import com.tencent.bk.job.execute.service.StepInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 并行错峰模式下步骤完成的收敛器（各调用方共用，避免收敛/聚合逻辑重复）。
 * <p>
 * 封装「单批次终态跃迁 + （若为最后一批）按各批聚合步骤终态并收敛」的并发安全操作，
 * 供步骤事件处理（GSE 回调、整步终止取消未下发批次）与错峰下发器（到点兜底收敛）复用。
 */
@Component
@Slf4j
public class ScatterStepConverger {

    private final StepInstanceService stepInstanceService;
    private final StepInstanceRollingTaskService stepInstanceRollingTaskService;
    private final TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher;
    private final FilePrepareService filePrepareService;

    @Autowired
    public ScatterStepConverger(StepInstanceService stepInstanceService,
                                StepInstanceRollingTaskService stepInstanceRollingTaskService,
                                TaskExecuteMQEventDispatcher taskExecuteMQEventDispatcher,
                                FilePrepareService filePrepareService) {
        this.stepInstanceService = stepInstanceService;
        this.stepInstanceRollingTaskService = stepInstanceRollingTaskService;
        this.taskExecuteMQEventDispatcher = taskExecuteMQEventDispatcher;
        this.filePrepareService = filePrepareService;
    }

    /**
     * 单批次终态跃迁 + 完成判定，若为最后一批则按各批聚合步骤终态并收敛。
     *
     * @param stepInstance      步骤实例
     * @param executeCount      执行次数
     * @param batch             当前到达终态的批次
     * @param batchStatus       当前批次终态
     * @param batchStartTime    当前批次开始时间
     * @param totalBatch        步骤总批次数
     * @param dispatchRefreshJob 收敛步骤时是否发送 refreshJob 事件（GSE 回调路径由 refreshStep 末尾统一发送，
     *                          此处传 false 以免重复；终止取消/到点兜底路径无外部 refreshJob，传 true）
     * @return 完成判定结果
     */
    public ScatterBatchFinishResult finishBatchAndConverge(StepInstanceDTO stepInstance,
                                                           int executeCount,
                                                           int batch,
                                                           RunStatusEnum batchStatus,
                                                           long batchStartTime,
                                                           int totalBatch,
                                                           boolean dispatchRefreshJob) {
        long taskInstanceId = stepInstance.getTaskInstanceId();
        long stepInstanceId = stepInstance.getId();
        long now = System.currentTimeMillis();

        ScatterBatchFinishResult result = stepInstanceRollingTaskService.finishBatchAndCheckAllDone(
            taskInstanceId, stepInstanceId, executeCount, batch, batchStatus,
            batchStartTime, now, now - batchStartTime, totalBatch);

        log.info("Converge parallel batch, stepInstanceId={}, executeCount={}, batch={}, batchStatus={}, result={}",
            stepInstanceId, executeCount, batch, batchStatus, result);

        if (result == ScatterBatchFinishResult.LAST_BATCH) {
            // 最后一个到达终态的批次，唯一收敛整个步骤
            RunStatusEnum stepStatus = aggregateParallelStepStatus(taskInstanceId, stepInstanceId, executeCount);
            long startTime = stepInstance.getStartTime() != null ? stepInstance.getStartTime() : batchStartTime;
            stepInstanceService.updateStepExecutionInfo(
                taskInstanceId, stepInstanceId, stepStatus, startTime, now, now - startTime);
            if (stepStatus == RunStatusEnum.SUCCESS || stepStatus == RunStatusEnum.IGNORE_ERROR) {
                clearStep(stepInstance);
            }
            if (dispatchRefreshJob) {
                taskExecuteMQEventDispatcher.dispatchJobEvent(
                    JobEvent.refreshJob(taskInstanceId,
                        EventSource.buildStepEventSource(taskInstanceId, stepInstanceId)));
            }
        }
        // NOT_LAST_BATCH / ALREADY_FINAL：不改步骤状态，步骤仍为 RUNNING/STOPPING，等待其它批次
        return result;
    }

    /**
     * 并行错峰模式：按各批次终态聚合步骤终态。
     * <p>
     * 优先级（与串行 finishStepWithAbnormalState 语义对齐）：
     * 任一批异常（ABNORMAL_STATE / ABANDONED）→ 步骤异常；否则任一批失败 → 失败；
     * 否则任一批终止成功 → 终止成功；否则任一批忽略错误 → 忽略错误；否则成功。
     */
    public RunStatusEnum aggregateParallelStepStatus(Long taskInstanceId, long stepInstanceId, int executeCount) {
        List<StepInstanceRollingTaskDTO> rollingTasks =
            stepInstanceRollingTaskService.listRollingTasksByStep(taskInstanceId, stepInstanceId);
        boolean hasAbnormal = false;
        boolean hasFail = false;
        boolean hasStopSuccess = false;
        boolean hasIgnoreError = false;
        for (StepInstanceRollingTaskDTO task : rollingTasks) {
            if (task.getExecuteCount() != executeCount) {
                continue;
            }
            RunStatusEnum status = task.getStatus();
            if (status == RunStatusEnum.ABNORMAL_STATE || status == RunStatusEnum.ABANDONED) {
                hasAbnormal = true;
            } else if (status == RunStatusEnum.FAIL) {
                hasFail = true;
            } else if (status == RunStatusEnum.STOP_SUCCESS) {
                hasStopSuccess = true;
            } else if (status == RunStatusEnum.IGNORE_ERROR) {
                hasIgnoreError = true;
            }
        }
        if (hasAbnormal) {
            return RunStatusEnum.ABNORMAL_STATE;
        }
        if (hasFail) {
            return RunStatusEnum.FAIL;
        }
        if (hasStopSuccess) {
            return RunStatusEnum.STOP_SUCCESS;
        }
        if (hasIgnoreError) {
            return RunStatusEnum.IGNORE_ERROR;
        }
        return RunStatusEnum.SUCCESS;
    }

    private void clearStep(StepInstanceDTO stepInstance) {
        // 当前仅有文件分发类步骤需要清理中间文件
        if (stepInstance.isFileStep()) {
            log.info("Clear file step, stepInstanceId={}", stepInstance.getId());
            filePrepareService.clearPreparedTmpFile(stepInstance.getTaskInstanceId(), stepInstance.getId());
        }
    }
}
