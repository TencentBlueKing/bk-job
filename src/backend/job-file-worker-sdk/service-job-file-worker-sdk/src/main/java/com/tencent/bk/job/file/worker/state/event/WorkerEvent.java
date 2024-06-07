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

package com.tencent.bk.job.file.worker.state.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.event.Event;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Setter
@ToString
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkerEvent extends Event {
    /**
     * Worker动作
     *
     * @see WorkerActionEnum
     */
    private WorkerActionEnum action;

    public static WorkerEvent waitAccessReady() {
        WorkerEvent workerEvent = new WorkerEvent();
        workerEvent.setAction(WorkerActionEnum.WAIT_ACCESS_READY);
        workerEvent.setTime(LocalDateTime.now());
        return workerEvent;
    }

    public static WorkerEvent heartBeat() {
        WorkerEvent workerEvent = new WorkerEvent();
        workerEvent.setAction(WorkerActionEnum.HEART_BEAT);
        workerEvent.setTime(LocalDateTime.now());
        return workerEvent;
    }

    public static WorkerEvent offLine() {
        WorkerEvent workerEvent = new WorkerEvent();
        workerEvent.setAction(WorkerActionEnum.OFF_LINE);
        workerEvent.setTime(LocalDateTime.now());
        return workerEvent;
    }
}
