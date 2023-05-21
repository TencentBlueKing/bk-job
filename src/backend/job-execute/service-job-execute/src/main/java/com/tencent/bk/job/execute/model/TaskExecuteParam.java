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
import com.tencent.bk.job.execute.engine.model.TaskVariableDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * 作业执行参数
 */
@Data
@Builder
public class TaskExecuteParam {
    /**
     * 业务ID
     */
    private Long appId;
    /**
     * 作业执行方案ID
     */
    private Long planId;
    /**
     * 任务名称，如果不为空就使用该值作为执行任务的名称
     */
    private String taskName;
    /**
     * 执行者
     */
    private String operator;
    /**
     * 作业执行传入变量
     */
    private List<TaskVariableDTO> executeVariableValues;
    /**
     * 启动方式
     */
    private TaskStartupModeEnum startupMode;
    /**
     * 定时任务ID
     */
    private Long cronTaskId;
    /**
     * 作业执行访问回调URL
     */
    private String callbackUrl;
    /**
     * 任务执行完成之后回调参数，比callbackUrl优先级高
     */
    private EsbCallbackDTO callback;
    /**
     * API调用方app_code
     */
    private String appCode;

    /**
     * 是否跳过权限校验
     */
    private boolean skipAuth;
}
