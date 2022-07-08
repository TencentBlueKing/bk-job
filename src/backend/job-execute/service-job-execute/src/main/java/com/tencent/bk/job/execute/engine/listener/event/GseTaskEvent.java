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
import com.tencent.bk.job.execute.engine.consts.GseTaskActionEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.StringJoiner;
import java.util.UUID;

/**
 * 执行引擎-GSE任务事件
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GseTaskEvent extends Event {
    /**
     * GSE任务操作
     *
     * @see com.tencent.bk.job.execute.engine.consts.GseTaskActionEnum
     */
    private int action;
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
     * GSE任务ID
     */
    private Long gseTaskId;
    /**
     * 请求ID,防止重复下发任务
     */
    private String requestId;

    public static GseTaskEvent startGseTask(Long stepInstanceId,
                                            Integer executeCount,
                                            Integer batch,
                                            Long gseTaskId,
                                            String requestId) {
        GseTaskEvent gseTaskEvent = buildGseTaskEvent(stepInstanceId, executeCount, batch, gseTaskId);
        if (StringUtils.isNotEmpty(requestId)) {
            gseTaskEvent.setRequestId(requestId);
        } else {
            gseTaskEvent.setRequestId(UUID.randomUUID().toString());
        }
        gseTaskEvent.setAction(GseTaskActionEnum.START.getValue());
        return gseTaskEvent;
    }

    public static GseTaskEvent stopGseTask(Long stepInstanceId,
                                           Integer executeCount,
                                           Integer batch,
                                           Long gseTaskId) {
        GseTaskEvent gseTaskEvent = buildGseTaskEvent(stepInstanceId, executeCount, batch, gseTaskId);
        gseTaskEvent.setAction(GseTaskActionEnum.STOP.getValue());
        return gseTaskEvent;
    }

    private static GseTaskEvent buildGseTaskEvent(Long stepInstanceId,
                                                  Integer executeCount,
                                                  Integer batch,
                                                  Long gseTaskId) {
        GseTaskEvent gseTaskEvent = new GseTaskEvent();
        gseTaskEvent.setGseTaskId(gseTaskId);
        gseTaskEvent.setStepInstanceId(stepInstanceId);
        gseTaskEvent.setExecuteCount(executeCount);
        gseTaskEvent.setBatch(batch);
        gseTaskEvent.setTime(LocalDateTime.now());

        return gseTaskEvent;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", GseTaskEvent.class.getSimpleName() + "[", "]")
            .add("action=" + action)
            .add("actionDesc=" + GseTaskActionEnum.valueOf(action))
            .add("gseTaskId=" + gseTaskId)
            .add("time=" + time)
            .add("requestId='" + requestId + "'")
            .add("source=" + source)
            .toString();
    }
}
