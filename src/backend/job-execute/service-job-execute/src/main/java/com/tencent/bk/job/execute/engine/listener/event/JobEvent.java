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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.tencent.bk.job.execute.engine.consts.JobActionEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.StringJoiner;

/**
 * 执行引擎-Job事件
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobEvent extends Event {
    /**
     * 作业操作
     *
     * @see com.tencent.bk.job.execute.engine.consts.JobActionEnum
     */
    private int action;
    /**
     * 作业实例ID
     */
    @JsonProperty("jobInstanceId")
    private Long jobInstanceId;
    /**
     * tmp: 作业实例ID - 兼容字段，发布完成后删除
     */
    @JsonProperty("taskInstanceId")
    private Long taskInstanceId;

    /**
     * 构造启动作业事件
     *
     * @param jobInstanceId 作业实例ID
     * @return 事件
     */
    public static JobEvent startJob(long jobInstanceId) {
        JobEvent jobEvent = new JobEvent();
        jobEvent.setJobInstanceId(jobInstanceId);
        jobEvent.setTaskInstanceId(jobInstanceId);
        jobEvent.setAction(JobActionEnum.START.getValue());
        jobEvent.setTime(LocalDateTime.now());
        return jobEvent;
    }

    /**
     * 构造停止作业事件
     *
     * @param jobInstanceId 作业实例ID
     * @return 事件
     */
    public static JobEvent stopJob(long jobInstanceId) {
        JobEvent jobEvent = new JobEvent();
        jobEvent.setJobInstanceId(jobInstanceId);
        jobEvent.setTaskInstanceId(jobInstanceId);
        jobEvent.setAction(JobActionEnum.STOP.getValue());
        jobEvent.setTime(LocalDateTime.now());
        return jobEvent;
    }

    /**
     * 构造重新执行作业事件
     *
     * @param jobInstanceId 作业实例ID
     * @return 事件
     */
    public static JobEvent restartJob(long jobInstanceId) {
        JobEvent jobEvent = new JobEvent();
        jobEvent.setJobInstanceId(jobInstanceId);
        jobEvent.setTaskInstanceId(jobInstanceId);
        jobEvent.setAction(JobActionEnum.RESTART.getValue());
        jobEvent.setTime(LocalDateTime.now());
        return jobEvent;
    }

    /**
     * 构造刷新作业事件
     *
     * @param jobInstanceId 作业实例ID
     * @param eventSource   事件源
     * @return 事件
     */
    public static JobEvent refreshJob(long jobInstanceId, EventSource eventSource) {
        JobEvent jobEvent = new JobEvent();
        jobEvent.setJobInstanceId(jobInstanceId);
        jobEvent.setTaskInstanceId(jobInstanceId);
        jobEvent.setSource(eventSource);
        jobEvent.setAction(JobActionEnum.REFRESH.getValue());
        jobEvent.setTime(LocalDateTime.now());
        return jobEvent;
    }

    public Long getJobInstanceId() {
        return jobInstanceId != null ? jobInstanceId : taskInstanceId;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", JobEvent.class.getSimpleName() + "[", "]")
            .add("action=" + action)
            .add("actionDesc=" + JobActionEnum.valueOf(action))
            .add("jobInstanceId=" + jobInstanceId)
            .add("eventSource=" + source)
            .add("time=" + time)
            .toString();
    }
}
