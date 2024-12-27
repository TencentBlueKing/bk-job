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

package com.tencent.bk.job.execute.engine.listener.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.execute.engine.consts.EventSourceTypeEnum;
import lombok.Data;

/**
 * 执行引擎事件源
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventSource {
    /**
     * Job 执行引擎任务调度事件源类型
     */
    private EventSourceTypeEnum sourceType;

    /**
     * 作业实例ID
     */
    private Long jobInstanceId;

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
    private Integer batch;

    /**
     * GSE 任务ID
     */
    private Long gseTaskId;

    /**
     * 构造事件源 - 步骤
     *
     * @param stepInstanceId 步骤实例ID
     * @return 执行引擎事件源
     */
    public static EventSource buildStepEventSource(long stepInstanceId) {
        EventSource eventSource = new EventSource();
        eventSource.setStepInstanceId(stepInstanceId);
        eventSource.setSourceType(EventSourceTypeEnum.STEP);
        return eventSource;
    }

    /**
     * 构造事件源 - GSE 任务
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   步骤执行次数
     * @param batch          滚动执行批次
     * @param gseTaskId      GSE 任务ID
     * @return 执行引擎事件源
     */
    public static EventSource buildGseTaskEventSource(Long stepInstanceId,
                                                      Integer executeCount,
                                                      Integer batch,
                                                      Long gseTaskId) {
        EventSource eventSource = new EventSource();
        eventSource.setStepInstanceId(stepInstanceId);
        eventSource.setExecuteCount(executeCount);
        eventSource.setBatch(batch);
        eventSource.setGseTaskId(gseTaskId);
        eventSource.setSourceType(EventSourceTypeEnum.GSE_TASK);
        return eventSource;
    }
}
