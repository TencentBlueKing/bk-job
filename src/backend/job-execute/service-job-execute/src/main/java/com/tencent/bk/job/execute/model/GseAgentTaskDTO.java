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

import com.tencent.bk.job.execute.engine.consts.IpStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * GSE Agent 任务
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
public class GseAgentTaskDTO {
    /**
     * 步骤实例ID
     */
    private long stepInstanceId;
    /**
     * 步骤执行次数
     */
    private int executeCount;
    /**
     * 滚动执行批次
     */
    private int batch;
    /**
     * 服务器IP,包含云区域
     */
    private String cloudAreaAndIp;
    /**
     * 服务器IP,不包含云区域
     */
    private String ip;
    /**
     * 是否目标服务器
     */
    private boolean isTargetServer = true;
    /**
     * 是否文件源服务器
     */
    private boolean isSourceServer = false;
    /**
     * 云区域ID
     */
    private Long cloudAreaId;
    /**
     * 展示给用户的IP
     */
    private String displayIp;
    /**
     * 任务状态
     */
    private int status = -1;
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
     * 执行程序退出码， 0 脚本执行成功，非 0 脚本执行失败
     */
    private Integer exitCode;
    /**
     * 执行结果分组
     */
    private String tag = "";
    /**
     * 脚本任务日志偏移量。Job 从 GSE 根据 scriptLogOffset 增量拉取执行日志
     */
    private int scriptLogOffset;
    /**
     * 脚本任务执行日志
     */
    private String scriptLogContent;
    /**
     * 结果是否发生变化
     */
    private volatile boolean changed;

    public void setStatus(int status) {
        this.changed = this.status != status;
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
        this.changed = this.scriptLogOffset != scriptLogOffset;
        this.scriptLogOffset = scriptLogOffset;
    }

    /**
     * 任务是否结束
     *
     * @return 任务是否结束
     */
    public boolean isFinished() {
        return status != IpStatus.WAITING.getValue() && status != IpStatus.RUNNING.getValue();
    }

    public ExecutionResultGroupDTO getExecutionResultGroup() {
        return new ExecutionResultGroupDTO(status, tag);
    }
}
