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

import lombok.Data;

import java.util.List;

/**
 * 步骤实例执行情况
 */
@Data
public class StepExecutionDTO {
    /**
     * 步骤实例ID
     */
    private Long stepInstanceId;
    /**
     * 执行次数
     */
    private Integer executeCount;
    /**
     * 步骤名称
     */
    private String name;
    /**
     * 步骤类型,1-脚本，2-文件，3-人工确认
     */
    private Integer type;
    /**
     * 人工确认提示
     */
    private String confirmMessage;
    /**
     * 人工确认理由
     */
    private String confirmReason;
    /**
     * 通知方式
     */
    private List<String> confirmNotifyChannels;
    /**
     * 确认人
     */
    private List<String> confirmUsers;
    /**
     * 确认角色
     */
    private List<String> confirmRoles;
    /**
     * 总耗时
     */
    private Long totalTime;
    /**
     * 运行状态
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
     * 操作者
     */
    private String operator;
    /**
     * 是否最后一个步骤
     */
    private boolean lastStep;
}
