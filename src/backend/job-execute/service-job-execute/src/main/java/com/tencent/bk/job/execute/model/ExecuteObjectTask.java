/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 THL A29 Limited, a Tencent company.  All rights reserved.
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

package com.tencent.bk.job.execute.model;

import com.tencent.bk.job.execute.engine.consts.ExecuteObjectTaskStatusEnum;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

/**
 * GSE 执行对象任务
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class ExecuteObjectTask {
    /**
     * 步骤实例ID
     */
    private long stepInstanceId;
    /**
     * 步骤执行次数
     */
    private int executeCount;
    /**
     * 任务对应的实际的步骤执行次数（重试场景，可能任务并没有实际被执行)
     */
    private Integer actualExecuteCount;
    /**
     * 滚动执行批次
     */
    private int batch;
    /**
     * GSE 任务ID
     */
    private Long gseTaskId;
    /**
     * 执行对象 ID
     */
    private Long executeObjId;
    /**
     * 任务状态
     */
    private ExecuteObjectTaskStatusEnum status;
    /**
     * 任务开始时间
     */
    private Long startTime;
    /**
     * 任务结束时间
     */
    private Long endTime;
    /**
     * 耗时，毫秒
     */
    private Long totalTime;
    /**
     * GSE返回错误码
     */
    private int errorCode;
    /**
     * 脚本任务-执行程序退出码， 0 脚本执行成功，非 0 脚本执行失败
     */
    private Integer exitCode;
    /**
     * 脚本任务-用户自定义执行结果分组
     */
    private String tag = "";
    /**
     * 脚本任务-日志偏移量。Job 从 GSE 根据 scriptLogOffset 增量拉取执行日志
     */
    private int scriptLogOffset;
    /**
     * 脚本任务-执行日志
     */
    private String scriptLogContent;
    /**
     * 文件任务类型
     */
    private FileTaskModeEnum fileTaskMode;
    /**
     * 结果是否发生变化
     */
    private volatile boolean changed;

    public ExecuteObjectTask(long stepInstanceId, int executeCount, int batch, Long executeObjId) {
        this.stepInstanceId = stepInstanceId;
        this.executeCount = executeCount;
        this.batch = batch;
        this.executeObjId = executeObjId;
    }

    public ExecuteObjectTask(long stepInstanceId,
                             int executeCount,
                             int batch,
                             FileTaskModeEnum fileTaskMode,
                             Long executeObjId) {
        this.stepInstanceId = stepInstanceId;
        this.executeCount = executeCount;
        this.batch = batch;
        this.fileTaskMode = fileTaskMode;
        this.executeObjId = executeObjId;
    }

    public ExecuteObjectTask(ExecuteObjectTask agentTask) {
        this.stepInstanceId = agentTask.getStepInstanceId();
        this.executeCount = agentTask.getExecuteCount();
        this.actualExecuteCount = agentTask.getActualExecuteCount();
        this.batch = agentTask.getBatch();
        this.fileTaskMode = agentTask.getFileTaskMode();
        this.executeObjId = agentTask.getExecuteObjId();
        this.status = agentTask.getStatus();
        this.startTime = agentTask.getStartTime();
        this.endTime = agentTask.getEndTime();
        this.totalTime = agentTask.getTotalTime();
        this.errorCode = agentTask.getErrorCode();
        this.exitCode = agentTask.getExitCode();
        this.tag = agentTask.getTag();
        this.scriptLogOffset = agentTask.getScriptLogOffset();
        this.scriptLogContent = agentTask.getScriptLogContent();
        this.fileTaskMode = agentTask.getFileTaskMode();
        this.gseTaskId = agentTask.getGseTaskId();
        this.changed = agentTask.isChanged();
    }

    public void setStatus(ExecuteObjectTaskStatusEnum status) {
        this.changed = true;
        this.status = status;
    }

    public void setStartTime(Long startTime) {
        this.changed = true;
        this.startTime = startTime;
    }

    public void setEndTime(Long endTime) {
        this.changed = true;
        this.endTime = endTime;
    }

    public void setTotalTime(Long totalTime) {
        this.changed = true;
        this.totalTime = totalTime;
    }

    public void setErrorCode(int errorCode) {
        this.changed = true;
        this.errorCode = errorCode;
    }

    public void setExitCode(Integer exitCode) {
        this.changed = true;
        this.exitCode = exitCode;
    }

    public void setScriptLogOffset(int scriptLogOffset) {
        this.changed = true;
        this.scriptLogOffset = scriptLogOffset;
    }

    /**
     * 任务是否结束
     *
     * @return 任务是否结束
     */
    public boolean isFinished() {
        return status != ExecuteObjectTaskStatusEnum.WAITING && status != ExecuteObjectTaskStatusEnum.RUNNING;
    }

    /**
     * 计算任务运行时间
     */
    public void calculateTotalTime() {
        if (this.endTime != null && this.startTime != null && this.endTime > this.startTime) {
            this.totalTime = this.endTime - this.startTime;
        }
    }

    /**
     * 重置任务状态数据
     */
    public void resetTaskInitialStatus() {
        this.status = ExecuteObjectTaskStatusEnum.WAITING;
        this.startTime = null;
        this.endTime = null;
        this.totalTime = null;

        this.errorCode = 0;
        this.scriptLogOffset = 0;
        this.exitCode = null;
        this.gseTaskId = null;
    }

    /**
     * 是否任务执行目标Agent
     */
    public boolean isTarget() {
        // 非文件分发文件源主机，目前来说都是目标主机
        return !(fileTaskMode != null && fileTaskMode == FileTaskModeEnum.UPLOAD);
    }


    /**
     * 任务是否执行成功
     */
    public boolean isSuccess() {
        return ExecuteObjectTaskStatusEnum.isSuccess(status);
    }

    public boolean isAgentIdEmpty() {
        return StringUtils.isEmpty(agentId);
    }
}
