/*
 * Tencent is pleased to support the open source community by making BK-JOB蓝鲸智云作业平台 available.
 *
 * Copyright (C) 2021 Tencent.  All rights reserved.
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
 * 执行引擎-滚动批次并行错峰下发恢复事件。
 * <p>
 * 用于优雅停机时，把某实例 {@link com.tencent.bk.job.execute.engine.rolling.scatter.ScatterDispatchManager}
 * 延迟队列中尚未下发的批次转移出去，由其它实例竞争消费后按剩余延时重新入队。
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RollingBatchDispatchResumeEvent extends Event {
    /**
     * 作业实例ID
     */
    private Long jobInstanceId;
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
     * 计划下发的绝对时刻（epoch millis）
     */
    private Long dispatchTime;
    /**
     * 预分配的 GSE 任务ID（如并行重试预先绑定的批次），可为 null
     */
    private Long gseTaskId;

    /**
     * 构造滚动批次下发恢复事件
     *
     * @param jobInstanceId  作业实例ID
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @param batch          滚动批次
     * @param dispatchTime   计划下发的绝对时刻
     * @param gseTaskId      预分配的 GSE 任务ID，可为 null
     * @return 事件
     */
    public static RollingBatchDispatchResumeEvent resume(Long jobInstanceId,
                                                         Long stepInstanceId,
                                                         Integer executeCount,
                                                         Integer batch,
                                                         Long dispatchTime,
                                                         Long gseTaskId) {
        RollingBatchDispatchResumeEvent event = new RollingBatchDispatchResumeEvent();
        event.setJobInstanceId(jobInstanceId);
        event.setStepInstanceId(stepInstanceId);
        event.setExecuteCount(executeCount);
        event.setBatch(batch);
        event.setDispatchTime(dispatchTime);
        event.setGseTaskId(gseTaskId);
        event.setTime(LocalDateTime.now());
        return event;
    }
}
