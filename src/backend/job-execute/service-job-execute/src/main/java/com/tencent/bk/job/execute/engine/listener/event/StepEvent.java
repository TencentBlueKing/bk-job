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
import com.tencent.bk.job.execute.engine.consts.StepActionEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.StringJoiner;

/**
 * 执行引擎-步骤事件
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StepEvent extends Event {
    /**
     * 步骤操作
     *
     * @see com.tencent.bk.job.execute.engine.consts.StepActionEnum
     */
    private int action;
    /**
     * 步骤实例ID
     */
    private long stepInstanceId;
    /**
     * 滚动执行批次
     */
    private Integer batch;

    /**
     * 构造`忽略错误`事件
     *
     * @param stepInstanceId 步骤实例ID
     * @return 事件
     */
    public static StepEvent ignoreError(long stepInstanceId) {
        StepEvent stepEvent = new StepEvent();
        stepEvent.setStepInstanceId(stepInstanceId);
        stepEvent.setAction(StepActionEnum.IGNORE_ERROR.getValue());
        stepEvent.setTime(LocalDateTime.now());
        return stepEvent;
    }

    /**
     * 构造`下一步`事件
     *
     * @param stepInstanceId 步骤实例ID
     * @return 事件
     */
    public static StepEvent nextStep(long stepInstanceId) {
        StepEvent stepEvent = new StepEvent();
        stepEvent.setStepInstanceId(stepInstanceId);
        stepEvent.setAction(StepActionEnum.NEXT_STEP.getValue());
        stepEvent.setTime(LocalDateTime.now());
        return stepEvent;
    }

    /**
     * 构造`人工确认-重新发起确认`事件
     *
     * @param stepInstanceId 步骤实例ID
     * @return 事件
     */
    public static StepEvent confirmStepContinue(long stepInstanceId) {
        StepEvent stepEvent = new StepEvent();
        stepEvent.setStepInstanceId(stepInstanceId);
        stepEvent.setAction(StepActionEnum.CONFIRM_CONTINUE.getValue());
        stepEvent.setTime(LocalDateTime.now());
        return stepEvent;
    }

    /**
     * 构造`人工确认-终止流程`事件
     *
     * @param stepInstanceId 步骤实例ID
     * @return 事件
     */
    public static StepEvent confirmStepTerminate(long stepInstanceId) {
        StepEvent stepEvent = new StepEvent();
        stepEvent.setStepInstanceId(stepInstanceId);
        stepEvent.setAction(StepActionEnum.CONFIRM_TERMINATE.getValue());
        stepEvent.setTime(LocalDateTime.now());
        return stepEvent;
    }

    /**
     * 构造`人工确认-重新发起确认`事件
     *
     * @param stepInstanceId 步骤实例ID
     * @return 事件
     */
    public static StepEvent confirmStepRestart(long stepInstanceId) {
        StepEvent stepEvent = new StepEvent();
        stepEvent.setStepInstanceId(stepInstanceId);
        stepEvent.setAction(StepActionEnum.CONFIRM_RESTART.getValue());
        stepEvent.setTime(LocalDateTime.now());
        return stepEvent;
    }

    /**
     * 构造`启动步骤`事件
     *
     * @param stepInstanceId 步骤实例ID
     * @param batch          滚动执行批次
     * @return 事件
     */
    public static StepEvent startStep(long stepInstanceId, Integer batch) {
        StepEvent stepEvent = new StepEvent();
        stepEvent.setStepInstanceId(stepInstanceId);
        stepEvent.setBatch(batch);
        stepEvent.setAction(StepActionEnum.START.getValue());
        stepEvent.setTime(LocalDateTime.now());
        return stepEvent;
    }

    /**
     * 构造`跳过步骤`事件
     *
     * @param stepInstanceId 步骤实例ID
     * @return 事件
     */
    public static StepEvent skipStep(long stepInstanceId) {
        StepEvent stepEvent = new StepEvent();
        stepEvent.setStepInstanceId(stepInstanceId);
        stepEvent.setAction(StepActionEnum.SKIP.getValue());
        stepEvent.setTime(LocalDateTime.now());
        return stepEvent;
    }

    /**
     * 构造`停止步骤`事件
     *
     * @param stepInstanceId 步骤实例ID
     * @return 事件
     */
    public static StepEvent stopStep(long stepInstanceId) {
        StepEvent stepEvent = new StepEvent();
        stepEvent.setStepInstanceId(stepInstanceId);
        stepEvent.setAction(StepActionEnum.STOP.getValue());
        stepEvent.setTime(LocalDateTime.now());
        return stepEvent;
    }

    /**
     * 构造`失败IP重试`事件
     *
     * @param stepInstanceId 步骤实例ID
     * @return 事件
     */
    public static StepEvent retryStepFail(long stepInstanceId) {
        StepEvent stepEvent = new StepEvent();
        stepEvent.setStepInstanceId(stepInstanceId);
        stepEvent.setAction(StepActionEnum.RETRY_FAIL.getValue());
        stepEvent.setTime(LocalDateTime.now());
        return stepEvent;
    }

    /**
     * 构造`全部重试`事件
     *
     * @param stepInstanceId 步骤实例ID
     * @return 事件
     */
    public static StepEvent retryStepAll(long stepInstanceId) {
        StepEvent stepEvent = new StepEvent();
        stepEvent.setStepInstanceId(stepInstanceId);
        stepEvent.setAction(StepActionEnum.RETRY_ALL.getValue());
        stepEvent.setTime(LocalDateTime.now());
        return stepEvent;
    }

    /**
     * 构造`第三方文件源文件拉取完成后继续GSE分发`事件
     *
     * @param stepInstanceId 步骤实例ID
     * @return 事件
     */
    public static StepEvent continueGseFileStep(long stepInstanceId) {
        StepEvent stepEvent = new StepEvent();
        stepEvent.setStepInstanceId(stepInstanceId);
        stepEvent.setAction(StepActionEnum.CONTINUE_FILE_PUSH.getValue());
        stepEvent.setTime(LocalDateTime.now());
        return stepEvent;
    }

    /**
     * 构造`刷新步骤状态`事件
     *
     * @param stepInstanceId 步骤实例ID
     * @return 事件
     */
    public static StepEvent refreshStep(long stepInstanceId, EventSource eventSource) {
        StepEvent stepEvent = new StepEvent();
        stepEvent.setStepInstanceId(stepInstanceId);
        stepEvent.setAction(StepActionEnum.REFRESH.getValue());
        stepEvent.setSource(eventSource);
        stepEvent.setTime(LocalDateTime.now());
        return stepEvent;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", StepEvent.class.getSimpleName() + "[", "]")
            .add("action=" + action)
            .add("actionDesc=" + StepActionEnum.valueOf(action))
            .add("stepInstanceId=" + stepInstanceId)
            .add("batch=" + batch)
            .add("source=" + source)
            .add("time=" + time)
            .toString();
    }
}
