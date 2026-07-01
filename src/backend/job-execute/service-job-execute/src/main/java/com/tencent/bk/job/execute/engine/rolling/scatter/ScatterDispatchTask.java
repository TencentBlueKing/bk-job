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

package com.tencent.bk.job.execute.engine.rolling.scatter;

import lombok.Getter;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 * 并行错峰模式下的批次延迟下发任务。基于 {@link Delayed} 由 {@link java.util.concurrent.DelayQueue} 驱动，
 * 到点后由 {@link ScatterDispatchManager} 消费并触发批次下发（仅发送 MQ 事件）。
 */
@Getter
public class ScatterDispatchTask implements Delayed {

    private final Long taskInstanceId;
    private final long stepInstanceId;
    private final int executeCount;
    private final int batch;
    /**
     * 计划下发的绝对时刻（epoch millis）
     */
    private final long dispatchTime;
    /**
     * 预分配的 GSE 任务ID。为 null 时由下发器在到点时创建并绑定；
     * 非 null（如并行重试预先绑定好的批次）时下发器直接发事件、不再重建/重绑。
     */
    private final Long gseTaskId;

    public ScatterDispatchTask(Long taskInstanceId,
                               long stepInstanceId,
                               int executeCount,
                               int batch,
                               long dispatchTime) {
        this(taskInstanceId, stepInstanceId, executeCount, batch, dispatchTime, null);
    }

    public ScatterDispatchTask(Long taskInstanceId,
                               long stepInstanceId,
                               int executeCount,
                               int batch,
                               long dispatchTime,
                               Long gseTaskId) {
        this.taskInstanceId = taskInstanceId;
        this.stepInstanceId = stepInstanceId;
        this.executeCount = executeCount;
        this.batch = batch;
        this.dispatchTime = dispatchTime;
        this.gseTaskId = gseTaskId;
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long diff = dispatchTime - System.currentTimeMillis();
        return unit.convert(diff, TimeUnit.MILLISECONDS);
    }

    @Override
    public int compareTo(Delayed o) {
        if (o instanceof ScatterDispatchTask) {
            return Long.compare(this.dispatchTime, ((ScatterDispatchTask) o).dispatchTime);
        }
        return Long.compare(getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ScatterDispatchTask)) {
            return false;
        }
        ScatterDispatchTask that = (ScatterDispatchTask) o;
        return stepInstanceId == that.stepInstanceId
            && executeCount == that.executeCount
            && batch == that.batch
            && Objects.equals(taskInstanceId, that.taskInstanceId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(taskInstanceId, stepInstanceId, executeCount, batch);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ScatterDispatchTask.class.getSimpleName() + "[", "]")
            .add("taskInstanceId=" + taskInstanceId)
            .add("stepInstanceId=" + stepInstanceId)
            .add("executeCount=" + executeCount)
            .add("batch=" + batch)
            .add("dispatchTime=" + dispatchTime)
            .toString();
    }
}
