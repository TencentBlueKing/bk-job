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

import com.tencent.bk.job.common.annotation.CompatibleImplementation;
import com.tencent.bk.job.common.model.dto.HostDTO;
import com.tencent.bk.job.execute.engine.consts.AgentTaskStatusEnum;
import com.tencent.bk.job.logsvr.consts.FileTaskModeEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

/**
 * GSE Agent 任务
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class AgentTaskDTO {
    /**
     * 步骤实例ID
     */
    private long stepInstanceId;
    /**
     * 步骤执行次数
     */
    private int executeCount;
    /**
     * Agent 任务对应的实际的步骤执行次数（重试场景，可能Agent任务并没有实际被执行)
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
     * 主机ID
     */
    private Long hostId;
    /**
     * Agent ID
     */
    private String agentId;
    /**
     * 服务器云区域+IP
     */
    @Deprecated
    @CompatibleImplementation(name = "rolling_execute", explain = "兼容字段，后续AgentTask仅包含hostId,不再存储具体的IP数据",
        version = "3.7.x")
    private String cloudIp;
    /**
     * 任务状态
     */
    private AgentTaskStatusEnum status;
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

    public AgentTaskDTO(long stepInstanceId, int executeCount, int batch, Long hostId, String agentId) {
        this.stepInstanceId = stepInstanceId;
        this.executeCount = executeCount;
        this.batch = batch;
        this.hostId = hostId;
        this.agentId = agentId;
    }

    public AgentTaskDTO(long stepInstanceId, int executeCount, int batch, FileTaskModeEnum fileTaskMode,
                        Long hostId, String agentId) {
        this.stepInstanceId = stepInstanceId;
        this.executeCount = executeCount;
        this.batch = batch;
        this.fileTaskMode = fileTaskMode;
        this.hostId = hostId;
        this.agentId = agentId;
    }

    public AgentTaskDTO(AgentTaskDTO agentTask) {
        this.stepInstanceId = agentTask.getStepInstanceId();
        this.executeCount = agentTask.getExecuteCount();
        this.actualExecuteCount = agentTask.getActualExecuteCount();
        this.batch = agentTask.getBatch();
        this.fileTaskMode = agentTask.getFileTaskMode();
        this.hostId = agentTask.getHostId();
        this.agentId = agentTask.getAgentId();
        this.cloudIp = agentTask.getCloudIp();
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

    public void setStatus(AgentTaskStatusEnum status) {
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
        return status != AgentTaskStatusEnum.WAITING && status != AgentTaskStatusEnum.RUNNING;
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
        this.status = AgentTaskStatusEnum.WAITING;
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

    public HostDTO getHost() {
        HostDTO host = null;
        if (hostId != null) {
            host = new HostDTO();
            host.setHostId(hostId);
            host.setAgentId(agentId);
        } else if (StringUtils.isNotEmpty(cloudIp)) {
            host = HostDTO.fromCloudIp(cloudIp);
        }

        return host;
    }

    /**
     * 任务是否执行成功
     */
    public boolean isSuccess() {
        return AgentTaskStatusEnum.isSuccess(status);
    }

    public String getAgentId() {
        if (StringUtils.isNotBlank(agentId)) {
            return agentId;
        } else {
            // 兼容没有agentId的历史数据
            return getCloudIp();
        }
    }
}
