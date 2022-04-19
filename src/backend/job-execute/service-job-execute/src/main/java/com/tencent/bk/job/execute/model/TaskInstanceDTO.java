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

import com.tencent.bk.job.common.esb.model.EsbCallbackDTO;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import lombok.Data;

import java.util.List;

/**
 * 执行作业实例
 */
@Data
public class TaskInstanceDTO {
    /**
     * id
     */
    private Long id;

    /**
     * 执行作业id
     */
    private Long taskId;

    /**
     * 定时作业ID
     */
    private Long cronTaskId;

    /**
     * 作业模板ID
     */
    private Long taskTemplateId;

    /**
     * 是否调试方案
     */
    private boolean debugTask;

    /**
     * 业务id
     */
    private Long appId;

    /**
     * 名称
     */
    private String name;

    /**
     * 执行人
     */
    private String operator;

    /**
     * 启动方式： 1.页面执行、2.API调用、3.定时执行
     *
     * @see TaskStartupModeEnum
     */
    private Integer startupMode;

    /**
     * 当前执行步骤id
     */
    private long currentStepId;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 开始时间
     */
    private Long startTime;

    /**
     * 结束时间
     */
    private Long endTime;

    /**
     * 总耗时，单位：毫秒
     */
    private Long totalTime;

    /**
     * 创建时间
     */
    private Long createTime;

    /**
     * 作业执行类型
     *
     * @see TaskTypeEnum
     */
    private Integer type;

    /**
     * API调用的作业，调用方appCode
     */
    private String appCode;

    /**
     * 回调地址
     */
    private String callbackUrl;

    /**
     * 任务执行完成之后回调参数，比callbackUrl优先级高
     */
    private EsbCallbackDTO callback;

    /**
     * 步骤实例
     */
    private List<StepInstanceDTO> stepInstances;

    /**
     * 变量
     */
    private List<TaskVariableDTO> variables;


}

