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

package com.tencent.bk.job.crontab.listener.event;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tencent.bk.job.common.event.Event;
import com.tencent.bk.job.crontab.constant.CrontabActionEnum;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.StringJoiner;

/**
 * 定时任务事件
 */
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CrontabEvent extends Event {
    /**
     * 定时任务操作
     *
     * @see CrontabActionEnum
     */
    private int action;
    /**
     * Job业务ID
     */
    private Long appId;
    /**
     * 定时任务ID
     */
    private Long cronJobId;

    /**
     * 构造添加定时任务事件
     *
     * @param appId     Job业务ID
     * @param cronJobId 定时任务ID
     * @return 事件
     */
    public static CrontabEvent addCron(long appId, long cronJobId) {
        CrontabEvent crontabEvent = new CrontabEvent();
        crontabEvent.setAppId(appId);
        crontabEvent.setCronJobId(cronJobId);
        crontabEvent.setAction(CrontabActionEnum.ADD_CRON.getValue());
        crontabEvent.setTime(LocalDateTime.now());
        return crontabEvent;
    }

    /**
     * 构造删除定时任务事件
     *
     * @param appId     Job业务ID
     * @param cronJobId 定时任务ID
     * @return 事件
     */
    public static CrontabEvent deleteCron(long appId, long cronJobId) {
        CrontabEvent crontabEvent = new CrontabEvent();
        crontabEvent.setAppId(appId);
        crontabEvent.setCronJobId(cronJobId);
        crontabEvent.setAction(CrontabActionEnum.DELETE_CRON.getValue());
        crontabEvent.setTime(LocalDateTime.now());
        return crontabEvent;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CrontabEvent.class.getSimpleName() + "[", "]")
            .add("action=" + action)
            .add("appId=" + appId)
            .add("cronJobId=" + cronJobId)
            .add("time=" + time)
            .toString();
    }
}
