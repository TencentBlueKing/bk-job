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

package com.tencent.bk.job.crontab.model.dto;

import com.tencent.bk.job.crontab.constant.ExecuteStatusEnum;
import com.tencent.bk.job.crontab.model.CronJobLaunchHistoryVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * @since 16/2/2020 22:00
 */
@Data
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class CronJobHistoryDTO {

    /**
     * 定时任务执行历史 ID
     */
    private Long id;

    /**
     * 业务 ID
     */
    private Long appId;

    /**
     * 定时任务 ID
     */
    private Long cronJobId;

    /**
     * 执行状态
     *
     * @see ExecuteStatusEnum
     */
    private ExecuteStatusEnum status;

    /**
     * 调度时间
     */
    private Long scheduledTime;

    /**
     * 执行开始时间
     */
    private Long startTime;

    /**
     * 执行结束时间
     */
    private Long finishTime;

    /**
     * 执行人
     */
    private String executor;

    /**
     * 错误码
     */
    private Long errorCode;

    /**
     * 错误消息
     */
    private String errorMsg;

    public static CronJobLaunchHistoryVO toVO(CronJobHistoryDTO cronJobHistoryInfo) {
        CronJobLaunchHistoryVO historyVO = new CronJobLaunchHistoryVO();
        historyVO.setScheduledTime(cronJobHistoryInfo.getScheduledTime());
        historyVO.setExecuteTime(cronJobHistoryInfo.getStartTime());
        historyVO.setStatus(cronJobHistoryInfo.getStatus().getValue());
        historyVO.setExecutor(cronJobHistoryInfo.getExecutor());
        historyVO.setErrorCode(cronJobHistoryInfo.getErrorCode());
        historyVO.setErrorMsg(cronJobHistoryInfo.getErrorMsg());
        return historyVO;
    }
}
