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

package com.tencent.bk.job.execute.engine.result;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 任务上下文
 */
@Data
@NoArgsConstructor
public class TaskContext {
    private Long jobInstanceId;

    /**
     * 该结果处理任务是否已释放作业存活心跳引用。
     * <p>
     * 作业存活心跳（RunningJobKeepaliveManager）按 jobInstanceId 引用计数：每个结果处理任务投递时 +1、结束时 -1。
     * 同一结果处理任务存在两条可能并发的停止路径——正常完成路径（ScheduledContinuousResultHandleTask 完成后）与
     * 优雅停机转移路径（AbstractResultHandleTask#tryStopImmediately）。二者可能对同一任务竞争触发，
     * 借助该标记保证每个任务对存活心跳的释放严格幂等（至多一次），避免引用计数被过度递减而导致心跳提前归 0。
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private final AtomicBoolean keepaliveStopped = new AtomicBoolean(false);

    public TaskContext(Long jobInstanceId) {
        this.jobInstanceId = jobInstanceId;
    }

    /**
     * 尝试将该任务标记为“已释放作业存活心跳引用”。
     *
     * @return 首次标记成功返回 true（应执行一次 stopKeepaliveTask），重复调用返回 false（应跳过，保证幂等）
     */
    public boolean markKeepaliveStopped() {
        return keepaliveStopped.compareAndSet(false, true);
    }
}
