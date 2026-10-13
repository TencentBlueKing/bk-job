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
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
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

package com.tencent.bk.job.crontab.common.constants;

import com.tencent.bk.job.execute.common.constants.RunStatusEnum;

/**
 * 定时任务的最新执行结果
 */
public enum CronJobLastExecuteStatusEnum {
    /**
     * 未执行
     */
    NOT_STARTED(0),
    /**
     * 执行成功
     */
    SUCCESS(1),
    /**
     * 执行失败
     */
    FAIL(2),
    /**
     * 跳过
     */
    SKIPPED(3),
    /**
     * 忽略错误
     */
    IGNORE_ERROR(4),
    /**
     * 手动结束
     */
    TERMINATED(5),
    /**
     * 状态异常
     */
    ABNORMAL_STATE(6),
    /**
     * 强制终止成功
     */
    STOP_SUCCESS(7),
    /**
     * 确认终止
     */
    CONFIRM_TERMINATED(8),
    /**
     * 被丢弃
     */
    ABANDONED(9);

    private final int value;

    CronJobLastExecuteStatusEnum(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * 终止态映射为具体状态，其他状态返回未执行
     *
     * @see RunStatusEnum#isFinishedStatus(RunStatusEnum)
     */
    public static CronJobLastExecuteStatusEnum fromRunStatus(RunStatusEnum status) {
        switch (status) {
            case SUCCESS:
                return SUCCESS;
            case FAIL:
                return FAIL;
            case SKIPPED:
                return SKIPPED;
            case IGNORE_ERROR:
                return IGNORE_ERROR;
            case TERMINATED:
                return TERMINATED;
            case ABNORMAL_STATE:
                return ABNORMAL_STATE;
            case STOP_SUCCESS:
                return STOP_SUCCESS;
            case CONFIRM_TERMINATED:
                return CONFIRM_TERMINATED;
            case ABANDONED:
                return ABANDONED;
            default:
                return NOT_STARTED;
        }
    }
}
