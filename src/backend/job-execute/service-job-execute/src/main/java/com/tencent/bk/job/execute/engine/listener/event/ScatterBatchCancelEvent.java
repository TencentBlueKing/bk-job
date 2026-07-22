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
 * 执行引擎-并行错峰批次取消广播事件。
 * <p>
 * 用于并行错峰模式整步终止时，把「取消该步骤未下发批次」广播到所有 job-execute 副本（fanout，含自身）。
 * 各副本收到后对本地 {@link com.tencent.bk.job.execute.engine.rolling.scatter.ScatterDispatchManager} 延迟队列
 * 执行取消，并把取消到的未下发批次收敛为终止成功。用于缩短终止到收敛的时间窗（即时性优化）；
 * 最终正确性由 {@link com.tencent.bk.job.execute.engine.rolling.scatter.ScatterBatchDispatcher} 到点兜底保证，
 * 故本广播丢失/副本重启后批次转移不影响正确性。
 */
@Getter
@Setter
@ToString
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScatterBatchCancelEvent extends Event {
    /**
     * 作业实例ID
     */
    private Long taskInstanceId;
    /**
     * 步骤实例ID
     */
    private Long stepInstanceId;
    /**
     * 执行次数（仅取消匹配该执行次数的未下发批次，避免误取消重试后新一轮批次）
     */
    private Integer executeCount;

    /**
     * 构造并行错峰批次取消广播事件
     *
     * @param taskInstanceId 作业实例ID
     * @param stepInstanceId 步骤实例ID
     * @param executeCount   执行次数
     * @return 事件
     */
    public static ScatterBatchCancelEvent cancel(Long taskInstanceId,
                                                 Long stepInstanceId,
                                                 Integer executeCount) {
        ScatterBatchCancelEvent event = new ScatterBatchCancelEvent();
        event.setTaskInstanceId(taskInstanceId);
        event.setStepInstanceId(stepInstanceId);
        event.setExecuteCount(executeCount);
        event.setTime(LocalDateTime.now());
        return event;
    }
}
