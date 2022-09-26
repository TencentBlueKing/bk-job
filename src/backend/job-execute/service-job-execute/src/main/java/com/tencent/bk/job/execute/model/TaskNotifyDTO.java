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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.execute.common.constants.TaskStartupModeEnum;
import lombok.Getter;
import lombok.Setter;

/**
 * 作业执行结果消息通知
 */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TaskNotifyDTO {
    /**
     * 操作者
     */
    private String operator;
    /**
     * 业务Id
     */
    private Long appId;
    /**
     * 资源Id
     */
    private String resourceId;
    /**
     * 资源类型
     */
    private Integer resourceType;
    /**
     * 启动方式
     *
     * @see TaskStartupModeEnum
     */
    private Integer startupMode;

    /**
     * 资源操作结果
     *
     * @see com.tencent.bk.job.manage.common.consts.notify.ExecuteStatusEnum
     */
    private Integer resourceExecuteStatus;
    /**
     * 作业ID
     */
    private Long taskId;
    /**
     * 作业执行实例ID
     */
    private Long taskInstanceId;
    /**
     * 作业名称
     */
    private String taskInstanceName;
    /**
     * 步骤名称
     */
    private String stepName;
    /**
     * 执行耗时
     */
    private Long cost;
    /**
     * 人工确认消息
     */
    private String confirmMessage;

    private NotifyDTO notifyDTO;
}
