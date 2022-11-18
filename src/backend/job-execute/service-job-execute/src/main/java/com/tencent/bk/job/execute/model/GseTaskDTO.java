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
import lombok.NoArgsConstructor;

/**
 * GSE 任务
 */
@Data
@NoArgsConstructor
public class GseTaskDTO {
    /**
     * GSE任务ID
     */
    private Long id;
    /**
     * 步骤实例ID
     */
    private Long stepInstanceId;
    /**
     * 步骤执行次数
     */
    private Integer executeCount;
    /**
     * 滚动执行批次
     */
    private int batch;
    /**
     * 任务开始时间
     */
    private Long startTime;
    /**
     * 任务结束时间
     */
    private Long endTime;
    /**
     * 任务耗时，单位毫秒
     */
    private Long totalTime;
    /**
     * 任务状态
     */
    private Integer status = 1;
    /**
     * GSE 任务ID
     */
    private String gseTaskId;
    /**
     * GSE 任务的唯一名称，类似于ID的作用
     */
    private String taskUniqueName;

    public GseTaskDTO(Long stepInstanceId, Integer executeCount, int batch) {
        this.stepInstanceId = stepInstanceId;
        this.executeCount = executeCount;
        this.batch = batch;
    }

    public String getTaskUniqueName() {
        if (taskUniqueName != null) {
            return taskUniqueName;
        } else {
            String taskName = "GseTask:" + id + ":" + stepInstanceId + ":" + executeCount;
            if (batch > 0) {
                taskName = taskName + ":" + batch;
            }
            taskUniqueName = taskName;
        }
        return taskUniqueName;
    }
}
