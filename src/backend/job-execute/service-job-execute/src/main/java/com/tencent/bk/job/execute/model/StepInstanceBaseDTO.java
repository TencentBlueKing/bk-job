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

import com.tencent.bk.job.common.gse.util.AgentUtils;
import com.tencent.bk.job.execute.common.constants.RunStatusEnum;
import com.tencent.bk.job.execute.common.constants.StepExecuteTypeEnum;
import com.tencent.bk.job.manage.common.consts.task.TaskStepTypeEnum;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Set;

/**
 * 步骤实例
 */
@Getter
@Setter
@ToString
public class StepInstanceBaseDTO {
    /**
     * 步骤实例ID
     */
    protected Long id;
    /**
     * 执行次数
     */
    protected int executeCount;
    /**
     * 执行步骤id
     */
    protected Long stepId;

    /**
     * 执行作业实例id
     */
    protected Long taskInstanceId;
    /**
     * 业务id
     */
    protected Long appId;
    /**
     * 名称
     */
    protected String name;
    /**
     * 步骤执行类型：1、执行脚本，2、传输文件，3、人工确认, 4、SQL执行
     *
     * @see StepExecuteTypeEnum
     */
    protected Integer executeType;
    /**
     * 步骤类型
     */
    protected TaskStepTypeEnum stepType;
    /**
     * 执行人
     */
    protected String operator;
    /**
     * 执行状态
     */
    protected RunStatusEnum status;
    /**
     * 开始时间
     */
    protected Long startTime;
    /**
     * 结束时间
     */
    protected Long endTime;
    /**
     * 总耗时，单位：毫秒
     */
    protected Long totalTime;
    /**
     * 创建时间
     */
    protected Long createTime;
    /**
     * 是否自动忽略错误
     */
    protected boolean ignoreError;
    /**
     * 目标服务器
     */
    protected ServersDTO targetServers;
    /**
     * 不合法的服务器
     */
    protected Set<String> invalidIps;
    /**
     * 步骤总数
     */
    protected Integer stepNum;
    /**
     * 当前步骤在作业中的顺序
     */
    protected Integer stepOrder;
    /**
     * 滚动执行批次
     */
    protected int batch;
    /**
     * 滚动配置ID
     */
    protected Long rollingConfigId;

    /**
     * 获取步骤类型
     *
     * @return 步骤类型
     * @see TaskStepTypeEnum
     */
    public TaskStepTypeEnum getStepType() {
        if (this.stepType == null) {
            if (executeType != null) {
                if (executeType.equals(StepExecuteTypeEnum.EXECUTE_SCRIPT.getValue())
                    || executeType.equals(StepExecuteTypeEnum.EXECUTE_SQL.getValue())) {
                    this.stepType = TaskStepTypeEnum.SCRIPT;
                } else if (executeType.equals(StepExecuteTypeEnum.SEND_FILE.getValue())) {
                    this.stepType = TaskStepTypeEnum.FILE;
                } else if (executeType.equals(StepExecuteTypeEnum.MANUAL_CONFIRM.getValue())) {
                    this.stepType = TaskStepTypeEnum.APPROVAL;
                }
            }
        }
        return this.stepType;
    }

    /**
     * 是否作业中最后一个步骤
     */
    public boolean isLastStep() {
        return this.stepNum.equals(1) || this.stepNum.equals(this.stepOrder);
    }

    /**
     * 是否文件分发步骤
     */
    public boolean isFileStep() {
        return getStepType() == TaskStepTypeEnum.FILE;
    }

    /**
     * 是否脚本执行步骤
     */
    public boolean isScriptStep() {
        return getStepType() == TaskStepTypeEnum.SCRIPT;
    }

    public int getTargetServerTotalCount() {
        if (this.targetServers != null && this.targetServers.getIpList() != null) {
            return this.targetServers.getIpList().size();
        } else {
            return 0;
        }
    }

    /**
     * 是否滚动步骤
     */
    public boolean isRollingStep() {
        return this.rollingConfigId != null && this.rollingConfigId > 0;
    }

    /**
     * 是否滚动执行第一批次
     */
    public boolean isFirstRollingBatch() {
        return this.batch == 1;
    }

    public String getUniqueKey() {
        if (executeCount == 0) {
            return id.toString();
        }
        return id + "_" + executeCount;
    }

    /**
     * 执行目标是否是 GSE V2 Agent
     */
    public boolean isTargetGseV2Agent() {
        boolean isTargetGseV1Agent = this.targetServers.getIpList().stream()
            .anyMatch(host -> AgentUtils.isGseV1AgentId(host.getAgentId()));
        return !isTargetGseV1Agent;
    }
}
