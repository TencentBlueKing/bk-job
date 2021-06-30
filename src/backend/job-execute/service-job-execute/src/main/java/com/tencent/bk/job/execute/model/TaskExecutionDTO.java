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

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tencent.bk.job.common.util.json.LongTimestampSerializer;
import com.tencent.bk.job.execute.common.constants.TaskTypeEnum;
import lombok.Data;

/**
 * 作业执行情况
 */
@Data
public class TaskExecutionDTO {
    /**
     * 作业实例ID
     */
    private Long taskInstanceId;
    /**
     * 执行方案ID
     */
    private Long taskId;
    /**
     * 作业模板 ID
     */
    private Long taskTemplateId;

    /**
     * 是否调试执行方案
     */
    private boolean debugTask;
    /**
     * 作业名称
     */
    private String name;
    /**
     * 任务类型
     *
     * @see TaskTypeEnum
     */
    private Integer type;
    /**
     * 总耗时
     */
    private Long totalTime;
    /**
     * 作业状态
     */
    private Integer status;
    /**
     * 开始时间
     */
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long startTime;
    /**
     * 结束时间
     */
    @JsonSerialize(using = LongTimestampSerializer.class)
    private Long endTime;

    /**
     * 当前执行步骤
     */
    private long currentStepInstanceId;
}
