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

package com.tencent.bk.job.execute.model;

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.constant.CompatibleType;
import com.tencent.bk.job.common.constant.ExecuteObjectTypeEnum;
import com.tencent.bk.job.execute.engine.consts.ExecuteObjectTaskStatusEnum;
import com.tencent.bk.job.execute.engine.model.ExecuteObject;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * JOB执行对象任务
 */
@ToString
@NoArgsConstructor
public class ExecuteObjectTask {
    /**
     * 作业实例ID
     */
    private long taskInstanceId;
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
    private String executeObjectId;
    /**
     * 执行对象类型
     */
    private ExecuteObjectTypeEnum executeObjectType;
    /**
     * 执行对象
     */
    private ExecuteObject executeObject;
    /**
     * 主机 ID
     */
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容老数据，数据失效后可删除")
    private Long hostId;
    /**
     * 主机 agentId
     */
    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容老数据，数据失效后可删除")
    private String agentId;
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

    public ExecuteObjectTask(long taskInstanceId,
                             long stepInstanceId,
                             int executeCount,
                             int batch,
                             ExecuteObjectTypeEnum executeObjectType,
                             String executeObjectId) {
        this.taskInstanceId = taskInstanceId;
        this.stepInstanceId = stepInstanceId;
        this.executeCount = executeCount;
        this.batch = batch;
        this.executeObjectType = executeObjectType;
        this.executeObjectId = executeObjectId;
    }

    public ExecuteObjectTask(long taskInstanceId,
                             long stepInstanceId,
                             int executeCount,
                             int batch,
                             ExecuteObject executeObject) {
        this.taskInstanceId = taskInstanceId;
        this.stepInstanceId = stepInstanceId;
        this.executeCount = executeCount;
        this.batch = batch;
        setExecuteObject(executeObject);
    }

    public ExecuteObjectTask(long taskInstanceId,
                             long stepInstanceId,
                             int executeCount,
                             int batch,
                             FileTaskModeEnum fileTaskMode,
                             ExecuteObjectTypeEnum executeObjectType,
                             String executeObjectId) {
        this.taskInstanceId = taskInstanceId;
        this.stepInstanceId = stepInstanceId;
        this.executeCount = executeCount;
        this.batch = batch;
        this.fileTaskMode = fileTaskMode;
        this.executeObjectType = executeObjectType;
        this.executeObjectId = executeObjectId;
    }

    public ExecuteObjectTask(long taskInstanceId,
                             long stepInstanceId,
                             int executeCount,
                             int batch,
                             FileTaskModeEnum fileTaskMode,
                             ExecuteObject executeObject) {
        this.taskInstanceId = taskInstanceId;
        this.stepInstanceId = stepInstanceId;
        this.executeCount = executeCount;
        this.batch = batch;
        this.fileTaskMode = fileTaskMode;
        setExecuteObject(executeObject);
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

    public long getTaskInstanceId() {
        return taskInstanceId;
    }

    public void setTaskInstanceId(long taskInstanceId) {
        this.taskInstanceId = taskInstanceId;
    }

    public long getStepInstanceId() {
        return stepInstanceId;
    }

    public void setStepInstanceId(long stepInstanceId) {
        this.stepInstanceId = stepInstanceId;
    }

    public int getExecuteCount() {
        return executeCount;
    }

    public void setExecuteCount(int executeCount) {
        this.executeCount = executeCount;
    }

    public Integer getActualExecuteCount() {
        return actualExecuteCount;
    }

    public void setActualExecuteCount(Integer actualExecuteCount) {
        this.actualExecuteCount = actualExecuteCount;
    }

    public int getBatch() {
        return batch;
    }

    public void setBatch(int batch) {
        this.batch = batch;
    }

    public Long getGseTaskId() {
        return gseTaskId;
    }

    public void setGseTaskId(Long gseTaskId) {
        this.gseTaskId = gseTaskId;
    }

    public String getExecuteObjectId() {
        return executeObjectId;
    }

    public void setExecuteObjectId(String executeObjectId) {
        this.executeObjectId = executeObjectId;
    }

    public ExecuteObjectTypeEnum getExecuteObjectType() {
        return executeObjectType;
    }

    public void setExecuteObjectType(ExecuteObjectTypeEnum executeObjectType) {
        this.executeObjectType = executeObjectType;
    }

    public ExecuteObject getExecuteObject() {
        return executeObject;
    }

    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容老数据，数据失效后可删除")
    public Long getHostId() {
        return hostId;
    }

    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容老数据，数据失效后可删除")
    public void setHostId(Long hostId) {
        this.hostId = hostId;
    }

    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容老数据，数据失效后可删除")
    public String getAgentId() {
        return agentId;
    }

    @Deprecated
    @CompatibleImplementation(name = "execute_object", deprecatedVersion = "3.9.x", type = CompatibleType.HISTORY_DATA,
        explain = "兼容老数据，数据失效后可删除")
    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public ExecuteObjectTaskStatusEnum getStatus() {
        return status;
    }

    public Long getStartTime() {
        return startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public Long getTotalTime() {
        return totalTime;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public int getScriptLogOffset() {
        return scriptLogOffset;
    }

    public String getScriptLogContent() {
        return scriptLogContent;
    }

    public void setScriptLogContent(String scriptLogContent) {
        this.scriptLogContent = scriptLogContent;
    }

    public FileTaskModeEnum getFileTaskMode() {
        return fileTaskMode;
    }

    public void setFileTaskMode(FileTaskModeEnum fileTaskMode) {
        this.fileTaskMode = fileTaskMode;
    }

    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    public void setExecuteObject(ExecuteObject executeObject) {
        this.executeObject = executeObject;
        if (executeObject != null) {
            executeObjectType = executeObject.getType();
            executeObjectId = executeObject.getId();
            if (executeObject.isHostExecuteObject()) {
                // 兼容老数据，发布完成后可删除
                hostId = executeObject.getHost().getHostId();
                agentId = executeObject.getHost().getAgentId();
            }
        }
    }
}
