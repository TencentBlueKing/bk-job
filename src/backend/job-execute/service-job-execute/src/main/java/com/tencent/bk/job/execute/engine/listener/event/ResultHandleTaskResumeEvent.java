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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * 执行引擎-GSE任务结果处理任务恢复事件
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResultHandleTaskResumeEvent extends Event {
    /**
     * 步骤实例ID
     */
    private Long stepInstanceId;
    /**
     * 执行次数
     */
    private Integer executeCount;
    /**
     * 滚动批次
     */
    private Integer batch;
    /**
     * GSE 任务ID
     */
    private Long gseTaskId;
    /**
     * 请求ID,防止重复下发任务
     */
    private String requestId;

    /**
     * 构造恢复任务事件
     *
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动批次
     * @param gseTaskId      GSE 任务ID
     * @param requestId      请求ID,防止重复下发任务
     * @return 事件
     */
    public static ResultHandleTaskResumeEvent resume(Long stepInstanceId,
                                                     Integer executeCount,
                                                     Integer batch,
                                                     Long gseTaskId,
                                                     String requestId) {
        ResultHandleTaskResumeEvent event = new ResultHandleTaskResumeEvent();
        event.setStepInstanceId(stepInstanceId);
        event.setExecuteCount(executeCount);
        event.setBatch(batch);
        event.setGseTaskId(gseTaskId);
        event.setRequestId(requestId);
        event.setTime(LocalDateTime.now());
        return event;
    }
}
