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
    private EventSourceTypeEnum sourceType;
    private Long taskInstanceId;
    private Long stepInstanceId;
    private Integer executeCount;
    private Integer batch;
    private Long gseTaskId;

    public static EventSource buildStepEventSource(long stepInstanceId) {
        EventSource eventSource = new EventSource();
        eventSource.setStepInstanceId(stepInstanceId);
        eventSource.setSourceType(EventSourceTypeEnum.STEP);
        return eventSource;
    }

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
